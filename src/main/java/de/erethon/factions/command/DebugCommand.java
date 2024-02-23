package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.war.objective.CrystalChargeCarrier;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Fyreum
 */
public class DebugCommand extends FCommand {

    public DebugCommand() {
        setCommand("debug");
        setConsoleCommand(true);
        setMinMaxArgs(0, Integer.MAX_VALUE);
        setPermissionFromName();
        setFUsage(getCommand());
        setDescription("Debug command");
        setHelpType(HelpType.LISTED);
        setListedHelpHeader("Allianzbefehle");
        addSubCommand(new DebugWarPhaseCommand());
        setAllExecutionPrefixes();
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            displayHelp(sender);
        }
        if (args[1].equalsIgnoreCase("spawnmob")) {
            Player player = (Player) sender;
            FPlayer fPlayer = getFPlayer(player);
            CrystalChargeCarrier carrier = new CrystalChargeCarrier(player.getWorld(), player.getLocation(), fPlayer.getCurrentRegion(), fPlayer.getAlliance());
        }
    }
}
