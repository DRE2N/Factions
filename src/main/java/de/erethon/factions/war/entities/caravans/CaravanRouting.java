package de.erethon.factions.war.entities.caravans;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionStructure;
import de.erethon.factions.region.WarRegion;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class CaravanRouting implements Listener {

    private static final int TIME_BETWEEN_NODES = 20 * 60;

    private final Factions plugin = Factions.get();

    private final File routeStorage = new File(plugin.getDataFolder(), "caravanRoutes.yml");

    private final Set<ActiveCaravanRoute> activeRoutes = new HashSet<>();
    private final Map<RegionStructure, Set<CaravanRoute>> byStart = new HashMap<>();
    private final Map<CaravanChunkPos, ActiveCaravanRoute> chunksPosToRoutes = new HashMap<>();
    private final Set<ActiveCaravanRoute> routesWithRealCaravans = new HashSet<>();

    public CaravanRouting() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this::updateRoutes, 0, TIME_BETWEEN_NODES);
        loadRoutesFromFile();
    }

    public void addRoute(CaravanRoute route) {
        if (!byStart.containsKey(route.start())) {
            byStart.put(route.start(), new HashSet<>());
        }
        byStart.get(route.start()).add(route);
    }

    public void removeRoute(CaravanRoute route) {
        byStart.get(route.start()).remove(route);
    }

    public void addActiveRoute(ActiveCaravanRoute route) {
        activeRoutes.add(route);
    }

    public void removeActiveRoute(ActiveCaravanRoute route) {
        activeRoutes.remove(route);
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
        // route.route().end().getRegion() - add supplies to the region
    }

    // We process routes every x seconds, so they "move" through the world
    // Even when they are not in loaded chunks
    private void updateRoutes() {
        Iterator<ActiveCaravanRoute> iterator = activeRoutes.iterator();
        while (iterator.hasNext()) {
            if (routesWithRealCaravans.contains(iterator.next())) { // Skip routes that have real caravans
                continue;
            }
            ActiveCaravanRoute route = iterator.next();
            CaravanRouteNode[] nodes = route.route().nodes();
            if (nodes.length == 0) {
                continue;
            }
            if (route.isAtEnd()) {
                onCaravanArrived(route);
                iterator.remove();
                continue;
            }
            route.advance();
        }
        // Keep a map of chunk positions to routes. If one of those chunks gets loaded, spawn in the real NPC caravan
        chunksPosToRoutes.clear();
        for (ActiveCaravanRoute route : activeRoutes) {
            CaravanRouteNode currentNode = route.currentNode();
            int chunkX = currentNode.x() >> 4;
            int chunkZ = currentNode.z() >> 4;
            chunksPosToRoutes.put(new CaravanChunkPos(chunkX, chunkZ), route);
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
        // Spawn in the real caravan
        CaravanCarrier carrier = new CaravanCarrier(event.getWorld(), route.currentNode().x(), route.currentNode().y(), route.currentNode().z(), route.route().start().getRegion().getAlliance(), route,this);
        routesWithRealCaravans.add(route);
    }

    public void removeRouteWithRealCaravan(ActiveCaravanRoute route) {
        routesWithRealCaravans.remove(route);
    }

    private void loadRoutesFromFile() {
        if (!routeStorage.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(routeStorage);
        if (!cfg.contains("routes")) {
            return;
        }
        for (String start : cfg.getConfigurationSection("routes").getKeys(false)) {
            Region startRegion = plugin.getRegionManager().getRegionById(Integer.parseInt(start));
            if (startRegion == null || !(startRegion instanceof WarRegion warStartRegion)) {
                Factions.log("Could not find region " + start + " for caravan route");
                continue;
            }
            RegionStructure startStructure = warStartRegion.getStructure(cfg.getString("routes." + start + ".startStructure"));
            if (startStructure == null) {
                Factions.log("Could not find structure " + cfg.getString("routes." + start + ".structure") + " for caravan route");
                continue;
            }
            for (String end : cfg.getConfigurationSection("routes." + start).getKeys(false)) {
                Region endRegion = plugin.getRegionManager().getRegionById(Integer.parseInt(end));
                if (endRegion == null || !(endRegion instanceof WarRegion warEndRegion)) {
                    Factions.log("Could not find region " + end + " for caravan route");
                    continue;
                }
                RegionStructure endStructure = warEndRegion.getStructure(cfg.getString("routes." + start + "." + end + ".endStructure"));
                if (endStructure == null) {
                    Factions.log("Could not find structure " + cfg.getString("routes." + start + "." + end + ".structure") + " for caravan route");
                    continue;
                }
                CaravanRoute route = new CaravanRoute(startStructure, endStructure, deserializeNodes(cfg.getString("routes." + start + "." + end + ".nodes")));
                addRoute(route);
            }
        }

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
        try {
            cfg.save(routeStorage);
        } catch (Exception e) {
            Factions.log("Failed to save caravan routes to file");
            e.printStackTrace();
        }

    }

    private static CaravanRouteNode[] deserializeNodes(String nodes) {
        String[] nodeStrings = nodes.split(";");
        CaravanRouteNode[] deserialized = new CaravanRouteNode[nodeStrings.length];
        for (int i = 0; i < nodeStrings.length; i++) {
            String[] parts = nodeStrings[i].split(",");
            deserialized[i] = new CaravanRouteNode(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        }
        return deserialized;
    }

    private static String serializeNodes(CaravanRouteNode[] nodes) {
        String serialized = "";
        for (CaravanRouteNode node : nodes) {
            // Serialize each node
            serialized += node.x() + "," + node.y() + "," + node.z() + ";";
        }
        return serialized;
    }


}
