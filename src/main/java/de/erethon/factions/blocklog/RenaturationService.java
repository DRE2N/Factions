package de.erethon.factions.blocklog;

import de.erethon.factions.Factions;
import de.erethon.factions.blocklog.model.BlockChange;
import de.erethon.factions.blocklog.model.RenaturationPhase;
import de.erethon.factions.blocklog.model.RenaturationProgress;
import de.erethon.factions.blocklog.util.BlockDataSerializer;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FLogger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles gradual renaturation (decay + restoration) of abandoned regions.
 * <p>
 * The Y-level sweep progresses on a periodic timer independent of chunk loads.
 * When a chunk is loaded, any blocks above the current sweep Y are restored immediately
 * (catching up to the current state). Block modifications always happen on the main thread
 * via tick-based scheduling.
 *
 * @author Malfrador
 */
public class RenaturationService implements Listener {

    /** How often the Y-level sweep ticks, in server ticks. */
    private static final int SWEEP_INTERVAL_TICKS = 20 * 30; // Every 30 seconds at 1x speed
    /** How many Y levels to drop per sweep tick at 1x speed. */
    private static final int BASE_Y_PER_SWEEP = 1;
    /** Maximum Y levels to drop in a single sweep tick, even at high speed. */
    private static final int MAX_Y_PER_SWEEP = 10;
    /** Max blocks to restore per server tick when catching up a chunk. */
    private static final int BLOCKS_PER_TICK = 20;
    /** Ticks between batches when restoring a chunk. */
    private static final int BATCH_TICK_INTERVAL = 2;

    private static final double VINE_CHANCE = 0.15;
    private static final double MOSS_CHANCE = 0.20;

    private final Factions plugin;
    private final BlockLogManager blockLogManager;
    private final Random random;

    /** Tracks chunks currently being restored to avoid duplicate processing. */
    private final Set<ChunkCoord> processingChunks = ConcurrentHashMap.newKeySet();

    /** Speed multipliers per region (for testing/debugging). */
    private final ConcurrentHashMap<Integer, Double> speedMultipliers = new ConcurrentHashMap<>();

    /** The current sweep task, if running. */
    private BukkitTask sweepTask;
    /** The current effective sweep interval. */
    private int currentSweepInterval = SWEEP_INTERVAL_TICKS;

    public RenaturationService(@NotNull Factions plugin,
                               @NotNull BlockLogManager blockLogManager,
                               @NotNull BaselineManager baselineManager) {
        this.plugin = plugin;
        this.blockLogManager = blockLogManager;
        this.random = new Random();
    }

    /**
     * Starts the periodic Y-level sweep timer.
     * Should be called once after initialization.
     */
    public void startSweepTask() {
        restartSweepTask(SWEEP_INTERVAL_TICKS);
        FLogger.INFO.log("Renaturation sweep task started (interval: " + SWEEP_INTERVAL_TICKS + " ticks)");
    }

