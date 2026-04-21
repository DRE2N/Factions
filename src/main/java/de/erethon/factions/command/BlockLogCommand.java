package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.command.logic.FCommandCache;
import org.bukkit.command.CommandSender;

/**
 * Container command for block logging operations.
 *
 * @author Malfrador
 */
public class BlockLogCommand extends FCommand {

    public static final String LABEL = "blocklog";

    public BlockLogCommand() {
        setCommand(LABEL);
        setAliases("blog", "bl");
        setMinMaxArgs(0, Integer.MAX_VALUE);
        setConsoleCommand(true);
        setPermission("factions.admin.blocklog");
        setFUsage("/" + getCommand() + " [...]");
        setDescription("Block logging and rollback commands");
        setHelpType(HelpType.LISTED);
        setListedHelpHeader("Block Log Commands");
        addSubCommands(
                new BlockLogQueryCommand(),
                new BlockLogRollbackCommand(),
                new BlockLogStatsCommand()
        );
        setAllExecutionPrefixes();
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        displayHelp(sender);
    }
}

