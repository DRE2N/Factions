package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.misc.JavaUtil;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.entity.FLegalEntity;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * @author Fyreum
 */
public class AllianceShowCommand extends FCommand {

    public AllianceShowCommand() {
        setCommand("show");
        setAliases("who", "info");
        setMinMaxArgs(0, 1);
        setConsoleCommand(true);
        setPermissionFromName(AllianceCommand.LABEL);
        setFUsage(getCommand() + " ([alliance|player])");
        setDescription("Zeigt Informationen der Allianz an");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Alliance alliance = args.length == 2 ? getAlliance(args[1]) : getAlliance(getFPlayer(sender));
        sender.sendMessage(Component.empty());
        MessageUtil.sendCenteredMessage(sender, FMessage.CMD_ALLIANCE_SHOW_HEADER.getMessage(alliance.getName()));
        MessageUtil.sendCenteredMessage(sender, FMessage.CMD_ALLIANCE_SHOW_SEPARATOR.getMessage());
        sender.sendMessage(FMessage.CMD_ALLIANCE_SHOW_SHORT_NAME.message(alliance.getDisplayShortName()));
        sender.sendMessage(FMessage.CMD_ALLIANCE_SHOW_LONG_NAME.message(alliance.getDisplayLongName()));
        sender.sendMessage(FMessage.CMD_ALLIANCE_SHOW_DESCRIPTION.message(alliance.getDisplayDescription()));
        sender.sendMessage(FMessage.CMD_ALLIANCE_SHOW_MONEY.message(alliance.getFAccount().getFormatted()));
        sender.sendMessage(FMessage.CMD_ALLIANCE_SHOW_MEMBERS.message(String.valueOf(alliance.getFactions().size()), getFactionsString(alliance)));
        sender.sendMessage(Component.empty());
    }

    private String getFactionsString(Alliance alliance) {
        return JavaUtil.toString(alliance.getFactions().stream().map(FLegalEntity::getName).toList());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabAlliances(args[1]);
        }
        return null;
    }
}
