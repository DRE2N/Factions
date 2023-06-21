package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.misc.JavaUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.faction.Faction;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * @author Fyreum
 */
public class ShowCommand extends FCommand {

    public ShowCommand() {
        setCommand("show");
        setAliases("who", "info");
        setMinMaxArgs(0, 1);
        setConsoleCommand(true);
        setPermissionFromName();
        setFUsage(getCommand() + " ([faction|player])");
        setDescription("Zeigt Informationen der Fraktion an");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Faction faction = args.length == 2 ? getFaction(args[1]) : getFaction(getFPlayer(sender));
        sender.sendMessage(Component.empty());
        MessageUtil.sendCenteredMessage(sender, FMessage.CMD_SHOW_HEADER.getMessage(faction.getName()));
        MessageUtil.sendCenteredMessage(sender, FMessage.CMD_SHOW_SEPARATOR.getMessage());
        sender.sendMessage(FMessage.CMD_SHOW_SHORT_NAME.message(faction.getDisplayShortName()));
        sender.sendMessage(FMessage.CMD_SHOW_LONG_NAME.message(faction.getDisplayLongName()));
        sender.sendMessage(FMessage.CMD_SHOW_DESCRIPTION.message(faction.getDisplayDescription()));
        sender.sendMessage(FMessage.CMD_SHOW_ALLIANCE.message(faction.hasAlliance() ? faction.getAlliance().getName() : FMessage.GENERAL_NONE.getMessage()));
        sender.sendMessage(FMessage.CMD_SHOW_LEVEL.message(faction.getLevel().getName()));
        sender.sendMessage(FMessage.CMD_SHOW_MONEY.message(faction.getFAccount().getFormatted(),
                faction.hasCurrentTaxDebt() ? " (" + faction.getFAccount().getFormatted(faction.getCurrentTaxDebt()) + ")" : ""));
        sender.sendMessage(FMessage.CMD_SHOW_CORE_REGION.message(faction.getCoreRegion().getName()));
        sender.sendMessage(FMessage.CMD_SHOW_ADMIN.message(getDisplayName(faction, faction.getAdmin())));
        sender.sendMessage(FMessage.CMD_SHOW_MEMBERS.message(String.valueOf(faction.getMembers().size()), getMembersString(faction)));
        sender.sendMessage(Component.empty());
    }

    private String getMembersString(Faction faction) {
        return JavaUtil.toString(faction.getMembers().stream().map(uuid -> getDisplayName(faction, uuid)).toList());
    }

    private String getDisplayName(Faction faction, UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return (faction.isAdmin(uuid) ? "**" : faction.isMod(uuid) ? "*" : "") + (player == null ? uuid.toString() : player.getName());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabFactions(args[1]);
        }
        return null;
    }
}
