package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import org.bukkit.command.CommandSender;

import java.util.List;

public class RegionLevelCommand extends FCommand {

    public RegionLevelCommand() {
        setCommand("level");
        setAliases("lvl", "lv");
        setMinMaxArgs(1, 2);
        setPermissionFromName(RegionCommand.LABEL);
        setFUsage(RegionCommand.LABEL + " " + getCommand() + "[minLevel] " + "[maxLevel]");
        setDescription("Setzt das Level der Region");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        Region region = getRegion(fPlayer);
        if (args.length > 3) {
            displayHelp(sender);
            return;
        }
        int minLevel;
        int maxLevel;
        try {
            minLevel = Integer.parseInt(args[1]);
            if (args.length == 3) {
                maxLevel = Integer.parseInt(args[2]);
            } else {
                maxLevel = minLevel;
            }
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(sender, "<red>Bitte gebe eine Zahl an.");
            return;
        }
        if (minLevel < 1 || maxLevel < 1) {
            MessageUtil.sendMessage(sender, "<red>Das Level muss mindestens 1 sein.");
            return;
        }
        if (minLevel > maxLevel) {
            MessageUtil.sendMessage(sender, "<red>Das maximale Level muss größer als das minimale Level sein.");
            return;
        }
        region.setMinMaxLevel(minLevel, maxLevel);
        MessageUtil.sendMessage(sender, "<green>Du hast das Level der Region auf " + minLevel + " - " + maxLevel + " gesetzt.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
