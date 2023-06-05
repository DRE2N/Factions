package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.war.WarStats;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * @author Fyreum
 */
public class StatsCommand extends FCommand {

    public StatsCommand() {
        setCommand("stats");
        setMinMaxArgs(0, 1);
        setConsoleCommand(true);
        setPermissionFromName();
        setFUsage(getCommand() + " ([player])");
        setDescription("Zeigt die Kampf-Stats eines Spieles");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = args.length == 2 ? getFPlayer(args[1]) : getFPlayer(sender);
        WarStats stats = fPlayer.getWarStats();
        sender.sendMessage(Component.empty());
        MessageUtil.sendCenteredMessage(sender, FMessage.CMD_STATS_HEADER.message(fPlayer.getLastName()));
        MessageUtil.sendCenteredMessage(sender, FMessage.CMD_STATS_SEPARATOR.message());
        sender.sendMessage(FMessage.CMD_STATS_KD.message(String.valueOf(stats.getKills()), String.valueOf(stats.getDeaths()), String.valueOf(stats.getKDRatio())));
        sender.sendMessage(FMessage.CMD_STATS_KILL_STREAK.message(String.valueOf(stats.getKillStreak())));
        sender.sendMessage(FMessage.CMD_STATS_HIGHEST_KILL_STREAK.message(String.valueOf(stats.getHighestKillStreak())));
        sender.sendMessage(FMessage.CMD_STATS_CAPTURING_TIME.message(String.valueOf(stats.getCapturingTime())));
        sender.sendMessage(Component.empty());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabPlayers(args[1]);
        }
        return null;
    }
}
