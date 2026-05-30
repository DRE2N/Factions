package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.RegionCache;
import de.erethon.factions.region.WarRegion;
import de.erethon.factions.war.RegionalWarTracker;
import de.erethon.factions.war.WarScore;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WarCommand extends FCommand {

    private static final List<String> SUBCOMMANDS = List.of("nearest", "objectives");

    public WarCommand() {
        setCommand("war");
        setMinMaxArgs(0, 2);
        setPermissionFromName();
        setFUsage(getCommand() + " [nearest|objectives]");
        setDescription("Shows current war state");
        setRegisterSeparately(true);
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        String sub = args.length >= 2 ? args[1].toLowerCase() : "";
        switch (sub) {
            case "nearest" -> nearest(sender);
            case "objectives" -> objectives(sender);
            default -> overview(sender);
        }
    }

    private void overview(CommandSender sender) {
        MessageUtil.sendCenteredMessage(sender, FMessage.WAR_COMMAND_HEADER.message());
        sender.sendMessage(FMessage.WAR_COMMAND_PHASE.message(plugin.getCurrentWarPhase().name()));
        WarScore score = plugin.getWar().getScore();
        for (Alliance alliance : plugin.getAllianceCache()) {
            sender.sendMessage(FMessage.WAR_COMMAND_SCORE_LINE.message(
                    alliance.getName(),
                    String.valueOf(score.getTotalScore(alliance)),
                    String.valueOf(score.getPotentialPointsNextTick(alliance)),
                    String.valueOf(alliance.getWarScore())));
        }
        sender.sendMessage(FMessage.WAR_COMMAND_NEAREST_HINT.message());
    }

    private void nearest(CommandSender sender) {
        FPlayer fPlayer = getFPlayer(sender);
        assurePlayerHasAlliance(fPlayer);
        Player player = fPlayer.getPlayer();
        WarRegion contested = getNearestContested(player.getLocation(), fPlayer.getAlliance());
        if (contested != null) {
            sender.sendMessage(FMessage.WAR_COMMAND_NEAREST_FIGHT.message(contested.getName(), formatLocation(getCenter(contested))));
            return;
        }
        WarRegion waypoint = plugin.getWar().getNearestWaypoint(player.getLocation(), fPlayer.getAlliance());
        if (waypoint != null) {
            sender.sendMessage(FMessage.WAR_COMMAND_NEAREST_WAYPOINT.message(waypoint.getName(), formatLocation(waypoint.getRegionalWarTracker().getWaypointSpawn())));
            return;
        }
        sender.sendMessage(FMessage.WAR_COMMAND_NEAREST_NONE.message());
    }

    private void objectives(CommandSender sender) {
        Alliance viewerAlliance = sender instanceof Player ? getFPlayerRaw(sender).getAlliance() : null;
        getWarRegions().stream()
                .sorted(Comparator.comparingInt((WarRegion region) -> region.getRegionalWarTracker().getRegionValue()).reversed())
                .limit(12)
                .forEach(region -> {
                    RegionalWarTracker tracker = region.getRegionalWarTracker();
                    sender.sendMessage(FMessage.WAR_COMMAND_OBJECTIVE_LINE.message(
                            region.getName(),
                            tracker.getTier().name(),
                            region.getAlliance() == null ? "None" : region.getAlliance().getName(),
                            String.valueOf(tracker.getRegionValue()),
                            tracker.getMapWarState(viewerAlliance)));
                });
    }

    private WarRegion getNearestContested(Location location, Alliance alliance) {
        return getWarRegions().stream()
                .filter(region -> region.getWorld() == location.getWorld())
                .filter(region -> region.getAlliance() == null || region.getAlliance() != alliance || region.getRegionalWarTracker().isContested())
                .filter(region -> region.getRegionalWarTracker().isContested() || region.getRegionalWarTracker().isCaptureUnlocked())
                .min(Comparator.comparingDouble(region -> getCenter(region).distanceSquared(location)))
                .orElse(null);
    }

    private List<WarRegion> getWarRegions() {
        List<WarRegion> regions = new ArrayList<>();
        for (RegionCache cache : plugin.getRegionManager().getCaches().values()) {
            for (de.erethon.factions.region.Region region : cache) {
                if (region instanceof WarRegion warRegion) {
                    regions.add(warRegion);
                }
            }
        }
        return regions;
    }

    private Location getCenter(WarRegion region) {
        if (region.getChunks().isEmpty()) {
            return region.getWorld().getSpawnLocation();
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (de.erethon.factions.region.LazyChunk chunk : region.getChunks()) {
            minX = Math.min(minX, chunk.getX());
            maxX = Math.max(maxX, chunk.getX());
            minZ = Math.min(minZ, chunk.getZ());
            maxZ = Math.max(maxZ, chunk.getZ());
        }
        int x = ((minX + maxX) / 2) << 4;
        int z = ((minZ + maxZ) / 2) << 4;
        return new Location(region.getWorld(), x, region.getWorld().getHighestBlockYAt(x, z), z);
    }

    private String formatLocation(Location location) {
        if (location == null) {
            return "unknown";
        }
        return location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabList(SUBCOMMANDS, args[1]);
        }
        return null;
    }
}
