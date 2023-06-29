package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class RegionStructureCommand extends FCommand {

    public static final String LABEL = "structure";
    public static final String CMD_PREFIX = RegionCommand.LABEL + " " + LABEL;

    public RegionStructureCommand() {
        setCommand("structure");
        setAliases("s");
        setMinMaxArgs(0, Integer.MAX_VALUE);
        setPermissionFromName(RegionCommand.LABEL);
        setFUsage(CMD_PREFIX + " [...]");
        setDescription("Befehle rund um Regionsstrukturen");
        setHelpType(HelpType.LISTED);
        setListedHelpHeader("Regionsstrukturenbefehle");
        addSubCommands(new RegionStructureCreateCommand(), new RegionStructureListCommand());
        setAllExecutionPrefixes();
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        displayHelp(sender);
    }
}
