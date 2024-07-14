package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.portal.Portal;
import de.erethon.factions.portal.PortalCondition;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * @author Fyreum
 */
public class PortalRemoveCondition extends FCommand {

    public PortalRemoveCondition() {
        setCommand("removecondition");
        setAliases("rc");
        setMinMaxArgs(2, 2);
        setPermissionFromName(PortalCommand.LABEL);
        setFUsage(getCommand() + " [portal] [condition]");
        setDescription("Entfernt eine Bedingung von einem Portal");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Portal portal = getPortal(args[1]);
        PortalCondition condition = PortalCondition.getByName(args[2]);
        assure(condition != null, FMessage.ERROR_PORTAL_CONDITION_NOT_FOUND);
        portal.removeCondition(condition);
        sender.sendMessage(FMessage.CMD_PORTAL_REMOVE_CONDITION_SUCCESS.message(args[2], args[1]));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabPortals(args[1]);
        }
        if (args.length == 3) {
            return getTabPortalConditions(args[2]);
        }
        return null;
    }
}
