package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.command.logic.FCommandCache;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandSender;

/**
 * Container command for baseline operations.
 *
 * @author Malfrador
 */
public class BaselineCommand extends FCommand {

    public static final String LABEL = "baseline";

    public BaselineCommand() {
        setCommand(LABEL);
        setAliases("base", "natural");
        setMinMaxArgs(0, Integer.MAX_VALUE);
        setConsoleCommand(true);
        setPermission("factions.admin.baseline");
        setFUsage("/" + getCommand() + " [...]");
        setDescription("Baseline (natural state) management commands");
        setHelpType(HelpType.LISTED);
        setListedHelpHeader("Baseline Commands");
        addSubCommands(
                new BaselineCaptureCommand(),
                new BaselineRestoreCommand(),
                new BaselineInfoCommand(),
                new BaselineUpdateCommand(),
                new BaselineSessionCommand()
        );
        setAllExecutionPrefixes();
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        displayHelp(sender);
    }
}

