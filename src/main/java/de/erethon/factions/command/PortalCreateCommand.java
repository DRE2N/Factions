package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.portal.Portal;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class PortalCreateCommand extends FCommand {

    public PortalCreateCommand() {
        setCommand("create");
        setAliases("c");
        setPermissionFromName(PortalCommand.LABEL);
        setFUsage(getCommand());
        setDescription("Erstellt ein neues Portal");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Portal portal = plugin.getPortalManager().createPortal();
        sender.sendMessage(FMessage.CMD_PORTAL_CREATE_SUCCESS.message(String.valueOf(portal.getId())));
    }

}
