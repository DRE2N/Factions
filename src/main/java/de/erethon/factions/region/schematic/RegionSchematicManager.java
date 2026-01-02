package de.erethon.factions.region.schematic;

import com.fastasyncworldedit.core.Fawe;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import de.erethon.factions.Factions;
import de.erethon.factions.region.LazyChunk;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FLogger;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Manager for saving and loading region schematics.
 * Schematics are saved per region with state IDs for different states.
 * <p>
 * Folder structure: plugins/Factions/schematics/regions/{regionId}/{stateId}.schem
 * <p>
 * The "rollback" state ID is reserved for restoring regions to their natural state.
 * <p>
 *
 * @author Malfrador
 */
public class RegionSchematicManager {

    public static final String ROLLBACK_STATE = "rollback";
    private static final String SCHEMATIC_EXTENSION = ".schem";

    private final File regionsSchematicFolder;
    private final WorldEdit worldEdit;

    public RegionSchematicManager(@NotNull Factions plugin) {
        this.regionsSchematicFolder = new File(Factions.SCHEMATICS, "regions");
        if (!regionsSchematicFolder.exists()) {
            regionsSchematicFolder.mkdirs();
        }
        this.worldEdit = Fawe.instance().getWorldEdit();
    }

    /**
     * Gets the folder for a specific region's schematics.
     *
     * @param region The region
     * @return The folder for the region's schematics
     */
    public @NotNull File getRegionFolder(@NotNull Region region) {
        return new File(regionsSchematicFolder, String.valueOf(region.getId()));
    }

    /**
     * Gets the schematic file for a specific region state.
     *
     * @param region  The region
     * @param stateId The state ID
     * @return The schematic file
     */
    public @NotNull File getSchematicFile(@NotNull Region region, @NotNull String stateId) {
        return new File(getRegionFolder(region), stateId + SCHEMATIC_EXTENSION);
    }

    /**
     * Checks if a schematic state exists for a region.
     *
     * @param region  The region
     * @param stateId The state ID
     * @return true if the schematic exists
     */
    public boolean hasState(@NotNull Region region, @NotNull String stateId) {
        return getSchematicFile(region, stateId).exists();
    }

    /**
     * Gets all available state IDs for a region.
     *
     * @param region The region
     * @return A set of available state IDs
     */
    public @NotNull Set<String> getAvailableStates(@NotNull Region region) {
        File folder = getRegionFolder(region);
        if (!folder.exists() || !folder.isDirectory()) {
            return Collections.emptySet();
        }
        File[] files = folder.listFiles((dir, name) -> name.endsWith(SCHEMATIC_EXTENSION));
        if (files == null) {
            return Collections.emptySet();
        }
        return Arrays.stream(files)
                .map(f -> f.getName().replace(SCHEMATIC_EXTENSION, ""))
                .collect(Collectors.toSet());
    }

