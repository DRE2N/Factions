package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.event.FactionDisbandEvent;
import de.erethon.factions.faction.Faction;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

/**
 * @author Fyreum
 */
public class DisbandCommand extends FCommand {

    public DisbandCommand() {
        setCommand("disband");
        setAliases("delete");
        setMinMaxArgs(0, 2);
        setConsoleCommand(true);
        setPermissionFromName();
        setFUsage(getCommand() + " ([faction])");
        setDescription("Löst die Fraktion auf");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Map.Entry<Faction, Boolean> result = getSenderFactionOrFromArgs(sender, args[1]);
        Faction faction = result.getKey();
        assureSenderHasAdminPerms(sender, faction);
        int index = result.getValue() ? 2 : 1;
        assure(args.length == index + 1 && args[index].equalsIgnoreCase("confirm"), FMessage.CMD_DISBAND_CONFIRMATION_REQUIRED, getUsage().replace("([faction])", index == 2 ? args[1] : "") + " confirm");
        faction.disband(FactionDisbandEvent.Reason.COMMAND);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabFactions(sender, args[1]);
        }
        return null;
    }
}
