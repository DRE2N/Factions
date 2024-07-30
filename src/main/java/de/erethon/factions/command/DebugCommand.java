package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.war.entities.CrystalChargeCarrier;
import de.erethon.factions.war.entities.CrystalMob;
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
        setListedHelpHeader("Debugbefehle");
        addSubCommand(new DebugWarPhaseCommand());
        setAllExecutionPrefixes();
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        if (args.length == 1) {
            displayHelp(sender);
        }
        if (args[1].equalsIgnoreCase("spawnmob")) {
            Player player = (Player) sender;
            FPlayer fPlayer = getFPlayer(player);
            CrystalChargeCarrier carrier = new CrystalChargeCarrier(player.getWorld(), player.getLocation(), fPlayer.getCurrentRegion(), fPlayer.getAlliance());
            return;
        }
        if (args[1].equalsIgnoreCase("spawncrystal")) {
            Player player = (Player) sender;
            CrystalMob crystalMob = new CrystalMob(player.getWorld(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
            return;
        }
        if (args[1].equalsIgnoreCase("revolt")) {
            Player player = (Player) sender;
            Faction faction = getFPlayer(player).getFaction();
            if (faction == null) {
                MessageUtil.sendMessage(player, "no faction");
                return;
            }
            faction.getEconomy().spawnRevolt(faction, Integer.parseInt(args[2]));
            MessageUtil.sendMessage(player, "Revolution!");
            return;
        }
    }

}
