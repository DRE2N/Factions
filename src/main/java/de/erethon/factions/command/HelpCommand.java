package de.erethon.factions.command;

import de.erethon.bedrock.misc.InfoUtil;
import de.erethon.factions.command.logic.FCommand;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class HelpCommand extends FCommand {

    public HelpCommand() {
        setCommand("help");
        setAliases("h", "?", "main");
        setMinMaxArgs(0, Integer.MAX_VALUE);
        setConsoleCommand(true);
        setFUsage(getCommand());
        setDescription("Listet alle dir zugänglichen Befehle auf");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        InfoUtil.sendListedHelp(sender, plugin.getCommandCache());
    }
}
