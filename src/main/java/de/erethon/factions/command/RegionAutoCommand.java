package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class RegionAutoCommand extends FCommand {

    public static final String LABEL = "auto";
    public static final String PERM_PREFIX = RegionCommand.LABEL + "." + LABEL;

    public RegionAutoCommand() {
        setCommand(LABEL);
        setMinMaxArgs(0, Integer.MAX_VALUE);
        setPermissionFromName(RegionCommand.LABEL);
        setFUsage(RegionCommand.LABEL + " " + getCommand() + " [...]");
        setDescription("Befehle rund um automatische Regionsänderungen");
        setHelpType(HelpType.LISTED);
        setListedHelpHeader("Automatische Befehle");
        addSubCommands(new RegionAutoAddCommand(), new RegionAutoRadiusCommand(), new RegionAutoRemoveCommand(),
                new RegionAutoShapeCommand(), new RegionAutoTransferCommand());
        setAllExecutionPrefixes();
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        displayHelp(sender);
    }
}
