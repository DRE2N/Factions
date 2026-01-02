package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class RegionCommand extends FCommand {

    public static final String LABEL = "region";

    public RegionCommand() {
        setCommand(LABEL);
        setAliases("r");
        setMinMaxArgs(0, Integer.MAX_VALUE);
        setConsoleCommand(true);
        setPermissionFromName();
        setFUsage(getCommand() + " [...]");
        setDescription("Befehle rund um Regionen");
        setHelpType(HelpType.LISTED);
        setListedHelpHeader("Regionsbefehle");
        addSubCommands(new RegionAddCommand(), new RegionAddNeighbourCommand(), new RegionAllianceCommand(),
                new RegionAutoCommand(), new RegionCreateCommand(), new RegionBordersCommand(), new RegionBorderDebugCommand(), new RegionDamageReductionCommand(),
                new RegionDeleteCommand(), new RegionDescriptionCommand(), new RegionInfoCommand(),
                new RegionNameCommand(), new RegionRemoveCommand(), new RegionRemoveNeighbourCommand(),
                new RegionSchematicCommand(), new RegionSplitCommand(), new RegionStatusCommand(), new RegionStructureCommand(),
                new RegionModeCommand(), new RegionTypeCommand(), new RegionLevelCommand());
        setAllExecutionPrefixes();
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        displayHelp(sender);
    }
}
