package de.erethon.factions.blocklog;

import com.fastasyncworldedit.core.Fawe;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.function.mask.AbstractMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import de.erethon.factions.Factions;
import de.erethon.factions.blocklog.mapupdate.MapUpdateSession;
import de.erethon.factions.blocklog.model.BlockChange;
import de.erethon.factions.blocklog.util.BlockDataSerializer;
import de.erethon.factions.region.LazyChunk;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.schematic.RegionSchematicManager;
import de.erethon.factions.util.FLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Malfrador
 */
public class BaselineManager {

    public static final String BASELINE_PREFIX = "baseline_v";
    public static final String STAGING_WORLD_NAME = "ErethonStaging";
    private static final int REPLAY_BLOCKS_PER_TICK = 100;
    private static final int REPLAY_BATCH_INTERVAL_TICKS = 1;

    private final Factions plugin;
    private final RegionSchematicManager schematicManager;
    private final BlockLogManager blockLogManager;

    public BaselineManager(@NotNull Factions plugin,
                           @NotNull RegionSchematicManager schematicManager,
                           @NotNull BlockLogManager blockLogManager) {
        this.plugin = plugin;
        this.schematicManager = schematicManager;
        this.blockLogManager = blockLogManager;
    }
    public CompletableFuture<Integer> captureBaseline(@NotNull Region region, @Nullable String notes) {
        return blockLogManager.getInitializationFuture().thenCompose(v -> {
            int currentVersion = blockLogManager.getRegionBaselineDAO()
                    .getCurrentVersion(region.getId()).orElse(0);
            int newVersion = currentVersion + 1;
            String stateId = BASELINE_PREFIX + newVersion;
            FLogger.REGION.log("Capturing baseline v" + newVersion + " for region " + region.getId());
            return schematicManager.saveRegionState(region, stateId).thenApply(success -> {
                if (!success) {
                    FLogger.ERROR.log("Failed to save baseline schematic for region " + region.getId());
                    return -1;
                }
                persistVersionUpdate(region, currentVersion, newVersion,
                        getBaselineSchematicPath(region, newVersion), notes);
                FLogger.REGION.log("Successfully captured baseline v" + newVersion + " for region " + region.getId());
                return newVersion;
            });
        });
    }

    public CompletableFuture<Integer> captureBaselineFromStagingWorld(@NotNull Region region,
                                                                       @Nullable String notes) {
        return blockLogManager.getInitializationFuture().thenCompose(v -> {
            // World loading must happen on the main thread
            CompletableFuture<World> worldFuture = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(plugin, () -> {
                World world = getOrLoadStagingWorld();
                if (world != null) {
                    worldFuture.complete(world);
                } else {
                    worldFuture.completeExceptionally(
                            new IllegalStateException("Could not load staging world '" + STAGING_WORLD_NAME + "'"));
                }
            });

            return worldFuture.thenCompose(stagingWorld -> {
                blockLogManager.getRegionBaselineDAO().ensureExists(region.getId());
                int currentVersion = blockLogManager.getRegionBaselineDAO()
                        .getCurrentVersion(region.getId()).orElse(0);
                int latestHistory = blockLogManager.getRegionBaselineDAO()
                        .getLatestHistoryVersion(region.getId());
                int newVersion = Math.max(currentVersion, latestHistory) + 1;
                String stateId = BASELINE_PREFIX + newVersion;
                String notesFinal = notes != null ? notes : "Map update from staging world";

                FLogger.REGION.log("Capturing baseline v" + newVersion + " for region " + region.getId()
                        + " from staging world '" + stagingWorld.getName() + "'");

                return schematicManager.saveRegionStateFromWorld(region, stateId, stagingWorld)
                        .thenApply(success -> {
                            if (!success) {
                                FLogger.ERROR.log("Failed to save staging baseline schematic for region " + region.getId());
                                return -1;
                            }
                            blockLogManager.getRegionBaselineDAO().addToHistory(region.getId(), newVersion,
                                    getBaselineSchematicPath(region, newVersion), notesFinal + " [staged]");
                            FLogger.REGION.log("Successfully captured staging baseline v" + newVersion
                                    + " for region " + region.getId());
                            return newVersion;
                        });
            });
        });
    }

