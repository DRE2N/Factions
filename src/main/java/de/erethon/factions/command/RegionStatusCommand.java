package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.WarRegion;
import de.erethon.factions.war.RegionalWarTracker;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class RegionStatusCommand extends FCommand {

    public RegionStatusCommand() {
        setCommand("status");
        setMinMaxArgs(0, 1);
        setConsoleCommand(true);
        setPermissionFromName(RegionCommand.LABEL);
        setFUsage(RegionCommand.LABEL + " " + getCommand() + " ([region])");
        setDescription("Zeigt den Kampfstatus der Region an");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Region region = args.length == 2 ? getRegion(args[1]) : getRegion(getFPlayer(sender));
        assure(region instanceof WarRegion, FMessage.ERROR_REGION_IS_NOT_A_WARZONE);
        WarRegion warRegion = (WarRegion) region;
        RegionalWarTracker tracker = warRegion.getRegionalWarTracker();

        MessageUtil.sendCenteredMessage(sender, FMessage.CMD_REGION_STATUS_HEADER.message(region.getName()));
        Alliance leader = tracker.getLeader();
        sender.sendMessage(FMessage.CMD_REGION_STATUS_CAPTURE_CAP.message(String.valueOf(tracker.getCaptureCap())));
        sender.sendMessage(FMessage.CMD_REGION_STATUS_LEADER.message(leader == null ? "None" : leader.getName()));
        sender.sendMessage(FMessage.WAR_OBJECTIVE_INFO_TIER.message(tracker.getTier().name()));
        sender.sendMessage(warRegion.getAlliance() == null
                ? FMessage.WAR_OBJECTIVE_INFO_OWNER_NONE.message()
                : FMessage.WAR_OBJECTIVE_INFO_OWNER.message(warRegion.getAlliance().getName()));
        sender.sendMessage(FMessage.WAR_OBJECTIVE_INFO_POINT_VALUE.message(String.valueOf(tracker.getRegionValue())));
        sender.sendMessage(tracker.isCaptureUnlocked()
                ? FMessage.WAR_OBJECTIVE_INFO_CAPTURE_UNLOCKED_YES.message()
                : FMessage.WAR_OBJECTIVE_INFO_CAPTURE_UNLOCKED_NO.message());
        sender.sendMessage(tracker.isContested()
                ? FMessage.WAR_OBJECTIVE_INFO_CONTESTED_YES.message(formatPercent(tracker.getHighestCaptureProgressPercent()))
                : FMessage.WAR_OBJECTIVE_INFO_CONTESTED_NO.message(formatPercent(tracker.getHighestCaptureProgressPercent())));
        sender.sendMessage(waypointAvailability(tracker, warRegion.getAlliance()));
        sender.sendMessage(FMessage.WAR_OBJECTIVE_INFO_REPAIR_SUPPLIES.message(String.valueOf(tracker.getRepairSupplies())));

        for (Alliance alliance : tracker.getScores().keySet()) {
            sender.sendMessage(FMessage.CMD_REGION_STATUS_ALLIANCE_HEADER.message(alliance.getColoredName()));
            sender.sendMessage(FMessage.CMD_REGION_STATUS_SCORE.message(String.valueOf(tracker.getScore(alliance)), String.valueOf(tracker.getScoreAsPercentage(alliance))));
            sender.sendMessage(FMessage.CMD_REGION_STATUS_KILLS.message(String.valueOf(tracker.getKills(alliance)), String.valueOf(tracker.getKillsAsScore(alliance))));
        }
    }

    private Component waypointAvailability(RegionalWarTracker tracker, Alliance alliance) {
        if (tracker.getWaypointSpawn() == null) {
            return FMessage.WAR_OBJECTIVE_INFO_WAYPOINT_AVAILABLE_NOT_SET.message();
        }
        return tracker.isWaypointAvailable(alliance)
                ? FMessage.WAR_OBJECTIVE_INFO_WAYPOINT_AVAILABLE_YES.message()
                : FMessage.WAR_OBJECTIVE_INFO_WAYPOINT_AVAILABLE_NO.message();
    }

    private String formatPercent(double percent) {
        return String.format(java.util.Locale.ROOT, "%.1f", percent);
    }
}
