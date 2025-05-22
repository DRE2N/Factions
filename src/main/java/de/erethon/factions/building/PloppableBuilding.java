package de.erethon.factions.building;

import com.fastasyncworldedit.core.FaweAPI;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.util.FLogger;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;

public class PloppableBuilding extends Building {

    private String schematicID = "";
    private File schematic;
    private boolean shouldUndo = false;
    private int time = 0;

    public PloppableBuilding(File file, BuildingManager manager) {
        super(file, manager);
    }

    public void paste(Player player) {
        Clipboard clipboard = null;
        try {
            clipboard = ClipboardFormats.findByFile(schematic).load(schematic);
        } catch (Exception e) {
            FLogger.BUILDING.log("Error loading schematic " + schematic.getName());
            e.printStackTrace();
        }
        Block targetBlock = player.getTargetBlockExact(10);
        if (targetBlock == null) {
            MessageUtil.sendTranslatable(player, "factions.error.noTarget");
            return;
        }
        if (clipboard == null) {
            MessageUtil.sendTranslatable(player, "factions.error.generic");
            return;
        }
        Location bukkitLocation = targetBlock.getLocation();
        com.sk89q.worldedit.world.World world = FaweAPI.getWorld(bukkitLocation.getWorld().getName());
        try (EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, -1)) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(session)
                    .to(BlockVector3.at(bukkitLocation.getX(), bukkitLocation.getY(), bukkitLocation.getZ()))
                    .ignoreAirBlocks(true)
                    .build();
            Operations.complete(operation);
            if (shouldUndo) {
                BukkitRunnable undoRunnable = new BukkitRunnable() {
                    @Override
                    public void run() {
                        EditSession undo = WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, -1);
                        session.undo(undo);
                        session.flushQueue();
                        undo.flushQueue();
                    }
                };
                undoRunnable.runTaskLater(Factions.getInstance(), time);
            }
        }
    }

    @Override
    public void load() {
        super.load();
        schematicID = config.getString("schematicID");
        schematic = new File(Factions.getInstance().getDataFolder(), "schematics/" + schematicID + ".schem");
        shouldUndo = config.getBoolean("shouldUndo", false);
        time = config.getInt("undoTime", 0);
    }
}
