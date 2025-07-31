package de.erethon.factions.region.schematic;

import com.fastasyncworldedit.core.Fawe;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EditSessionBuilder;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.misc.FileUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.region.RegionStructure;
import de.erethon.factions.war.structure.WarStructure;
import org.bukkit.Location;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Utility class for FastAsyncWorldEdit schematics.
 */
public class FAWESchematicUtils {

    private static final File schematicFolder = new File(Factions.getInstance().getDataFolder(), "schematics");
    private static final WorldEdit worldEdit = Fawe.instance().getWorldEdit();

    /**
     * Don't run this on the main thread.
     */
    public static void pasteSlice(String schematicID, Location origin, int slice) {
        File schematicFile = new File(schematicFolder, schematicID + ".schematic");
        if (!schematicFile.exists()) {
            Factions.getInstance().getLogger().warning("Schematic " + schematicID + " does not exist.");
            return;
        }
        Clipboard clipboard;
        try {
            Factions.log("Loading schematic " + schematicFile);
            clipboard = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.load(schematicFile);
        } catch (Exception e) {
            Factions.getInstance().getLogger().warning("Error loading schematic " + schematicID);
            return;
        }
        EditSessionBuilder builder = worldEdit.newEditSessionBuilder();
        BukkitWorld world = new BukkitWorld(origin.getWorld());
        builder.world(world);
        try (EditSession session = builder.build()) {
            Region fullRegion = clipboard.getRegion();
            int maxY = fullRegion.getMaximumY();
            int y = Math.min(slice, maxY);
            Factions.log("Full region: " + fullRegion.getMinimumPoint() + " to " + fullRegion.getMaximumPoint());
            int locationY = fullRegion.getMinimumY() + y;
            Factions.log("Location Y: " + locationY);
            // Create the slice region
            Region regionToPaste = new CuboidRegion(fullRegion.getMinimumPoint().withY(locationY), fullRegion.getMaximumPoint().withY(locationY));
            BlockVector3 to = fullRegion.getMinimumPoint().withY(locationY);
            Factions.log("Slice: " + regionToPaste.getMinimumPoint() + " to " + regionToPaste.getMaximumPoint());
            // ForwardExtentCopy limited to the slice region
            ForwardExtentCopy copy = new ForwardExtentCopy(clipboard, regionToPaste, regionToPaste.getMinimumPoint(), session, to);
            Operations.complete(copy);
        }
    }

    /**
     * Don't run this on the main thread either.
     */
    public static void saveWarStructureToSchematic(RegionStructure structure) {
        File schematicFile;
        if (structure instanceof SchematicSavable savable) {
            schematicFile = new File(schematicFolder, savable.getSchematicID() + ".schematic");
        } else {
            return;
        }
        FileUtil.createIfNotExisting(schematicFile);
        try {
            BlockVector3 pos1 = BlockVector3.at(structure.getMinPosition().blockX(), structure.getMinPosition().blockY(), structure.getMinPosition().blockZ());
            BlockVector3 pos2 = BlockVector3.at(structure.getMaxPosition().blockX(), structure.getMaxPosition().blockY(), structure.getMaxPosition().blockZ());
            Region region = new CuboidRegion(pos1, pos2);
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            EditSessionBuilder builder = worldEdit.newEditSessionBuilder();
            BukkitWorld world = new BukkitWorld(structure.getRegion().getWorld());
            builder.world(world);
            try (EditSession session = builder.build()) {
                ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(session, region, clipboard, region.getMinimumPoint());
                Factions.log("Operation: " + forwardExtentCopy);
                Operations.complete(forwardExtentCopy);
                try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.getWriter(new FileOutputStream(schematicFile))) {
                    Factions.log("Writing schematic to " + schematicFile);
                    writer.write(clipboard);
                }
            }
        } catch (IOException e) {
            Factions.getInstance().getLogger().warning("Error saving schematic " + savable.getSchematicID() + ": " + e.getMessage());
        }
    }
}
