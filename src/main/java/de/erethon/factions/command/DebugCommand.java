package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.RegionStructure;
import de.erethon.factions.region.schematic.FAWESchematicUtils;
import de.erethon.factions.region.schematic.SchematicSavable;
import de.erethon.factions.war.entities.CrystalChargeCarrier;
import de.erethon.factions.war.entities.CrystalMob;
import de.erethon.factions.war.structure.WarStructure;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

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
        if (args[1].equalsIgnoreCase("setfactionalliance")) {
            Faction faction = getFPlayer(sender).getFaction();
            Alliance alliance = getAlliance(args[2]);
            if (faction == null || alliance == null) {
                MessageUtil.sendMessage(sender, "Faction or alliance not found");
                return;
            }
            faction.setAlliance(alliance);
            MessageUtil.sendMessage(sender, "Set alliance to " + alliance.getName() + " for " + faction.getName());
            return;
        }
        if (args[1].equalsIgnoreCase("saveRegionSchematic")) {
            RegionStructure struct = plugin.getRegionManager().getRegionByPlayer((Player) sender).getStructureAt(((Player) sender).getLocation());
            if (struct == null) {
                MessageUtil.sendMessage(sender, "No structure found");
                return;
            }
            if (struct instanceof SchematicSavable savable) {
                FAWESchematicUtils.saveWarStructureToSchematic(struct);
                MessageUtil.sendMessage(sender, "Saved schematic");
            } else {
                MessageUtil.sendMessage(sender, "Not a savable structure" + struct.getClass().getSimpleName());
            }
        }
        if (args[1].equalsIgnoreCase("pasteSlice")) {
            RegionStructure struct = plugin.getRegionManager().getRegionByPlayer((Player) sender).getStructureAt(((Player) sender).getLocation());
            if (struct == null) {
                MessageUtil.sendMessage(sender, "No structure found");
                return;
            }
            if (struct instanceof SchematicSavable savable) {
                FAWESchematicUtils.pasteSlice(savable.getSchematicID(), savable.getOrigin(), Integer.parseInt(args[2]));
                MessageUtil.sendMessage(sender, "Pasted slice " + args[2]);
            } else {
                MessageUtil.sendMessage(sender, "Not a savable structure: " + struct.getClass().getSimpleName());
            }
        }
        if (args[1].equalsIgnoreCase("pasteRegion")) {
            RegionStructure struct = plugin.getRegionManager().getRegionByPlayer((Player) sender).getStructureAt(((Player) sender).getLocation());
            if (struct == null) {
                MessageUtil.sendMessage(sender, "No structure found");
                return;
            }
            if (struct instanceof SchematicSavable savable) {
                BukkitRunnable runnable = new BukkitRunnable() {
                    private int y = 0;
                    @Override
                    public void run() {
                        FAWESchematicUtils.pasteSlice(savable.getSchematicID(), savable.getOrigin(), y);
                        y++;
                    }
                };
                runnable.runTaskTimer(plugin, 0, Integer.parseInt(args[2]));
            } else {
                MessageUtil.sendMessage(sender, "Not a savable structure: " + struct.getClass().getSimpleName());
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return List.of("spawnmob", "spawncrystal", "revolt", "setfactionalliance", "saveRegionSchematic", "pasteSlice", "pasteRegion");
        }
        return super.onTabComplete(sender, args);
    }
}
