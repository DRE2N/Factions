package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.event.FPlayerFactionLeaveEvent;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * @author Fyreum
 */
public class KickCommand extends FCommand {

    public KickCommand() {
        setCommand("kick");
        setAliases("k");
        setMinMaxArgs(1, 1);
        setConsoleCommand(true);
        setPermissionFromName();
        setFUsage(getCommand() + " [player]");
        setDescription("Kickt den Spieler aus der Fraktion");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerInFaction(args[1]);
        Faction faction = fPlayer.getFaction();
        assure(!(sender instanceof Player) || getFPlayerRaw(sender) != fPlayer, FMessage.ERROR_CANNOT_KICK_YOURSELF);
        assure(faction.hasPrivilegeOver(sender, fPlayer), FMessage.ERROR_NO_PERMISSION);
        faction.playerLeave(fPlayer, FPlayerFactionLeaveEvent.Reason.KICKED);
        sender.sendMessage(FMessage.CMD_KICK_SUCCESS.message(fPlayer.getLastName(), faction.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabPlayers(args[1]);
        }
        return null;
    }
}
