package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import org.bukkit.command.CommandSender;

/**
 * Container command for region schematic operations.
 *
 * @author Malfrador
 */
public class RegionSchematicCommand extends FCommand {

    public static final String LABEL = "schematic";

    public RegionSchematicCommand() {
        setCommand(LABEL);
        setAliases("schem", "s");
        setMinMaxArgs(0, Integer.MAX_VALUE);
        setConsoleCommand(true);
        setPermissionFromName(RegionCommand.LABEL);
        setFUsage(RegionCommand.LABEL + " " + getCommand() + " [...]");
        setDescription("Befehle für Region-Schematics");
        setHelpType(HelpType.LISTED);
        setListedHelpHeader("Region Schematic Befehle");
        addSubCommands(
                new RegionSchematicSaveCommand(),
                new RegionSchematicLoadCommand(),
                new RegionSchematicListCommand(),
                new RegionSchematicDeleteCommand()
        );
        setAllExecutionPrefixes();
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        displayHelp(sender);
    }
}

