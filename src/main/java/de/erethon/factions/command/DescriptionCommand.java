package de.erethon.factions.command;

import de.erethon.aergia.util.BroadcastUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.faction.Faction;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

/**
 * @author Fyreum
 */
public class DescriptionCommand extends FCommand {

    public DescriptionCommand() {
        setCommand("description");
        setAliases("desc", "d");
        setConsoleCommand(true);
        setMinMaxArgs(1, Integer.MAX_VALUE);
        setPermissionFromName();
        setFUsage(getCommand() + "([faction]) [...]");
        setDescription("Setzt die Beschreibung der Fraktion");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Map.Entry<Faction, Boolean> result = getSenderFactionOrFromArgs(sender, args[1]);
        Faction faction = result.getKey();
        assureSenderHasAdminPerms(sender, faction);
        String description;
        if (result.getValue()) {
            assure(args.length > 2);
            description = getFinalArg(args, 2);
        } else {
            description = getFinalArg(args, 1);
        }
        faction.setDescription(description);
        BroadcastUtil.broadcast(FMessage.FACTION_INFO_DESCRIPTION_CHANGED.message(faction.getName(), description));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabFactions(sender, args[1]);
        }
        return null;
    }
}
