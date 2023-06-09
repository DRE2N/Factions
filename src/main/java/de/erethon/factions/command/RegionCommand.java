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
        setPermissionFromName();
        setFUsage(getCommand() + " [...]");
        setDescription("Befehle rund um Regionen");
        setHelpType(HelpType.LISTED);
        setListedHelpHeader("Regionsbefehle");
        addSubCommands(new RegionAddChunkCommand(), new RegionAddNeighbourCommand(), new RegionAllianceCommand(),
                new RegionAutoAddChunkCommand(), new RegionAutoRadiusCommand(), new RegionAutoRemoveCommand(),
                new RegionCreateCommand(), new RegionDamageReductionCommand(), new RegionDeleteCommand(),
                new RegionDescriptionCommand(), new RegionInfoCommand(), new RegionRemoveChunkCommand(),
                new RegionRemoveNeighbourCommand(), new RegionTypeCommand());
        setAllExecutionPrefixes();
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        displayHelp(sender);
    }
}