    @Nullable
    public World getOrLoadStagingWorld() {
        World existing = Bukkit.getWorld(STAGING_WORLD_NAME);
        if (existing != null) {
            return existing;
        }
        FLogger.REGION.log("Loading staging world '" + STAGING_WORLD_NAME + "'...");
        World world = Bukkit.createWorld(new WorldCreator(STAGING_WORLD_NAME));
        if (world == null) {
            FLogger.ERROR.log("Failed to load staging world '" + STAGING_WORLD_NAME
                    + "' — ensure the world folder exists on disk.");
        }
        return world;
    }

    public CompletableFuture<Void> applyMapUpdate(@NotNull MapUpdateSession session,
                                                   @NotNull CommandSender reporter) {
        Region region = session.getRegion();
        World liveWorld = region.getWorld();

        return blockLogManager.getInitializationFuture().thenComposeAsync(v -> {
            sendMsg(reporter, "<yellow>[MapUpdate] Starting map update for region " + region.getName() + "...</yellow>");

            String stateId = BASELINE_PREFIX + session.getNewBaselineVersion();
            File schematicFile = schematicManager.getSchematicFile(region, stateId);
            if (!schematicFile.exists()) {
                sendMsg(reporter, "<red>[MapUpdate] ERROR: Schematic for baseline v"
                        + session.getNewBaselineVersion() + " does not exist. Run '/f baseline update' first.</red>");
                return CompletableFuture.completedFuture(null);
            }

            sendMsg(reporter, "<gray>[MapUpdate] Pasting baseline schematic via FAWE (zone mask active)...</gray>");
            return CompletableFuture.supplyAsync(() -> pasteBaselineWithMask(schematicFile, liveWorld, session))
                    .thenComposeAsync(pastedBlocks -> {
                        if (pastedBlocks >= 0) {
                            sendMsg(reporter, "<gray>[MapUpdate] Baseline paste complete (" + pastedBlocks + " changed blocks).</gray>");
                        } else {
                            sendMsg(reporter, "<gray>[MapUpdate] Baseline paste complete (changed block count unavailable).</gray>");
                        }
                        sendMsg(reporter, "<gray>[MapUpdate] Baseline paste complete. Replaying player changes chunk-by-chunk...</gray>");

                        Set<LazyChunk> chunks = region.getChunks();
                        List<LazyChunk> chunkList = new ArrayList<>(chunks);
                        AtomicInteger processedChunks = new AtomicInteger(0);
                        AtomicLong replayedChanges = new AtomicLong(0);
                        int totalChunks = chunkList.size();

                        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
                        for (LazyChunk chunk : chunkList) {
                            chain = chain.thenComposeAsync(
                                    ignored2 -> processChunkReplay(chunk, region, liveWorld, session, reporter,
                                            processedChunks, totalChunks, replayedChanges));
                        }

                        return chain.thenRunAsync(() -> {
                            try {
                                int newVersion = session.getNewBaselineVersion();
                                String schematicPath = getBaselineSchematicPath(region, newVersion);
                                blockLogManager.getRegionBaselineDAO().ensureExists(region.getId());
                                blockLogManager.getRegionBaselineDAO()
                                        .updateVersion(region.getId(), newVersion, schematicPath);
                                blockLogManager.getRegionBaselineDAO()
                                        .addToHistory(region.getId(), newVersion, schematicPath,
                                                "Applied via map update session");
                            } catch (Exception e) {
                                FLogger.ERROR.log("Failed to update DB version after map update for region "
                                        + region.getId() + ": " + e.getMessage());
                            }
                            sendMsg(reporter, "<green>[MapUpdate] Map update for region " + region.getName() + " complete!"
                                    + " Replayed " + replayedChanges.get() + " player changes.</green>");
                        });
                    });
        });
    }

