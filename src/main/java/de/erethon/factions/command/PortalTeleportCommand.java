package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.player.FPlayer;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * @author Fyreum
 */
public class PortalTeleportCommand extends FCommand {

    public PortalTeleportCommand() {
        setCommand("teleport");
        setAliases("tp");
        setMinMaxArgs(1, 1);
        setPermissionFromName(PortalCommand.LABEL);
        setFUsage(getCommand() + " [portal]");
        setDescription("Teleportiert dich zu einem Portal");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        getPortal(args[1]).teleport(fPlayer);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return getTabPortals(args[0]);
        }
        return null;
    }
}
