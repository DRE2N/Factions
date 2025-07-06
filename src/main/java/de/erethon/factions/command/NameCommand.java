package de.erethon.factions.command;

import de.erethon.aergia.util.BroadcastUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.dialog.EditNameDialog;
import de.erethon.factions.faction.Faction;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * @author Fyreum
 */
public class NameCommand extends FCommand {

    public NameCommand() {
        setCommand("name");
        setAliases("n");
        setMinMaxArgs(0, 1);
        setConsoleCommand(false);
        setPermissionFromName();
        setFUsage(getCommand() + " ([faction])");
        setDescription("Setzt den Namen der Fraktion");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Faction faction = args.length == 1 ? getFaction(getFPlayer(sender)) : getFaction(args[1]);
        assureSenderHasAdminPerms(sender, faction);
        Player player = (Player) sender;
        if (true) {
           EditNameDialog.show(getFPlayer(sender), faction, player);
           return;
        }
        int maximumChars = plugin.getFConfig().getMaximumNameChars();
        assure(args[1].length() <= maximumChars, FMessage.ERROR_TEXT_IS_TOO_LONG, String.valueOf(maximumChars));
        assure(plugin.getFactionCache().getByName(args[1]) == null, FMessage.ERROR_NAME_IN_USE, args[1]);
        String oldName = faction.getName();
        faction.setName(args[1]);
        BroadcastUtil.broadcast(FMessage.FACTION_INFO_NAME_CHANGED.message(oldName, args[1]));
    }
}
