package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.event.FPlayerFactionLeaveEvent;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class LeaveCommand extends FCommand {

    public LeaveCommand() {
        setCommand("leave");
        setAliases("l");
        setMinMaxArgs(0, 1);
        setPermissionFromName();
        setFUsage(getCommand());
        setDescription("Verlässt die aktuelle Fraktion");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        Faction faction = fPlayer.getFaction();
        assurePlayerHasFaction(fPlayer);
        if (fPlayer.isAdmin()) {
            assure(args.length == 2 && args[1].equalsIgnoreCase("confirm"), FMessage.CMD_LEAVE_CONFIRMATION_REQUIRED, getUsage() + " confirm");
        }
        faction.playerLeave(fPlayer, FPlayerFactionLeaveEvent.Reason.COMMAND);
        sender.sendMessage(FMessage.CMD_LEAVE_SUCCESS.message());
    }
}
