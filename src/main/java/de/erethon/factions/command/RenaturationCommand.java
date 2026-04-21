package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.command.logic.FCommandCache;
import org.bukkit.command.CommandSender;

/**
 * Container command for renaturation operations.
 *
 * @author Malfrador
 */
public class RenaturationCommand extends FCommand {

    public static final String LABEL = "renaturation";

    public RenaturationCommand() {
        setCommand(LABEL);
        setAliases("renat", "decay");
        setMinMaxArgs(0, Integer.MAX_VALUE);
        setConsoleCommand(true);
        setPermission("factions.admin.renaturation");
        setFUsage("/" + FCommandCache.LABEL + " " + getCommand() + " [...]");
        setDescription("Renaturation (decay) management commands");
        setHelpType(HelpType.LISTED);
        setListedHelpHeader("Renaturation Commands");
        addSubCommands(
                new RenaturationStartCommand(),
                new RenaturationPauseCommand(),
                new RenaturationStatusCommand(),
                new RenaturationSpeedCommand()
        );
        setAllExecutionPrefixes();
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        displayHelp(sender);
    }
}

