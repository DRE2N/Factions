package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class ObjectiveCommand extends FCommand {

    public static final String LABLE = "objective";

    public ObjectiveCommand() {
        setCommand(LABLE);
        setAliases("o");
        setMinMaxArgs(0, Integer.MAX_VALUE);
        setPermissionFromName();
        setFUsage(getCommand() + " [...]");
        setDescription("Befehle rund um Kriegsziele");
        setHelpType(HelpType.LISTED);
        setListedHelpHeader("Kriegszielbefehle");
        addSubCommand(new ObjectiveCreateCommand());
        setAllExecutionPrefixes();
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        displayHelp(sender);
    }

}
