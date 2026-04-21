package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import org.bukkit.command.CommandSender;

/**
 * Container command for map update session management.
 * All subcommands live under {@code /f baseline session ...}.
 *
 * @author Malfrador
 */
public class BaselineSessionCommand extends FCommand {

    public BaselineSessionCommand() {
        setCommand("session");
        setAliases("s");
        setMinMaxArgs(0, Integer.MAX_VALUE);
        setConsoleCommand(true);
        setPermission("factions.admin.baseline.session");
        setFUsage(BaselineCommand.LABEL + " session [...]");
        setDescription("Map update session management commands");
        setHelpType(HelpType.LISTED);
        setListedHelpHeader("Map Update Session Commands");
        addSubCommands(
                new BaselineSessionStartCommand(),
                new BaselineSessionProtectCommand(),
                new BaselineSessionIncludeCommand(),
                new BaselineSessionStatusCommand(),
                new BaselineSessionApplyCommand(),
                new BaselineSessionCancelCommand()
        );
        setAllExecutionPrefixes();
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        displayHelp(sender);
    }
}

