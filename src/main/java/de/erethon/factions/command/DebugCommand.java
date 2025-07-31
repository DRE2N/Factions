package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.building.TechTree;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.RegionStructure;
import de.erethon.factions.region.schematic.FAWESchematicUtils;
import de.erethon.factions.region.schematic.SchematicSavable;
import de.erethon.factions.war.entities.CrystalChargeCarrier;
import de.erethon.factions.war.entities.CrystalMob;
import de.erethon.factions.war.structure.WarStructure;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.inventory.MenuType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

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
        if (args[1].equalsIgnoreCase("test")) {
            Player player = (Player) sender;
            ClientboundOpenScreenPacket packet = new ClientboundOpenScreenPacket(0, MenuType.GENERIC_9x1, Component.empty());
            CraftPlayer craftPlayer = (CraftPlayer) player;
            craftPlayer.getHandle().connection.send(packet);
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
        if (args[1].equalsIgnoreCase("cigar")) {
            Player player = (Player) sender;
            final Vector3f LOCAL_CIGAR_OFFSET = new Vector3f(0.1f, -0.25f, 0.3f);
            final Vector3f CIGAR_SCALE = new Vector3f(0.2f, 0.2f, 0.7f);
            BlockDisplay cigar = player.getWorld().spawn(player.getEyeLocation(), BlockDisplay.class);
            cigar.setBlock(Material.OAK_LOG.createBlockData());
            cigar.setInterpolationDuration(1);
            cigar.setInterpolationDelay(-1);
            cigar.setTeleportDuration(1);
            Transformation transformation = new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    CIGAR_SCALE,
                    new AxisAngle4f(0, 0, 0, 1)
            );
            cigar.setTransformation(transformation);

            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    Location eyeLocation = player.getEyeLocation();
                    Vector3f forward = eyeLocation.getDirection().toVector3f();
                    Vector3f worldUp = new Vector3f(0.0f, 1.0f, 0.0f);

                    Vector3f right = new Vector3f();
                    worldUp.cross(forward, right).normalize();

                    if (Float.isNaN(right.x)) {
                        Vector3f worldForward = new Vector3f(0.0f, 0.0f, 1.0f);
                        worldForward.cross(forward, right).normalize();
                    }

                    Vector3f up = new Vector3f();
                    forward.cross(right, up).normalize();

                    Vector3f worldOffset = new Vector3f();
                    worldOffset.add(right.mul(LOCAL_CIGAR_OFFSET.x()));
                    worldOffset.add(up.mul(LOCAL_CIGAR_OFFSET.y()));
                    worldOffset.add(forward.mul(LOCAL_CIGAR_OFFSET.z()));

                    if (player.isSneaking()) {
                        worldOffset.add(0, -0.3f, 0);
                    }
                    Location cigarLocation = eyeLocation.clone().add(worldOffset.x, worldOffset.y, worldOffset.z);
                    cigar.teleport(cigarLocation);
                }
            };
            runnable.runTaskTimer(plugin, 0L, 0L);
        }
        if (args[1].equalsIgnoreCase("tech")) {
            new TechTree().show((Player) sender);
            MessageUtil.sendMessage(sender, "Tech tree?");
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