    /**
     * Saves the current state of a region to a schematic asynchronously.
     * Uses FAWE's async task system for optimal performance.
     *
     * @param region  The region to save
     * @param stateId The state ID to save as
     * @return A CompletableFuture that completes with true if successful
     */
    public CompletableFuture<Boolean> saveRegionState(@NotNull Region region, @NotNull String stateId) {
        World world = region.getWorld();
        BukkitWorld bukkitWorld = new BukkitWorld(world);
        return CompletableFuture.supplyAsync(() -> {
            Set<LazyChunk> chunks = region.getChunks();
            if (chunks.isEmpty()) {
                FLogger.ERROR.log("Cannot save region " + region.getId() + ": no chunks defined");
                return false;
            }


            // Calculate bounding box from chunks
            int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

            for (LazyChunk chunk : chunks) {
                int chunkMinX = chunk.getX() << 4;
                int chunkMinZ = chunk.getZ() << 4;
                int chunkMaxX = chunkMinX + 15;
                int chunkMaxZ = chunkMinZ + 15;

                minX = Math.min(minX, chunkMinX);
                minZ = Math.min(minZ, chunkMinZ);
                maxX = Math.max(maxX, chunkMaxX);
                maxZ = Math.max(maxZ, chunkMaxZ);
            }

            int minY = world.getMinHeight();
            int maxY = world.getMaxHeight() - 1;

            File schematicFile = getSchematicFile(region, stateId);
            File folder = schematicFile.getParentFile();
            if (!folder.exists() && !folder.mkdirs()) {
                FLogger.ERROR.log("Failed to create schematic folder for region " + region.getId());
                return false;
            }

            BlockVector3 pos1 = BlockVector3.at(minX, minY, minZ);
            BlockVector3 pos2 = BlockVector3.at(maxX, maxY, maxZ);
            CuboidRegion cuboidRegion = new CuboidRegion(pos1, pos2);

            // Create clipboard with origin at the region's minimum point
            // This ensures the schematic pastes back at the correct location
            BlockArrayClipboard clipboard = new BlockArrayClipboard(cuboidRegion);
            clipboard.setOrigin(pos1);

            try (EditSession session = worldEdit.newEditSessionBuilder()
                    .world(bukkitWorld)
                    .fastMode(true)
                    .combineStages(true)
                    .checkMemory(false)
                    .changeSetNull()
                    .limitUnlimited()
                    .build()) {

                ForwardExtentCopy copy = new ForwardExtentCopy(session, cuboidRegion, clipboard, pos1);
                copy.setCopyingEntities(false);
                copy.setCopyingBiomes(true);
                Operations.complete(copy);
            }

            // Write to file after EditSession is closed
            try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.getWriter(new FileOutputStream(schematicFile))) {
                FLogger.REGION.log("Saving region " + region.getId() + " state '" + stateId + "' to " + schematicFile);
                writer.write(clipboard);
                return true;
            } catch (IOException e) {
                FLogger.ERROR.log("Error saving region " + region.getId() + " state '" + stateId + "': " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Loads a schematic state and pastes it into the world asynchronously.
     * Uses FAWE's async task system for optimal performance.
     *
     * @param region  The region to restore
     * @param stateId The state ID to load
     * @return A CompletableFuture that completes with true if successful
     */
    public CompletableFuture<Boolean> loadRegionState(@NotNull Region region, @NotNull String stateId) {
        World world = region.getWorld();
        BukkitWorld bukkitWorld = new BukkitWorld(world);
        return CompletableFuture.supplyAsync(() -> {
            File schematicFile = getSchematicFile(region, stateId);
            if (!schematicFile.exists()) {
                FLogger.ERROR.log("Schematic for region " + region.getId() + " state '" + stateId + "' does not exist");
                return false;
            }

            try {
                Clipboard clipboard = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.load(schematicFile);
                FLogger.REGION.log("Loading region " + region.getId() + " state '" + stateId + "' from " + schematicFile);

                try (EditSession session = worldEdit.newEditSessionBuilder()
                        .world(bukkitWorld)
                        .fastMode(true)
                        .combineStages(true)
                        .checkMemory(false)
                        .changeSetNull()
                        .limitUnlimited()
                        .build()) {

                    // Disable lighting updates for faster pasting
                    session.setSideEffectApplier(com.sk89q.worldedit.util.SideEffectSet.none());

                    // Paste at the clipboard's origin (where it was saved from)
                    BlockVector3 origin = clipboard.getOrigin();

                    // Use ClipboardHolder for proper pasting at original location
                    ClipboardHolder holder = new ClipboardHolder(clipboard);

                    // Create operation to paste the clipboard at its original location
                    com.sk89q.worldedit.function.operation.Operation operation = holder
                            .createPaste(session)
                            .to(origin)
                            .ignoreAirBlocks(false)
                            .copyEntities(false)
                            .copyBiomes(true)
                            .build();

                    Operations.complete(operation);
                }
                return true;
            } catch (IOException e) {
                FLogger.ERROR.log("Error loading region " + region.getId() + " state '" + stateId + "': " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Deletes a schematic state for a region.
     *
     * @param region  The region
     * @param stateId The state ID to delete
     * @return true if the deletion was successful
     */
    public boolean deleteState(@NotNull Region region, @NotNull String stateId) {
        File schematicFile = getSchematicFile(region, stateId);
        if (!schematicFile.exists()) {
            return false;
        }
        return schematicFile.delete();
    }

    /**
     * Checks if a region has a rollback state saved.
     *
     * @param region The region
     * @return true if the region has a rollback state
     */
    public boolean hasRollbackState(@NotNull Region region) {
        return hasState(region, ROLLBACK_STATE);
    }

    /**
     * Saves the current state as the rollback state asynchronously.
     *
     * @param region The region
     * @return A CompletableFuture that completes with true if successful
     */
    public CompletableFuture<Boolean> saveRollbackState(@NotNull Region region) {
        return saveRegionState(region, ROLLBACK_STATE);
    }

    /**
     * Restores the region to its rollback state asynchronously.
     *
     * @param region The region
     * @return A CompletableFuture that completes with true if successful
     */
    public CompletableFuture<Boolean> rollback(@NotNull Region region) {
        return loadRegionState(region, ROLLBACK_STATE);
    }
}

