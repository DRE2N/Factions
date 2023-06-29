package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class AllianceCommand extends FCommand {

    public static final String LABEL = "alliance";

    public AllianceCommand() {
        setCommand(LABEL);
        setAliases("a");
        setMinMaxArgs(0, Integer.MAX_VALUE);
        setConsoleCommand(true);
        setPermissionFromName();
        setFUsage(getCommand() + " [...]");
        setDescription("Befehle rund um Allianzen");
        setHelpType(HelpType.LISTED);
        setListedHelpHeader("Allianzbefehle");
        addSubCommands(new AllianceChooseCommand(), new AllianceShowCommand());
        setAllExecutionPrefixes();
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        displayHelp(sender);
    }
}
