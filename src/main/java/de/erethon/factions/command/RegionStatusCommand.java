package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.region.Region;
import de.erethon.factions.war.RegionalWarTracker;
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
        assure(region.getType().isWarGround(), FMessage.ERROR_REGION_IS_NOT_A_WARZONE);
        RegionalWarTracker tracker = region.getRegionalWarTracker();

        MessageUtil.sendCenteredMessage(sender, FMessage.CMD_REGION_STATUS_HEADER.message(region.getName()));
        Alliance leader = tracker.getLeader();
        sender.sendMessage(FMessage.CMD_REGION_STATUS_CAPTURE_CAP.message(String.valueOf(tracker.getCaptureCap())));
        sender.sendMessage(FMessage.CMD_REGION_STATUS_LEADER.message(leader == null ? FMessage.GENERAL_NONE.message() : leader.getColoredName()));

        for (Alliance alliance : tracker.getScores().keySet()) {
            sender.sendMessage(FMessage.CMD_REGION_STATUS_ALLIANCE_HEADER.message(alliance.getColoredName()));
            sender.sendMessage(FMessage.CMD_REGION_STATUS_SCORE.message(String.valueOf(tracker.getScore(alliance)), String.valueOf(tracker.getScoreAsPercentage(alliance))));
            sender.sendMessage(FMessage.CMD_REGION_STATUS_KILLS.message(String.valueOf(tracker.getKills(alliance)), String.valueOf(tracker.getKillsAsScore(alliance))));
        }
    }
}
