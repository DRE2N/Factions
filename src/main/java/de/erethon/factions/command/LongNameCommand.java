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
public class LongNameCommand extends FCommand {

    public LongNameCommand() {
        setCommand("longname");
        setAliases("ln");
        setMinMaxArgs(1, 2);
        setConsoleCommand(true);
        setPermissionFromName();
        setFUsage(getCommand() + " [name] ([faction])");
        setDescription("Setzt den ausführlichen Namen der Fraktion");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Faction faction = args.length == 2 ? getFaction(getFPlayer(sender)) : getFaction(args[2]);
        assureSenderHasAdminPerms(sender, faction);
        int maximumChars = plugin.getFConfig().getMaximumLongNameChars();
        assure(args[1].length() <= maximumChars, FMessage.ERROR_TEXT_IS_TOO_LONG, String.valueOf(maximumChars));
        faction.setLongName(args[1]);
        BroadcastUtil.broadcast(FMessage.FACTION_INFO_LONG_NAME_CHANGED.message(faction.getName(), args[1]));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 3) {
            return getTabFactions(sender, args[2]);
        }
        return null;
    }
}