    private CompletableFuture<Void> processChunkReplay(@NotNull LazyChunk chunk,
                                                         @NotNull Region region,
                                                         @NotNull World liveWorld,
                                                         @NotNull MapUpdateSession session,
                                                         @NotNull CommandSender reporter,
                                                         @NotNull AtomicInteger processedChunks,
                                                         int totalChunks,
                                                         @NotNull AtomicLong replayedChanges) {
        int chunkMinX = chunk.getX() << 4;
        int chunkMinZ = chunk.getZ() << 4;
        int chunkMaxX = chunkMinX + 15;
        int chunkMaxZ = chunkMinZ + 15;
        if (session.hasIncludeOnlyZones() && !chunkOverlapsAnyZone(chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ,
                session.getIncludeOnlyZones().values())) {
            processedChunks.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            List<BlockChange> latest;
            try {
                latest = blockLogManager.getBlockLogDAO()
                        .getLatestPlayerChangesInBounds(region.getId(),
                                chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
            } catch (Exception e) {
                FLogger.ERROR.log("Failed to query block changes for chunk ("
                        + chunk.getX() + "," + chunk.getZ() + ") in region " + region.getId()
                        + ": " + e.getMessage());
                return List.<BlockChange>of();
            }
            return latest;
        }).thenCompose(changes -> {
            if (changes.isEmpty()) {
                processedChunks.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            }

            List<BlockChange> toApply = new ArrayList<>(changes.size());
            for (BlockChange change : changes) {
                if (!session.isIncluded(change.x(), change.y(), change.z())) {
                    continue;
                }
                toApply.add(change);
            }

            int done = processedChunks.incrementAndGet();
            if (done % 50 == 0 || done == totalChunks) {
                sendMsg(reporter, "<gray>[MapUpdate] Replayed " + done + "/" + totalChunks + " chunks...</gray>");
            }

            if (toApply.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }

            replayedChanges.addAndGet(toApply.size());

            CompletableFuture<Void> batchDone = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(plugin, () ->
                    scheduleBatchChain(toApply, 0, liveWorld, session, batchDone));
            return batchDone;
        });
    }
    private void scheduleBatchChain(@NotNull List<BlockChange> changes, int offset,
                                     @NotNull World world,
                                     @NotNull MapUpdateSession session,
                                     @NotNull CompletableFuture<Void> done) {
        int end = Math.min(offset + REPLAY_BLOCKS_PER_TICK, changes.size());
        for (int i = offset; i < end; i++) {
            applyPlayerChange(changes.get(i), world, session);
        }
        if (end >= changes.size()) {
            done.complete(null);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> scheduleBatchChain(changes, end, world, session, done),
                    REPLAY_BATCH_INTERVAL_TICKS);
        }
    }

    private void applyPlayerChange(@NotNull BlockChange change, @NotNull World world,
                                    @NotNull MapUpdateSession session) {
        int yShift = session.getYShiftAt(change.x(), change.y(), change.z());
        int targetY = change.y() + yShift;
        Location loc = new Location(world, change.x(), targetY, change.z());

        switch (change.changeType()) {
            case PLAYER_PLACE -> {
                byte[] data = blockLogManager.getPalette().getData(change.newBlockId());
                if (data != null && data.length > 0) {
                    BlockDataSerializer.applyBlockData(loc, data);
                } else {
                    FLogger.ERROR.log("Missing palette data for block ID " + change.newBlockId()
                            + " during map update replay at "
                            + change.x() + "," + change.y() + "," + change.z()
                            + (yShift != 0 ? " (Y-shifted to " + targetY + ")" : ""));
                }
            }
            case PLAYER_BREAK -> loc.getBlock().setType(org.bukkit.Material.AIR, false);
            default -> { /* other change types are not player-driven, skip */ }
        }
    }

    private long pasteBaselineWithMask(@NotNull File schematicFile,
                                       @NotNull World liveWorld,
                                       @NotNull MapUpdateSession session) {
        try (AutoCloseable ignored = blockLogManager.suppressExternalEditLogging()) {
            Clipboard clipboard = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.load(schematicFile);
            BukkitWorld weWorld = new BukkitWorld(liveWorld);

            try (EditSession editSession =
                         Fawe.instance().getWorldEdit()
                                 .newEditSessionBuilder()
                                 .world(weWorld)
                                 .fastMode(true)
                                 .combineStages(true)
                                 .checkMemory(false)
                                 .changeSetNull()
                                 .limitUnlimited()
                                 .build()) {

                Mask zoneMask =
                        new AbstractMask() {
                            @Override
                            public boolean test(BlockVector3 vec) {
                                int x = vec.x();
                                int y = vec.y();
                                int z = vec.z();
                                if (!session.isIncluded(x, y, z)) return false;
                                return !session.isProtected(x, y, z);
                            }

                            @Override
                            public Mask copy() {
                                return this;
                            }
                        };
                editSession.setMask(zoneMask);

                BlockVector3 origin = clipboard.getOrigin();
                ClipboardHolder holder =
                        new ClipboardHolder(clipboard);
                Operation op = holder
                        .createPaste(editSession)
                        .to(origin)
                        .ignoreAirBlocks(false)
                        .copyEntities(false)
                        .copyBiomes(true)
                        .build();
                Operations.complete(op);
                return editSession.getBlockChangeCount();
            }
        } catch (IOException e) {
            FLogger.ERROR.log("Failed to paste baseline schematic during map update: " + e.getMessage());
        } catch (Exception e) {
            FLogger.ERROR.log("Unexpected error while pasting baseline schematic: " + e.getMessage());
        }
        return -1;
    }

    public CompletableFuture<Boolean> restoreBaseline(@NotNull Region region, int version) {
        FLogger.REGION.log("Restoring region " + region.getId() + " to baseline v" + version);
        return schematicManager.loadRegionState(region, BASELINE_PREFIX + version);
    }

    public CompletableFuture<Boolean> restoreCurrentBaseline(@NotNull Region region) {
        return blockLogManager.getInitializationFuture().thenCompose(v -> {
            int version = blockLogManager.getRegionBaselineDAO()
                    .getCurrentVersion(region.getId()).orElse(0);
            if (version <= 0) {
                return CompletableFuture.completedFuture(false);
            }
            return restoreBaseline(region, version);
        });
    }

    public boolean hasBaseline(@NotNull Region region) {
        return blockLogManager.getRegionBaselineDAO().hasBaseline(region.getId());
    }

    public int getCurrentVersion(@NotNull Region region) {
        return blockLogManager.getRegionBaselineDAO()
                .getCurrentVersion(region.getId()).orElse(0);
    }

    public int getLatestCapturedVersion(@NotNull Region region) {
        return blockLogManager.getRegionBaselineDAO().getLatestHistoryVersion(region.getId());
    }

    private String getBaselineSchematicPath(@NotNull Region region, int version) {
        return schematicManager.getSchematicFile(region, BASELINE_PREFIX + version).getAbsolutePath();
    }

    private void persistVersionUpdate(@NotNull Region region, int currentVersion,
                                       int newVersion, @NotNull String schematicPath,
                                       @Nullable String notes) {
        blockLogManager.getRegionBaselineDAO().ensureExists(region.getId());
        if (currentVersion > 0) {
            blockLogManager.getRegionBaselineDAO()
                    .addToHistory(region.getId(), currentVersion,
                            getBaselineSchematicPath(region, currentVersion),
                            "Replaced by v" + newVersion);
        }
        boolean hasRow = blockLogManager.getRegionBaselineDAO().get(region.getId()).isPresent();
        if (!hasRow) {
            blockLogManager.getRegionBaselineDAO().create(region.getId(), newVersion, schematicPath);
        } else {
            blockLogManager.getRegionBaselineDAO()
                    .updateVersion(region.getId(), newVersion, schematicPath);
        }
        blockLogManager.getRegionBaselineDAO()
                .addToHistory(region.getId(), newVersion, schematicPath, notes);
    }

    private boolean chunkOverlapsAnyZone(int cMinX, int cMinZ, int cMaxX, int cMaxZ,
                                          @NotNull Iterable<MapUpdateSession.ZoneEntry> zones) {
        for (MapUpdateSession.ZoneEntry entry : zones) {
            BlockVector3 zMin = entry.region().getMinimumPoint();
            BlockVector3 zMax = entry.region().getMaximumPoint();
            if (cMaxX >= zMin.x() && cMinX <= zMax.x()
                    && cMaxZ >= zMin.z() && cMinZ <= zMax.z()) {
                return true;
            }
        }
        return false;
    }

    private void sendMsg(@NotNull CommandSender reporter, @NotNull String miniMessage) {
        reporter.sendMessage(MiniMessage.miniMessage()
                .deserialize(miniMessage));
    }
}
