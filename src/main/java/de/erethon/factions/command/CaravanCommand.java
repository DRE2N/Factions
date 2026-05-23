package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionStructure;
import de.erethon.factions.region.WarRegion;
import de.erethon.factions.util.FPermissionUtil;
import de.erethon.factions.war.entities.caravans.ActiveCaravanRoute;
import de.erethon.factions.war.entities.caravans.CaravanRoute;
import de.erethon.factions.war.entities.caravans.CaravanRouteNode;
import de.erethon.factions.war.entities.caravans.CaravanRouting;
import io.papermc.paper.math.Position;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class CaravanCommand extends FCommand {

    private static final List<String> SUBCOMMANDS = List.of("route", "spawn", "deliver", "show", "list", "waypoint");

    public CaravanCommand() {
        setCommand("caravan");
        setAliases("caravans");
        setMinMaxArgs(0, Integer.MAX_VALUE);
        setPermissionFromName();
        setFUsage(getCommand() + " [route|spawn|deliver|show|list|waypoint]");
        setDescription("Admin commands for war supply caravans");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        assure(FPermissionUtil.isBypass(sender), de.erethon.factions.data.FMessage.ERROR_NO_PERMISSION);
        CaravanRouting routing = plugin.getWar().getCaravanRouting();
        if (routing == null) {
            sender.sendMessage(Component.text("Caravan routing is not initialized. Check that the Theanor region exists and war loaded cleanly."));
            return;
        }
        String sub = args.length >= 2 ? args[1].toLowerCase() : "list";
        switch (sub) {
            case "route" -> createRoute(sender, args, routing);
            case "spawn" -> spawnRoute(sender, args, routing, true);
            case "deliver" -> spawnRoute(sender, args, routing, false);
            case "show" -> showRoute(sender, args, routing);
            case "list" -> listRoutes(sender, args, routing);
            case "waypoint" -> waypoint(sender, args, routing);
            default -> displayHelp(sender);
        }
    }

    private void createRoute(CommandSender sender, String[] args, CaravanRouting routing) {
        RouteSelection selection = routeSelection(args);
        CaravanRoute route = new CaravanRoute(selection.start(), selection.end(), nodes(args, selection));
        routing.addRoute(route);
        routing.saveRoutesToFile();
        sender.sendMessage(Component.text("Created caravan route from "
                + selection.start().getRegion().getName() + "/" + selection.start().getName()
                + " to " + selection.end().getRegion().getName() + "/" + selection.end().getName()
                + " with " + route.nodes().length + " nodes."));
    }

    private void spawnRoute(CommandSender sender, String[] args, CaravanRouting routing, boolean spawnRealCaravan) {
        RouteSelection selection = routeSelection(args);
        int supplies = args.length >= 7 ? parseInt(args[6]) : 5;
        ActiveCaravanRoute route = routing.newRouteFromStartToFinish(selection.start(), selection.end(), supplies);
        if (route == null) {
            sender.sendMessage(Component.text("No stored caravan route found for that start/end pair."));
            return;
        }
        if (spawnRealCaravan) {
            routing.startRoute(route, true);
            sender.sendMessage(Component.text("Started caravan with " + supplies + " supplies."));
            return;
        }
        routing.onCaravanArrived(route);
        sender.sendMessage(Component.text("Delivered " + supplies + " repair supplies immediately."));
    }

    private void showRoute(CommandSender sender, String[] args, CaravanRouting routing) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can show caravan routes."));
            return;
        }
        RouteSelection selection = routeSelection(args);
        CaravanRoute route = route(routing, selection);
        if (route == null) {
            sender.sendMessage(Component.text("No stored caravan route found for that start/end pair."));
            return;
        }
        sender.sendMessage(Component.text("Showing caravan route for 10 seconds."));
        new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                drawRoute(player, route);
                if (++ticks >= 10) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    private void listRoutes(CommandSender sender, String[] args, CaravanRouting routing) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /f caravan list <startRegion> <startStructure>"));
            return;
        }
        RegionStructure start = structure(args[2], args[3]);
        Set<CaravanRoute> routes = routing.getRoutes(start);
        if (routes == null || routes.isEmpty()) {
            sender.sendMessage(Component.text("No caravan routes from " + start.getRegion().getName() + "/" + start.getName() + "."));
            return;
        }
        for (CaravanRoute route : routes) {
            sender.sendMessage(Component.text(start.getRegion().getName() + "/" + start.getName()
                    + " -> " + route.end().getRegion().getName() + "/" + route.end().getName()
                    + " (" + route.nodes().length + " nodes)"));
        }
    }

    private void waypoint(CommandSender sender, String[] args, CaravanRouting routing) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can edit caravan waypoints."));
            return;
        }
        WaypointEdit edit = waypointEdit(args);
        if (!"move".equals(edit.action()) || !"here".equals(edit.target())) {
            sender.sendMessage(Component.text("Usage: /f caravan waypoint move here <startRegion> <startStructure> <endRegion> <endStructure>"));
            return;
        }
        CaravanRoute route = route(routing, edit.selection());
        if (route == null) {
            sender.sendMessage(Component.text("No stored caravan route found for that start/end pair."));
            return;
        }
        CaravanRouteNode[] nodes = route.nodes().clone();
        if (nodes.length == 0) {
            sender.sendMessage(Component.text("That caravan route has no waypoints."));
            return;
        }
        int index = edit.waypointIndex() == null ? nearestNodeIndex(player.getLocation(), route) : edit.waypointIndex();
        if (index < 0 || index >= nodes.length) {
            sender.sendMessage(Component.text("Waypoint #" + index + " does not exist. Valid range: 0-" + (nodes.length - 1) + "."));
            return;
        }
        CaravanRouteNode oldNode = nodes[index];
        CaravanRouteNode newNode = node(player.getLocation());
        nodes[index] = newNode;
        CaravanRoute updated = routing.replaceRouteRaw(route, new CaravanRoute(route.start(), route.end(), nodes));
        if (updated == null) {
            sender.sendMessage(Component.text("Failed to update caravan route."));
            return;
        }
        routing.saveRoutesToFile();
        sender.sendMessage(Component.text("Moved caravan waypoint #" + index + " from "
                + formatNode(oldNode) + " to " + formatNode(newNode) + "."));
        showRoute(player, updated);
    }

    private RouteSelection routeSelection(String[] args) {
        assure(args.length >= 6, de.erethon.factions.data.FMessage.ERROR_REGION_STRUCTURE_WRONG_ARG_FORMAT);
        return new RouteSelection(structure(args[2], args[3]), structure(args[4], args[5]));
    }

    private CaravanRoute route(CaravanRouting routing, RouteSelection selection) {
        Set<CaravanRoute> routes = routing.getRoutes(selection.start());
        if (routes == null) {
            return null;
        }
        return routes.stream()
                .filter(route -> route.end().equals(selection.end()))
                .findFirst()
                .orElse(null);
    }

    private RegionStructure structure(String regionName, String structureName) {
        Region region = getRegion(regionName);
        assure(region instanceof WarRegion, de.erethon.factions.data.FMessage.ERROR_REGION_IS_NOT_A_WARZONE);
        RegionStructure structure = ((WarRegion) region).getStructure(structureName);
        assure(structure != null, de.erethon.factions.data.FMessage.ERROR_WAR_OBJECTIVE_NOT_FOUND, structureName);
        return structure;
    }

    private CaravanRouteNode[] nodes(String[] args, RouteSelection selection) {
        if (args.length >= 7) {
            return parseNodes(args[6]);
        }
        return new CaravanRouteNode[] {
                node(selection.start().getCenterPosition()),
                node(selection.end().getCenterPosition())
        };
    }

    private CaravanRouteNode[] parseNodes(String raw) {
        return Arrays.stream(raw.split(";"))
                .filter(node -> !node.isBlank())
                .map(node -> {
                    String[] parts = node.split(",");
                    assure(parts.length == 3, de.erethon.factions.data.FMessage.ERROR_REGION_STRUCTURE_WRONG_ARG_FORMAT);
                    return new CaravanRouteNode(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                })
                .toArray(CaravanRouteNode[]::new);
    }

    private CaravanRouteNode node(Position position) {
        return new CaravanRouteNode(position.blockX(), position.blockY(), position.blockZ());
    }

    private CaravanRouteNode node(Location location) {
        int x = location.getBlockX();
        int z = location.getBlockZ();
        return new CaravanRouteNode(x, location.getWorld().getHighestBlockYAt(x, z) + 1, z);
    }

    private int nearestNodeIndex(Location location, CaravanRoute route) {
        CaravanRouteNode[] nodes = route.nodes();
        int nearest = 0;
        double nearestDistance = Double.MAX_VALUE;
        for (int i = 0; i < nodes.length; i++) {
            CaravanRouteNode node = nodes[i];
            double dx = location.getX() - (node.x() + 0.5);
            double dy = location.getY() - node.y();
            double dz = location.getZ() - (node.z() + 0.5);
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = i;
            }
        }
        return nearest;
    }

    private String formatNode(CaravanRouteNode node) {
        return node.x() + "," + node.y() + "," + node.z();
    }

    private void showRoute(Player player, CaravanRoute route) {
        new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                drawRoute(player, route);
                if (++ticks >= 10) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    private void drawRoute(Player player, CaravanRoute route) {
        CaravanRouteNode[] nodes = route.nodes();
        if (nodes.length == 0) {
            return;
        }
        World world = route.start().getRegion().getWorld();
        for (CaravanRouteNode node : nodes) {
            player.spawnParticle(Particle.HAPPY_VILLAGER, new Location(world, node.x() + 0.5, node.y() + 0.2, node.z() + 0.5), 8, 0.4, 0.2, 0.4, 0.0);
        }
        for (int i = 0; i < nodes.length - 1; i++) {
            drawSegment(player, world, nodes[i], nodes[i + 1]);
        }
    }

    private void drawSegment(Player player, World world, CaravanRouteNode start, CaravanRouteNode end) {
        double startX = start.x() + 0.5;
        double startY = start.y() + 0.2;
        double startZ = start.z() + 0.5;
        double dx = end.x() + 0.5 - startX;
        double dy = end.y() + 0.2 - startY;
        double dz = end.z() + 0.5 - startZ;
        int steps = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz)));
        for (int step = 0; step <= steps; step++) {
            double factor = step / (double) steps;
            player.spawnParticle(Particle.HAPPY_VILLAGER, new Location(world, startX + dx * factor, startY + dy * factor, startZ + dz * factor), 1, 0, 0, 0, 0);
        }
    }

    private WaypointEdit waypointEdit(String[] args) {
        assure(args.length >= 8, de.erethon.factions.data.FMessage.ERROR_REGION_STRUCTURE_WRONG_ARG_FORMAT);
        if ("move".equalsIgnoreCase(args[2]) && "here".equalsIgnoreCase(args[3])) {
            return new WaypointEdit(args[2].toLowerCase(), args[3].toLowerCase(),
                    new RouteSelection(structure(args[4], args[5]), structure(args[6], args[7])),
                    args.length >= 9 ? parseWaypointIndex(args[8]) : null);
        }
        if ("move".equalsIgnoreCase(args[6]) && "here".equalsIgnoreCase(args[7])) {
            return new WaypointEdit(args[6].toLowerCase(), args[7].toLowerCase(),
                    new RouteSelection(structure(args[2], args[3]), structure(args[4], args[5])),
                    args.length >= 9 ? parseWaypointIndex(args[8]) : null);
        }
        return new WaypointEdit("", "", routeSelection(args), null);
    }

    private int parseWaypointIndex(String raw) {
        if (raw.startsWith("#")) {
            raw = raw.substring(1);
        }
        return parseInt(raw);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabList(SUBCOMMANDS, args[1]);
        }
        if (args.length == 3 && "waypoint".equalsIgnoreCase(args[1])) {
            return getTabList(List.of("move"), args[2]);
        }
        if (args.length == 4 && "waypoint".equalsIgnoreCase(args[1]) && "move".equalsIgnoreCase(args[2])) {
            return getTabList(List.of("here"), args[3]);
        }
        if ("waypoint".equalsIgnoreCase(args[1]) && args.length >= 5) {
            int shiftedLength = args.length - 2;
            if (shiftedLength == 3 || shiftedLength == 5) {
                if (sender instanceof Player player) {
                    return getTabRegions(player, args[args.length - 1]);
                }
                return getTabRegions(args[args.length - 1]);
            }
            if (shiftedLength == 4) {
                return getTabList(structureNames(sender, args[4]), args[5]);
            }
            if (shiftedLength == 6) {
                return getTabList(structureNames(sender, args[6]), args[7]);
            }
            if (shiftedLength == 7) {
                return getTabList(waypointIndexes(args), args[8]);
            }
            return null;
        }
        if (args.length == 3 || args.length == 5) {
            if (sender instanceof Player player) {
                return getTabRegions(player, args[args.length - 1]);
            }
            return getTabRegions(args[args.length - 1]);
        }
        if (args.length == 4) {
            return getTabList(structureNames(sender, args[2]), args[3]);
        }
        if (args.length == 6) {
            return getTabList(structureNames(sender, args[4]), args[5]);
        }
        return null;
    }

    private List<String> structureNames(CommandSender sender, String regionArg) {
        Region region = regionForTab(sender, regionArg);
        if (!(region instanceof WarRegion warRegion)) {
            return List.of();
        }
        return new java.util.ArrayList<>(warRegion.getStructures().keySet());
    }

    private Region regionForTab(CommandSender sender, String regionArg) {
        if ("here".equalsIgnoreCase(regionArg) && sender instanceof Player player) {
            return plugin.getRegionManager().getRegionByLocation(player.getLocation());
        }
        return plugin.getRegionManager().getRegionByName(regionArg);
    }

    private List<String> waypointIndexes(String[] args) {
        try {
            WaypointEdit edit = waypointEdit(args);
            CaravanRoute route = route(plugin.getWar().getCaravanRouting(), edit.selection());
            if (route == null) {
                return List.of();
            }
            List<String> indexes = new java.util.ArrayList<>();
            for (int i = 0; i < route.nodes().length; i++) {
                indexes.add(String.valueOf(i));
            }
            return indexes;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private record RouteSelection(RegionStructure start, RegionStructure end) {
    }

    private record WaypointEdit(String action, String target, RouteSelection selection, Integer waypointIndex) {
    }
}
