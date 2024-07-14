package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class PortalCommand extends FCommand {

    public static final String LABEL = "portal";

    public PortalCommand() {
        setCommand(LABEL);
        setAliases("p");
        setMinMaxArgs(0, Integer.MAX_VALUE);
        setPermissionFromName();
        setFUsage(getCommand());
        setDescription("Befehle rund um Portale");
        setHelpType(HelpType.LISTED);
        setListedHelpHeader("Portalbefehle");
        addSubCommands(new PortalAddConditionCommand(), new PortalCreateCommand(), new PortalRemoveCondition(),
                new PortalSetLocationCommand(), new PortalTeleportCommand());
        setAllExecutionPrefixes();
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        displayHelp(sender);
    }

}
