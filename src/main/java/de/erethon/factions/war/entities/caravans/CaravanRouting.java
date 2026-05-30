package de.erethon.factions.war.entities.caravans;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.util.FBroadcastUtil;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionStructure;
import de.erethon.factions.region.WarRegion;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

public class CaravanRouting implements Listener {

    private static final int TIME_BETWEEN_NODES = 20 * 60;
    private static final int MAX_NODE_DISTANCE = 16;

    private final Factions plugin = Factions.get();

    private final Set<ActiveCaravanRoute> activeRoutes = new HashSet<>();
    private final Map<RegionStructure, Set<CaravanRoute>> byStart = new HashMap<>();
    private final Map<CaravanChunkPos, ActiveCaravanRoute> chunksPosToRoutes = new HashMap<>();
    private final Map<ActiveCaravanRoute, UUID> realCaravanIds = new HashMap<>();

    public CaravanRouting() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this::updateRoutes, 0, TIME_BETWEEN_NODES);
        loadRoutesFromFile();
        loadActiveRoutesFromDatabase();
    }

    public void addRoute(CaravanRoute route) {
        route = normalizeRoute(route);
        if (!byStart.containsKey(route.start())) {
            byStart.put(route.start(), new HashSet<>());
        }
        byStart.get(route.start()).add(route);
    }

    public void removeRoute(CaravanRoute route) {
        byStart.get(route.start()).remove(route);
    }

    public CaravanRoute replaceRoute(CaravanRoute oldRoute, CaravanRoute newRoute) {
        Set<CaravanRoute> routes = byStart.get(oldRoute.start());
        if (routes == null || !routes.remove(oldRoute)) {
            return null;
        }
        newRoute = normalizeRoute(newRoute);
        routes.add(newRoute);
        return newRoute;
    }

    public CaravanRoute replaceRouteRaw(CaravanRoute oldRoute, CaravanRoute newRoute) {
        Set<CaravanRoute> routes = byStart.get(oldRoute.start());
        if (routes == null || !routes.remove(oldRoute)) {
            return null;
        }
        routes.add(newRoute);
        return newRoute;
    }

    public void addActiveRoute(ActiveCaravanRoute route) {
        activeRoutes.add(route);
        saveActiveRoutesToDatabase();
    }

    public void startRoute(ActiveCaravanRoute route, boolean spawnRealCaravan) {
        addActiveRoute(route);
        if (spawnRealCaravan) {
            spawnRealCaravan(route);
        }
    }

    public void removeActiveRoute(ActiveCaravanRoute route) {
        activeRoutes.remove(route);
        saveActiveRoutesToDatabase();
    }

    public Set<CaravanRoute> getRoutes(RegionStructure start) {
        return byStart.get(start);
    }

    public ActiveCaravanRoute newRouteFromStartToFinish(RegionStructure start, RegionStructure finish, int supplies) {
        Set<CaravanRoute> routes = byStart.get(start);
        if (routes == null) {
            Factions.log("No caravan routes from " + start + " found");
            return null;
        }
        for (CaravanRoute route : routes) {
            if (route.end().equals(finish)) {
                if (route.nodes().length == 0) {
                    Factions.log("Caravan route from " + start + " to " + finish + " has no nodes");
                    return null;
                }
                return new ActiveCaravanRoute(route, route.nodes()[0], supplies);
            }
        }
        Factions.log("No caravan route from " + start + " to " + finish + " found");
        return null;
    }

    public void onCaravanArrived(ActiveCaravanRoute route) {
        if (route.route().end().getRegion() instanceof WarRegion warRegion) {
            int supplies = route.supplies();
            if (warRegion.getRegionalWarTracker().isLowestRankedAlliance(warRegion.getAlliance())) {
                supplies = (int) Math.ceil(supplies * 1.15);
            }
            warRegion.getRegionalWarTracker().addRepairSupplies(supplies);
            warRegion.getRegionalWarTracker().addContribution(warRegion.getRegionalWarTracker().getOperatingFaction(), "caravan_deliveries", 1);
            Factions.log("Caravan delivered " + supplies + " repair supplies to " + warRegion.getName());
            FBroadcastUtil.broadcastWar(FMessage.WAR_CARAVAN_ARRIVED, String.valueOf(supplies), warRegion.getName());
        }
    }

    // We process routes every x seconds, so they "move" through the world
    // Even when they are not in loaded chunks
    private void updateRoutes() {
        Iterator<ActiveCaravanRoute> iterator = activeRoutes.iterator();
        while (iterator.hasNext()) {
            ActiveCaravanRoute route = iterator.next();
            discardStaleVisual(route);
            if (hasRealCaravan(route)) {
                continue;
            }
            CaravanRouteNode[] nodes = route.route().nodes();
            if (nodes.length == 0) {
                continue;
            }
            if (route.isAtEnd()) {
                onCaravanArrived(route);
                iterator.remove();
                discardVisual(route);
                saveActiveRoutesToDatabase();
                continue;
            }
            route.advance();
            saveActiveRoutesToDatabase();
        }
        // Keep a map of chunk positions to routes. If one of those chunks gets loaded, spawn in the real NPC caravan
        chunksPosToRoutes.clear();
        for (ActiveCaravanRoute route : activeRoutes) {
            CaravanRouteNode currentNode = route.currentNode();
            int chunkX = currentNode.x() >> 4;
            int chunkZ = currentNode.z() >> 4;
            chunksPosToRoutes.put(new CaravanChunkPos(chunkX, chunkZ), route);
            if (route.route().start().getRegion().getWorld().isChunkLoaded(chunkX, chunkZ) && !hasRealCaravan(route)) {
                spawnRealCaravan(route);
            }
        }
    }

    // If a chunk gets loaded, check if there is a caravan route that goes through it
    // If there is, spawn in the real caravan so players can see and protect it
    @EventHandler
    private void onEntityChunkLoad(EntitiesLoadEvent event) {
        CaravanChunkPos chunkPos = new CaravanChunkPos(event.getChunk().getX(), event.getChunk().getZ());
        ActiveCaravanRoute route = chunksPosToRoutes.get(chunkPos);
        if (route == null) {
            return;
        }
        if (hasRealCaravan(route)) {
            return;
        }
        // Spawn in the real caravan
        spawnRealCaravan(route);
    }

    public void spawnRealCaravan(ActiveCaravanRoute route) {
        if (route.route().start().getRegion().getAlliance() == null) {
            Factions.log("Cannot spawn caravan for route from " + route.route().start().getName() + ": start region has no alliance");
            return;
        }
        discardVisual(route);
        CaravanRouteNode node = route.currentNode();
        CaravanCarrier carrier = new CaravanCarrier(route.route().start().getRegion().getWorld(), node.x() + 0.5, node.y(), node.z() + 0.5, route.route().start().getRegion().getAlliance(), route, this);
        ((CraftWorld) route.route().start().getRegion().getWorld()).getHandle().addFreshEntity(carrier);
        carrier.startVisuals();
        realCaravanIds.put(route, carrier.getUUID());
    }

    public void onVisualReachedNextNode(ActiveCaravanRoute route, CaravanCarrier carrier) {
        if (!activeRoutes.contains(route)) {
            return;
        }
        UUID activeId = realCaravanIds.get(route);
        if (activeId == null || !activeId.equals(carrier.getUUID())) {
            carrier.discard();
            return;
        }
        route.advance();
        saveActiveRoutesToDatabase();
        if (!route.isAtEnd()) {
            return;
        }
        onCaravanArrived(route);
        activeRoutes.remove(route);
        discardVisual(route);
        saveActiveRoutesToDatabase();
    }

    public void removeRouteWithRealCaravan(ActiveCaravanRoute route) {
        realCaravanIds.remove(route);
        saveActiveRoutesToDatabase();
    }

    private boolean hasRealCaravan(ActiveCaravanRoute route) {
        UUID uuid = realCaravanIds.get(route);
        if (uuid == null) {
            return false;
        }
        return ((CraftWorld) route.route().start().getRegion().getWorld()).getHandle().getEntity(uuid) instanceof CaravanCarrier;
    }

    private void discardStaleVisual(ActiveCaravanRoute route) {
        if (!hasRealCaravan(route)) {
            realCaravanIds.remove(route);
        }
    }

    private void discardVisual(ActiveCaravanRoute route) {
        UUID uuid = realCaravanIds.remove(route);
        if (uuid == null) {
            return;
        }
        net.minecraft.world.entity.Entity entity = ((CraftWorld) route.route().start().getRegion().getWorld()).getHandle().getEntity(uuid);
        if (entity != null) {
            if (entity instanceof CaravanCarrier carrier) {
                carrier.discardVisuals();
            }
            entity.discard();
        }
    }

    private void loadRoutesFromFile() {
        YamlConfiguration cfg = new YamlConfiguration();
        plugin.getDatabaseManager().loadCaravanRoutesState().ifPresent(state -> {
            try {
                cfg.loadFromString(state);
            } catch (Exception e) {
                Factions.log("Failed to load caravan routes from database");
                e.printStackTrace();
            }
        });
        if (!cfg.contains("routes")) {
            return;
        }
        for (String start : cfg.getConfigurationSection("routes").getKeys(false)) {
            int startRegionId = parseRouteRegionId(start);
            if (startRegionId < 0) {
                Factions.log("Could not parse start region " + start + " for caravan route");
                continue;
            }
            Region startRegion = plugin.getRegionManager().getRegionById(startRegionId);
            if (startRegion == null || !(startRegion instanceof WarRegion warStartRegion)) {
                Factions.log("Could not find region " + start + " for caravan route");
                continue;
            }
            for (String end : cfg.getConfigurationSection("routes." + start).getKeys(false)) {
                if ("startStructure".equals(end)) {
                    continue;
                }
                String routePath = "routes." + start + "." + end;
                if (!cfg.isConfigurationSection(routePath)) {
                    continue;
                }
                RegionStructure startStructure = warStartRegion.getStructure(cfg.getString(routePath + ".startStructure"));
                if (startStructure == null) {
                    Factions.log("Could not find start structure " + cfg.getString(routePath + ".startStructure") + " for caravan route");
                    continue;
                }
                int endRegionId = parseRouteRegionId(end);
                if (endRegionId < 0) {
                    Factions.log("Could not parse end region " + end + " for caravan route");
                    continue;
                }
                Region endRegion = plugin.getRegionManager().getRegionById(endRegionId);
                if (endRegion == null || !(endRegion instanceof WarRegion warEndRegion)) {
                    Factions.log("Could not find region " + end + " for caravan route");
                    continue;
                }
                RegionStructure endStructure = warEndRegion.getStructure(cfg.getString(routePath + ".endStructure"));
                if (endStructure == null) {
                    Factions.log("Could not find end structure " + cfg.getString(routePath + ".endStructure") + " for caravan route");
                    continue;
                }
                CaravanRoute route = new CaravanRoute(startStructure, endStructure, deserializeNodes(cfg.getString(routePath + ".nodes", "")));
                addRoute(route);
            }
        }
        saveRoutesToFile();

    }

    public void saveRoutesToFile() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<RegionStructure, Set<CaravanRoute>> entry : byStart.entrySet()) {
            for (CaravanRoute route : entry.getValue()) {
                String startRegion = String.valueOf(route.start().getRegion().getId());
                String endRegion = String.valueOf(route.end().getRegion().getId());
                String startStructure = route.start().getName();
                String endStructure = route.end().getName();
                cfg.set("routes." + startRegion + "." + endRegion + ".startStructure", startStructure);
                cfg.set("routes." + startRegion + "." + endRegion + ".endStructure", endStructure);
                cfg.set("routes." + startRegion + "." + endRegion + ".nodes", serializeNodes(route.nodes()));
            }
        }
        plugin.getDatabaseManager().saveCaravanRoutesState(cfg.saveToString());
    }

    private void loadActiveRoutesFromDatabase() {
        YamlConfiguration cfg = new YamlConfiguration();
        plugin.getDatabaseManager().loadActiveCaravanRoutesState().ifPresent(state -> {
            try {
                cfg.loadFromString(state);
            } catch (Exception e) {
                Factions.log("Failed to load active caravan routes from database");
                e.printStackTrace();
            }
        });
        if (!cfg.contains("active")) {
            return;
        }
        for (String key : cfg.getConfigurationSection("active").getKeys(false)) {
            int startRegionId = cfg.getInt("active." + key + ".startRegion", -1);
            int endRegionId = cfg.getInt("active." + key + ".endRegion", -1);
            String startStructureName = cfg.getString("active." + key + ".startStructure");
            String endStructureName = cfg.getString("active." + key + ".endStructure");
            int currentNodeIndex = cfg.getInt("active." + key + ".currentNodeIndex", 0);
            int supplies = cfg.getInt("active." + key + ".supplies", 0);
            Region startRegion = plugin.getRegionManager().getRegionById(startRegionId);
            Region endRegion = plugin.getRegionManager().getRegionById(endRegionId);
            if (!(startRegion instanceof WarRegion warStart) || !(endRegion instanceof WarRegion warEnd)) {
                continue;
            }
            RegionStructure start = warStart.getStructure(startStructureName);
            RegionStructure end = warEnd.getStructure(endStructureName);
            if (start == null || end == null) {
                continue;
            }
            ActiveCaravanRoute active = newRouteFromStartToFinish(start, end, supplies);
            if (active == null) {
                continue;
            }
            while (active.currentNodeIndex() < currentNodeIndex && !active.isAtEnd()) {
                active.advance();
            }
            activeRoutes.add(active);
        }
    }

    private void saveActiveRoutesToDatabase() {
        YamlConfiguration cfg = new YamlConfiguration();
        int i = 0;
        for (ActiveCaravanRoute route : activeRoutes) {
            String path = "active." + i++;
            cfg.set(path + ".startRegion", route.route().start().getRegion().getId());
            cfg.set(path + ".endRegion", route.route().end().getRegion().getId());
            cfg.set(path + ".startStructure", route.route().start().getName());
            cfg.set(path + ".endStructure", route.route().end().getName());
            cfg.set(path + ".currentNodeIndex", route.currentNodeIndex());
            cfg.set(path + ".supplies", route.supplies());
        }
        plugin.getDatabaseManager().saveActiveCaravanRoutesState(cfg.saveToString());
    }

    private static CaravanRouteNode[] deserializeNodes(String nodes) {
        if (nodes == null || nodes.isBlank()) {
            return new CaravanRouteNode[0];
        }
        String[] nodeStrings = nodes.split(";");
        CaravanRouteNode[] deserialized = new CaravanRouteNode[nodeStrings.length];
        for (int i = 0; i < nodeStrings.length; i++) {
            String[] parts = nodeStrings[i].split(",");
            deserialized[i] = new CaravanRouteNode(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        }
        return deserialized;
    }

    private static String serializeNodes(CaravanRouteNode[] nodes) {
        StringBuilder serialized = new StringBuilder();
        for (CaravanRouteNode node : nodes) {
            if (!serialized.isEmpty()) {
                serialized.append(";");
            }
            serialized.append(node.x()).append(",").append(node.y()).append(",").append(node.z());
        }
        return serialized.toString();
    }

    private CaravanRoute normalizeRoute(CaravanRoute route) {
        org.bukkit.World world = route.start().getRegion().getWorld();
        CaravanRouteNode[] sourceNodes = route.nodes();
        if (sourceNodes.length == 0) {
            sourceNodes = new CaravanRouteNode[] {
                    node(route.start().getCenterPosition()),
                    node(route.end().getCenterPosition())
            };
        }
        List<CaravanRouteNode> normalized = new ArrayList<>();
        CaravanRouteNode previous = normalizeNode(world, sourceNodes[0].x(), sourceNodes[0].z());
        normalized.add(previous);
        for (int i = 1; i < sourceNodes.length; i++) {
            CaravanRouteNode target = normalizeNode(world, sourceNodes[i].x(), sourceNodes[i].z());
            int dx = target.x() - previous.x();
            int dz = target.z() - previous.z();
            int steps = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dz * dz) / MAX_NODE_DISTANCE));
            for (int step = 1; step <= steps; step++) {
                double factor = step / (double) steps;
                int x = (int) Math.round(previous.x() + dx * factor);
                int z = (int) Math.round(previous.z() + dz * factor);
                addNodeIfDistinct(normalized, normalizeNode(world, x, z));
            }
            previous = target;
        }
        return new CaravanRoute(route.start(), route.end(), normalized.toArray(CaravanRouteNode[]::new));
    }

    private CaravanRouteNode node(io.papermc.paper.math.Position position) {
        return new CaravanRouteNode(position.blockX(), position.blockY(), position.blockZ());
    }

    private CaravanRouteNode normalizeNode(org.bukkit.World world, int x, int z) {
        return new CaravanRouteNode(x, world.getHighestBlockYAt(x, z) + 1, z);
    }

    private void addNodeIfDistinct(List<CaravanRouteNode> nodes, CaravanRouteNode node) {
        if (!nodes.isEmpty() && nodes.get(nodes.size() - 1).equals(node)) {
            return;
        }
        nodes.add(node);
    }

    private static int parseRouteRegionId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return -1;
        }
    }


}
