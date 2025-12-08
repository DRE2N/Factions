package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import org.bukkit.command.CommandSender;

public class MarkerCommand extends FCommand {

    public static final String LABEL = "marker";

    public MarkerCommand() {
        setCommand(LABEL);
        setAliases("m");
        setMinMaxArgs(0, Integer.MAX_VALUE);
        setConsoleCommand(true);
        setPermissionFromName();
        setFUsage(getCommand() + " [...]");
        setDescription("Commands for managing map markers");
        setHelpType(HelpType.LISTED);
        setListedHelpHeader("Marker commands");
        addSubCommands(new MarkerCreateCommand(), new MarkerDeleteCommand(), new MarkerInfoCommand(),
                new MarkerListCommand(), new MarkerIconCommand(), new MarkerNameCommand(),
                new MarkerDescriptionCommand(), new MarkerPositionCommand());
        setAllExecutionPrefixes();
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        displayHelp(sender);
    }
}

