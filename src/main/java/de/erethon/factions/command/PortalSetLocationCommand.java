package de.erethon.factions.command;

import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * @author Fyreum
 */
public class PortalSetLocationCommand extends FCommand {

    public PortalSetLocationCommand() {
        setCommand("setlocation");
        setAliases("set", "s");
        setMinMaxArgs(2, 2);
        setPermissionFromName(PortalCommand.LABEL);
        setFUsage(getCommand() + " [portal] [alliance]");
        setDescription("Setzt die Teleport-Position einer Allianz");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Alliance alliance = getAlliance(args[2]);
        Location location = ((Player) sender).getLocation();
        getPortal(args[1]).setLocation(alliance, location);
        sender.sendMessage(FMessage.CMD_PORTAL_SET_LOCATION_SUCCESS.message(args[1], alliance.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabPortals(args[1]);
        }
        return null;
    }

}