    /**
     * Restarts the sweep task at the given interval.
     */
    private void restartSweepTask(int intervalTicks) {
        if (sweepTask != null) {
            sweepTask.cancel();
        }
        currentSweepInterval = intervalTicks;
        sweepTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::sweepTick,
                1, intervalTicks); // initial delay 1 tick so it runs soon
    }

    /**
     * Stops the sweep task.
     */
    public void stopSweepTask() {
        if (sweepTask != null) {
            sweepTask.cancel();
            sweepTask = null;
        }
    }

    // ---- Speed multiplier ----

    public void setSpeedMultiplier(int regionId, double multiplier) {
        if (multiplier == 1.0) {
            speedMultipliers.remove(regionId);
        } else {
            speedMultipliers.put(regionId, multiplier);
        }

        // Recalculate the sweep interval based on the fastest active multiplier.
        // Higher multiplier = shorter interval between sweep ticks.
        double maxMultiplier = speedMultipliers.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(1.0);
        int newInterval = Math.max(1, (int) (SWEEP_INTERVAL_TICKS / maxMultiplier));
        if (newInterval != currentSweepInterval) {
            restartSweepTask(newInterval);
            FLogger.INFO.log("Renaturation sweep interval adjusted to " + newInterval + " ticks (max multiplier: " + maxMultiplier + "x)");
        }
    }

    public double getSpeedMultiplier(int regionId) {
        return speedMultipliers.getOrDefault(regionId, 1.0);
    }

    // ---- Start / Pause ----

    public void startRenaturation(@NotNull Region region) {
        // Check if already actively running (not complete)
        if (blockLogManager.getRenaturationProgressDAO().hasActiveRenaturation(region.getId())) {
            FLogger.REGION.log("Region " + region.getId() + " already has active renaturation");
            return;
        }

        // Delete any existing (completed) entry before inserting a fresh one
        blockLogManager.getRenaturationProgressDAO().delete(region.getId());

        int maxY = region.getWorld().getMaxHeight() - 1;
        int minY = region.getWorld().getMinHeight();

        RenaturationProgress progress = new RenaturationProgress(
                region.getId(),
                (short) maxY,
                (short) maxY,
                (short) minY,
                RenaturationPhase.DECAY,
                null
        );

        blockLogManager.getRenaturationProgressDAO().create(progress);
        FLogger.REGION.log("Started renaturation for region " + region.getId()
                + " (Y " + maxY + " → " + minY + ")");
    }

    public void pauseRenaturation(@NotNull Region region) {
        blockLogManager.getRenaturationProgressDAO().delete(region.getId());
        FLogger.REGION.log("Paused renaturation for region " + region.getId());
    }

    // ---- Y-level sweep (runs async on timer) ----

    /**
     * Called periodically. For each active renaturation, lowers the Y level
     * based on the speed multiplier. Block changes in loaded chunks for the
     * newly-swept Y band are scheduled on the main thread.
     */
    private void sweepTick() {
        if (!blockLogManager.isReady()) return;

        List<RenaturationProgress> activeProcesses;
        try {
            activeProcesses = blockLogManager.getRenaturationProgressDAO().getAllActive();
        } catch (Exception e) {
            FLogger.ERROR.log("Failed to fetch active renaturation processes: " + e.getMessage());
            return;
        }

        for (RenaturationProgress progress : activeProcesses) {
            try {
                sweepRegion(progress);
            } catch (Exception e) {
                FLogger.ERROR.log("Failed to sweep region " + progress.getRegionId() + ": " + e.getMessage());
            }
        }
    }

    private void sweepRegion(@NotNull RenaturationProgress progress) {
        int regionId = progress.getRegionId();

        if (progress.getCurrentYLevel() <= progress.getMinYTarget()) {
            blockLogManager.getRenaturationProgressDAO().updatePhase(regionId, RenaturationPhase.COMPLETE);
            speedMultipliers.remove(regionId);
            FLogger.REGION.log("Renaturation complete for region " + regionId);
            return;
        }

        // Apply speed multiplier to Y drop: higher multiplier = more Y levels per sweep
        double multiplier = getSpeedMultiplier(regionId);
        int yDrop = Math.min((int) Math.ceil(BASE_Y_PER_SWEEP * multiplier), MAX_Y_PER_SWEEP);
        int oldY = progress.getCurrentYLevel();
        int newY = Math.max(progress.getMinYTarget(), oldY - yDrop);

        // Update DB first
        blockLogManager.getRenaturationProgressDAO().updateYLevel(regionId, (short) newY);

        // Fetch block changes in the Y band we just swept
        List<BlockChange> changes;
        try {
            changes = blockLogManager.getBlockLogDAO()
                    .getPlayerChangesInYBand(regionId, newY, oldY);
        } catch (Exception e) {
            FLogger.ERROR.log("Failed to query block changes for region " + regionId
                    + " Y band [" + newY + ", " + oldY + "]: " + e.getMessage());
            return;
        }

        if (changes.isEmpty()) {
            return;
        }

        FLogger.REGION.log("Sweeping region " + regionId + ": Y " + oldY + " → " + newY
                + " (" + changes.size() + " block changes, speed " + multiplier + "x)");

        // Deduplicate by position: keep only the FIRST (oldest) change per coordinate
        // so we restore to the original terrain state, not an intermediate player state.
        List<BlockChange> deduplicated = deduplicateByPosition(changes);

        Region region = plugin.getRegionManager().getRegionById(regionId);
        if (region == null) {
            FLogger.ERROR.log("Region " + regionId + " not found in RegionManager during sweep");
            return;
        }

        World world = region.getWorld();
        if (world == null) {
            FLogger.ERROR.log("World not found for region " + regionId + " during sweep");
            return;
        }

        // Sort top-down for correct restoration order
        deduplicated.sort(Comparator.comparingInt(BlockChange::y).reversed());

        // Filter to blocks in currently loaded chunks only — unloaded chunks
        // will catch up via onChunkLoad when they are eventually loaded.
        Bukkit.getScheduler().runTask(plugin, () -> {
            List<BlockChange> loadedChanges = deduplicated.stream()
                    .filter(c -> world.isChunkLoaded(c.x() >> 4, c.z() >> 4))
                    .toList();

            if (!loadedChanges.isEmpty()) {
                FLogger.REGION.log("Restoring " + loadedChanges.size() + " blocks in loaded chunks for region " + regionId);
                scheduleBatchedRestoration(loadedChanges, world);
            }
        });
    }

    /**
     * Deduplicates block changes by position, keeping only the first (oldest) entry per coordinate.
     * This ensures we restore to the original terrain state rather than an intermediate player state.
     */
    private List<BlockChange> deduplicateByPosition(@NotNull List<BlockChange> changes) {
        // Sort by timestamp ascending first to ensure the oldest change per position wins
        List<BlockChange> sorted = new ArrayList<>(changes);
        sorted.sort(Comparator.comparing(BlockChange::timestamp));

        Map<Long, BlockChange> byPosition = new LinkedHashMap<>();
        for (BlockChange change : sorted) {
            long posKey = packPosition(change.x(), change.y(), change.z());
            byPosition.putIfAbsent(posKey, change);
        }
        return new ArrayList<>(byPosition.values());
    }

    private static long packPosition(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) y & 0xFFFL) << 26 | ((long) z & 0x3FFFFFFL);
    }

    // ---- Chunk load catch-up ----

    /**
     * When a chunk loads in a region with active renaturation, restore all
     * blocks above the current Y level that haven't been processed yet.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!blockLogManager.isReady()) return;
        Chunk chunk = event.getChunk();

        Region region = plugin.getRegionManager().getRegionByChunk(chunk);
        if (region == null) return;

        if (!blockLogManager.getRenaturationProgressDAO().hasActiveRenaturation(region.getId())) {
            return;
        }

        ChunkCoord coord = new ChunkCoord(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        if (!processingChunks.add(coord)) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                RenaturationProgress progress = blockLogManager.getRenaturationProgressDAO()
                        .get(region.getId())
                        .orElse(null);
                if (progress == null) {
                    processingChunks.remove(coord);
                    return;
                }

                int currentY = progress.getCurrentYLevel();
                int chunkMinX = chunk.getX() << 4;
                int chunkMinZ = chunk.getZ() << 4;

                // Get all player changes (place + break) in this chunk ABOVE the current Y sweep line
                List<BlockChange> changes = blockLogManager.getBlockLogDAO()
                        .getPlayerChangesInChunkAboveY(region.getId(), chunk.getWorld().getName(),
                                chunkMinX, chunkMinX + 15, chunkMinZ, chunkMinZ + 15, currentY);

                if (changes.isEmpty()) {
                    processingChunks.remove(coord);
                    return;
                }

                // Deduplicate by position to get the original terrain state
                List<BlockChange> deduplicated = deduplicateByPosition(changes);

                FLogger.REGION.log("Chunk load catch-up for (" + chunk.getX() + ", " + chunk.getZ()
                        + ") in region " + region.getId() + ": " + deduplicated.size() + " blocks above Y " + currentY);

                deduplicated.sort(Comparator.comparingInt(BlockChange::y).reversed());

                Bukkit.getScheduler().runTask(plugin, () -> {
                    scheduleBatchedRestoration(deduplicated, chunk.getWorld());
                    // Remove from processing set after all batches are scheduled
                    int batches = (deduplicated.size() + BLOCKS_PER_TICK - 1) / BLOCKS_PER_TICK;
                    long totalTicks = (long) batches * BATCH_TICK_INTERVAL + 1;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> processingChunks.remove(coord), totalTicks);
                });
            } catch (Exception e) {
                FLogger.ERROR.log("Failed chunk catch-up for " + coord + ": " + e.getMessage());
                processingChunks.remove(coord);
            }
        });
    }

    // ---- Batched block restoration ----

    /**
     * Schedules block restorations in batches on the main thread.
     * Must be called from or scheduled onto the main thread.
     */
    private void scheduleBatchedRestoration(@NotNull List<BlockChange> changes,
                                            @NotNull World world) {
        List<List<BlockChange>> batches = new ArrayList<>();
        for (int i = 0; i < changes.size(); i += BLOCKS_PER_TICK) {
            batches.add(changes.subList(i, Math.min(i + BLOCKS_PER_TICK, changes.size())));
        }

        for (int i = 0; i < batches.size(); i++) {
            List<BlockChange> batch = batches.get(i);
            long delay = (long) i * BATCH_TICK_INTERVAL;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (BlockChange change : batch) {
                    restoreBlock(change, world);
                }
            }, delay);
        }
    }

    /**
     * Restores a single block: applies decay effects, then reverts to original state.
     */
    private void restoreBlock(@NotNull BlockChange change, @NotNull World world) {
        Location loc = new Location(world, change.x(), change.y(), change.z());
        Block block = loc.getBlock();

        String materialBefore = block.getType().name();
        String paletteMaterial = blockLogManager.getPalette().getMaterial(change.oldBlockId());

        // If block is still player-placed, apply decay cosmetics then revert
        if (!block.getType().isAir()) {
            // Small chance of decay cosmetics before removal
            if (random.nextDouble() < MOSS_CHANCE) {
                Material mossyVariant = getMossyVariant(block.getType());
                if (mossyVariant != null) {
                    block.setType(mossyVariant, false);
                }
            }
            if (block.getType() == Material.STONE_BRICKS && random.nextDouble() < 0.3) {
                block.setType(Material.CRACKED_STONE_BRICKS, false);
            }
        }

        // Restore to original block state
        byte[] oldData = blockLogManager.getPalette().getData(change.oldBlockId());
        if (oldData != null && oldData.length > 0) {
            BlockDataSerializer.applyBlockData(loc, oldData);
            FLogger.REGION.log("Restored block at " + change.x() + "," + change.y() + "," + change.z()
                    + " from " + materialBefore + " to " + paletteMaterial + " (palette ID " + change.oldBlockId() + ")");
        } else {
            block.setType(Material.AIR, false);
            FLogger.REGION.log("Restored block at " + change.x() + "," + change.y() + "," + change.z()
                    + " from " + materialBefore + " to AIR (palette ID " + change.oldBlockId() + ")");
        }

        // Add vines to adjacent blocks for flavor (they'll disappear as we sweep lower)
        if (random.nextDouble() < VINE_CHANCE) {
            addVinesToAdjacentBlocks(block);
        }
    }

    // ---- Cosmetic helpers ----

    private void addVinesToAdjacentBlocks(@NotNull Block block) {
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (BlockFace face : faces) {
            Block adjacent = block.getRelative(face);
            if (adjacent.getType().isAir() && random.nextDouble() < 0.5) {
                Block support = adjacent.getRelative(face.getOppositeFace());
                if (!support.getType().isSolid()) continue;

                adjacent.setType(Material.VINE, false);
                if (adjacent.getBlockData() instanceof org.bukkit.block.data.MultipleFacing vineData) {
                    // Vine faces the block it's attached to
                    vineData.setFace(face.getOppositeFace(), true);
                    adjacent.setBlockData(vineData, false);
                }
            }
        }
    }

    private boolean canHaveVines(@NotNull Block block) {
        Material type = block.getType();
        return type.isSolid() && !type.isAir() && type != Material.GLASS && type != Material.GLASS_PANE;
    }

    private Material getMossyVariant(@NotNull Material material) {
        return switch (material) {
            case COBBLESTONE -> Material.MOSSY_COBBLESTONE;
            case STONE_BRICKS -> Material.MOSSY_STONE_BRICKS;
            case COBBLESTONE_WALL -> Material.MOSSY_COBBLESTONE_WALL;
            case STONE_BRICK_WALL -> Material.MOSSY_STONE_BRICK_WALL;
            case COBBLESTONE_STAIRS -> Material.MOSSY_COBBLESTONE_STAIRS;
            case STONE_BRICK_STAIRS -> Material.MOSSY_STONE_BRICK_STAIRS;
            case COBBLESTONE_SLAB -> Material.MOSSY_COBBLESTONE_SLAB;
            case STONE_BRICK_SLAB -> Material.MOSSY_STONE_BRICK_SLAB;
            default -> null;
        };
    }

    private record ChunkCoord(String world, int x, int z) {}
}

