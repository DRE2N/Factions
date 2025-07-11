package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.misc.JavaUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
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
        setAliases("who", "info", "f");
        setMinMaxArgs(0, 1);
        setConsoleCommand(true);
        setPermissionFromName();
        setFUsage(getCommand() + " ([faction|player])");
        setDescription("Zeigt Informationen der Fraktion an");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Faction faction = args.length == 2 ? getFaction(args[1]) : getFaction(getFPlayer(sender));
        FPlayer fPlayer = getFPlayer(sender);
        sender.sendMessage(Component.empty());
        MessageUtil.sendCenteredMessage(sender, FMessage.CMD_SHOW_HEADER.message(faction.getName(true)));
        MessageUtil.sendCenteredMessage(sender, FMessage.CMD_SHOW_SEPARATOR.message());
        sender.sendMessage(FMessage.CMD_SHOW_SHORT_NAME.message(faction.getDisplayShortName()));
        sender.sendMessage(FMessage.CMD_SHOW_LONG_NAME.message(faction.getDisplayLongName()));
        sender.sendMessage(FMessage.CMD_SHOW_DESCRIPTION.message(faction.getDisplayDescription()));
        sender.sendMessage(FMessage.CMD_SHOW_ALLIANCE.message(faction.getAlliance().asComponent(fPlayer)));
        sender.sendMessage(FMessage.CMD_SHOW_LEVEL.message(faction.getLevel().displayName()));
        sender.sendMessage(FMessage.CMD_SHOW_MONEY.message(faction.getFAccount().getFormatted(),
                faction.hasCurrentTaxDebt() ? " (" + faction.getFAccount().getFormatted(faction.getCurrentTaxDebt()) + ")" : ""));
        sender.sendMessage(FMessage.CMD_SHOW_CORE_REGION.message(faction.getCoreRegion().asComponent(fPlayer)));
        sender.sendMessage(FMessage.CMD_SHOW_ADMIN.message(getDisplayName(faction, faction.getAdmin())));
        sender.sendMessage(FMessage.CMD_SHOW_MEMBERS.message(faction.getMembers().size() + "/" + faction.getMaxMembers(), getMembersString(faction)));
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
