package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.faction.Faction;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class AuthoriseCommand extends FCommand {

    public AuthoriseCommand() {
        setCommand("authorise");
        setMinMaxArgs(1, 2);
        setConsoleCommand(true);
        setPermissionFromName();
        setFUsage(getCommand()  + " [faction] ([faction])");
        setDescription("Autorisiert eine andere Fraktion auf dem Fraktionsgebiet bauen zu dürfen");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Faction faction = args.length == 3 ? getFaction(getFPlayer(sender)) : getFaction(args[2]);
        Faction other = getFaction(args[1]);
        assure(faction != other, FMessage.ERROR_CANNOT_AUTHORISE_SELF);
        assureSenderHasAdminPerms(sender, other);

        if (faction.toggleAuthorisedBuilder(other)) {
            sender.sendMessage(FMessage.CMD_AUTHORISE_ADDED.message(faction.getDisplayShortName(), other.getDisplayShortName()));
        } else {
            sender.sendMessage(FMessage.CMD_AUTHORISE_REMOVED.message(faction.getDisplayShortName(), other.getDisplayShortName()));
        }
    }
}
