package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class DeclineCommand extends FCommand {

    public DeclineCommand() {
        setCommand("decline");
        setMinMaxArgs(1, 1);
        setPermissionFromName();
        setFUsage(getCommand() + " [faction]");
        setDescription("Lehnte eine Einladung einer Fraktion ab");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayer(sender);
        Faction faction = getFaction(args[1]);
        assure(faction.isInvitedPlayer(fPlayer), FMessage.CMD_JOIN_NOT_INVITED);
        faction.removeInvitedPlayer(fPlayer);
        sender.sendMessage(FMessage.CMD_DECLINE_SUCCESS.message(faction.getName()));
    }
}
