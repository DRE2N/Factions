package de.erethon.factions.command;

import de.erethon.aergia.util.BroadcastUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.faction.Faction;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * @author Fyreum
 */
public class NameCommand extends FCommand {

    public NameCommand() {
        setCommand("name");
        setAliases("n");
        setMinMaxArgs(1, 2);
        setConsoleCommand(true);
        setPermissionFromName();
        setFUsage(getCommand() + " [name] ([faction])");
        setDescription("Setzt den Namen der Fraktion");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Faction faction = args.length == 2 ? getFaction(getFPlayer(sender)) : getFaction(args[2]);
        assureSenderHasAdminPerms(sender, faction);
        String oldName = faction.getName();
        faction.setName(args[1]);
        BroadcastUtil.broadcast(FMessage.FACTION_INFO_NAME_CHANGED.message(oldName, args[1]));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 3) {
            return getTabFactions(sender, args[2]);
        }
        return null;
    }
}
