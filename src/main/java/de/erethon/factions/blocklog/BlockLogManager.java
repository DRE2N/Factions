package de.erethon.factions.blocklog;

import de.erethon.bedrock.database.BedrockDBConnection;
import de.erethon.bedrock.database.EDatabaseManager;
import de.erethon.factions.Factions;
import de.erethon.factions.blocklog.dao.BlockLogDAO;
import de.erethon.factions.blocklog.dao.RegionBaselineDAO;
import de.erethon.factions.blocklog.dao.RenaturationProgressDAO;
import de.erethon.factions.blocklog.model.BlockChange;
import de.erethon.factions.blocklog.model.ChangeType;
import de.erethon.factions.util.FLogger;
import org.jdbi.v3.core.Jdbi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Main manager for the block logging system.
 * Handles async logging, queuing, and batch operations.
 *
 * @author Malfrador
 */
public class BlockLogManager extends EDatabaseManager {

    private static final int BATCH_SIZE = 10000;
    private static final int BATCH_INTERVAL_MS = 5000;
    private static final int MAX_QUEUE_SIZE = 100000;

    private final Factions plugin;
    private final ConcurrentLinkedQueue<BlockChange> blockQueue;
    private final AtomicLong queueSize;
    private final AtomicBoolean shuttingDown;
    private final AtomicInteger externalEditLoggingSuppressionDepth;
    private final File emergencyBufferFile;

    private BlockLogDAO blockLogDAO;
    private RegionBaselineDAO regionBaselineDAO;
    private RenaturationProgressDAO renaturationProgressDAO;
    private BlockPalette palette;

    private volatile boolean ready = false;

    private Thread batchProcessorThread;

    public BlockLogManager(Factions plugin, BedrockDBConnection connection) {
        super(connection, new ThreadPoolExecutor(2, 4, 60L, java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<>()));
        this.plugin = plugin;
        this.blockQueue = new ConcurrentLinkedQueue<>();
        this.queueSize = new AtomicLong(0);
        this.shuttingDown = new AtomicBoolean(false);
        this.externalEditLoggingSuppressionDepth = new AtomicInteger(0);
        this.emergencyBufferFile = new File(plugin.getDataFolder(), "blocklog_emergency_buffer.dat");
    }

    @Override
    protected CompletableFuture<Void> initializeSchema() {
        return CompletableFuture.runAsync(() -> {
            jdbi.useHandle(handle -> {
                FLogger.REGION.log("Initializing block log database schema...");

                // Initialize palette first (block_log references it)
                palette = new BlockPalette(jdbi);
                palette.initialize();

                // Create block_log table with partitioning
                handle.execute("""
                    CREATE TABLE IF NOT EXISTS block_log (
                        id BIGSERIAL,
                        region_id INTEGER NOT NULL,
                        x INTEGER NOT NULL,
                        y SMALLINT NOT NULL,
                        z INTEGER NOT NULL,
                        world_name VARCHAR(64) NOT NULL,
                        
                        old_block_id INTEGER NOT NULL DEFAULT 0,
                        new_block_id INTEGER NOT NULL DEFAULT 0,
                        
                        change_type VARCHAR(32) NOT NULL,
                        cause_uuid UUID,
                        cause_name VARCHAR(64),
                        reverted BOOLEAN NOT NULL DEFAULT FALSE,
                        reverted_at TIMESTAMP,
                        reverted_by_uuid UUID,
                        reverted_by_name VARCHAR(64),
                        
                        timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
                        baseline_version INTEGER DEFAULT 1,
                        
                        PRIMARY KEY (id, timestamp)
                    ) PARTITION BY RANGE (timestamp)
                    """);

                // Migration: add rollback tracking columns for older deployments.
                handle.execute("ALTER TABLE block_log ADD COLUMN IF NOT EXISTS reverted BOOLEAN NOT NULL DEFAULT FALSE");
                handle.execute("ALTER TABLE block_log ADD COLUMN IF NOT EXISTS reverted_at TIMESTAMP");
                handle.execute("ALTER TABLE block_log ADD COLUMN IF NOT EXISTS reverted_by_uuid UUID");
                handle.execute("ALTER TABLE block_log ADD COLUMN IF NOT EXISTS reverted_by_name VARCHAR(64)");

                try {
                    boolean hasOldColumns = handle.createQuery("""
                        SELECT EXISTS (
                            SELECT 1 FROM information_schema.columns
                            WHERE table_name = 'block_log' AND column_name = 'old_block_data'
                        )
                        """).mapTo(Boolean.class).one();
                    if (hasOldColumns) {
                        FLogger.REGION.log("Migrating block_log table from BYTEA to palette IDs...");
                        handle.execute("ALTER TABLE block_log DROP COLUMN IF EXISTS old_block_data");
                        handle.execute("ALTER TABLE block_log DROP COLUMN IF EXISTS new_block_data");
                        handle.execute("ALTER TABLE block_log ADD COLUMN IF NOT EXISTS old_block_id INTEGER NOT NULL DEFAULT 0");
                        handle.execute("ALTER TABLE block_log ADD COLUMN IF NOT EXISTS new_block_id INTEGER NOT NULL DEFAULT 0");
                        FLogger.REGION.log("block_log migration complete");
                    }
                } catch (Exception e) {
                    FLogger.REGION.log("block_log migration check skipped: " + e.getMessage());
                }

                // Create initial partition for current month
                String currentMonth = getCurrentMonthPartitionName();
                String nextMonth = getNextMonthPartitionName();

                try {
                    handle.execute(String.format("""
                        CREATE TABLE IF NOT EXISTS %s PARTITION OF block_log
                            FOR VALUES FROM ('%s-01 00:00:00') TO ('%s-01 00:00:00')
                        """, currentMonth, getCurrentYearMonth(), getNextYearMonth()));
                } catch (Exception e) {
                    // Partition might already exist
                    FLogger.REGION.log("Partition " + currentMonth + " already exists or creation failed: " + e.getMessage());
                }

                // Create indexes
                handle.execute(String.format("""
                    CREATE INDEX IF NOT EXISTS idx_%s_region_time 
                        ON %s (region_id, timestamp DESC)
                    """, currentMonth, currentMonth));

                handle.execute(String.format("""
                    CREATE INDEX IF NOT EXISTS idx_%s_region_active_time
                        ON %s (region_id, timestamp DESC)
                        WHERE reverted = FALSE
                    """, currentMonth, currentMonth));

                handle.execute(String.format("""
                    CREATE INDEX IF NOT EXISTS idx_%s_region_y 
                        ON %s (region_id, y DESC, timestamp DESC)
                    """, currentMonth, currentMonth));

                handle.execute(String.format("""
                    CREATE INDEX IF NOT EXISTS idx_%s_cause 
                        ON %s (cause_uuid, timestamp DESC)
                    """, currentMonth, currentMonth));

                handle.execute(String.format("""
                    CREATE INDEX IF NOT EXISTS idx_%s_position 
                        ON %s (world_name, x, y, z, timestamp DESC)
                    """, currentMonth, currentMonth));

                // Index for chunk-based queries (renaturation)
                handle.execute(String.format("""
                    CREATE INDEX IF NOT EXISTS idx_%s_chunk_renat 
                        ON %s (region_id, world_name, x, z, change_type, y DESC)
                        WHERE change_type = 'PLAYER_PLACE'
                    """, currentMonth, currentMonth));

                // Region baseline table
                handle.execute("""
                    CREATE TABLE IF NOT EXISTS region_baseline (
                        region_id INTEGER PRIMARY KEY,
                        current_version INTEGER NOT NULL DEFAULT 1,
                        current_schematic_path VARCHAR(512),
                        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                    )
                    """);

                // Region baseline history table
                handle.execute("""
                    CREATE TABLE IF NOT EXISTS region_baseline_history (
                        region_id INTEGER NOT NULL,
                        version INTEGER NOT NULL,
                        schematic_path VARCHAR(512) NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        notes TEXT,
                        PRIMARY KEY (region_id, version)
                    )
                    """);

                // Renaturation progress table
                handle.execute("""
                    CREATE TABLE IF NOT EXISTS renaturation_progress (
                        region_id INTEGER PRIMARY KEY,
                        started_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        current_y_level SMALLINT NOT NULL,
                        max_y_processed SMALLINT NOT NULL,
                        min_y_target SMALLINT NOT NULL,
                        phase VARCHAR(32) NOT NULL,
                        decay_rules_json JSONB,
                        last_processed TIMESTAMP NOT NULL DEFAULT NOW(),
                        estimated_completion TIMESTAMP
                    )
                    """);

                // Migration: rename old column if it exists
                try {
                    handle.execute("ALTER TABLE renaturation_progress RENAME COLUMN decay_rules TO decay_rules_json");
                } catch (Exception ignored) {
                    // Column already renamed or doesn't exist
                }

                handle.execute("""
                    CREATE INDEX IF NOT EXISTS idx_renaturation_active 
                        ON renaturation_progress (phase, last_processed)
                        WHERE phase != 'COMPLETE'
                    """);

                FLogger.REGION.log("Block log database schema initialized successfully");
            });
        }, asyncExecutor);
    }

    @Override
    protected void registerCustomMappers() {
        // Row mapper is registered via @RegisterRowMapper on BlockLogDAO
    }

    @Override
    protected void configureJdbiPlugins(Jdbi jdbiInstance) {
        super.configureJdbiPlugins(jdbiInstance);
    }

    /**
     * Initializes the manager and starts the batch processor.
     */
    public void initialize() {
        getInitializationFuture().whenComplete((result, throwable) -> {
            if (throwable != null) {
                FLogger.ERROR.log("BlockLogManager schema initialization failed: " + throwable.getMessage());
                throwable.printStackTrace();
                return;
            }
            this.blockLogDAO = getDao(BlockLogDAO.class);
            this.regionBaselineDAO = getDao(RegionBaselineDAO.class);
            this.renaturationProgressDAO = getDao(RenaturationProgressDAO.class);

            startBatchProcessor();
            ready = true;
            FLogger.REGION.log("BlockLogManager initialized and batch processor started");
        });
    }

    /**
     * Returns true if the manager has finished async initialization and is ready to accept events.
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * Queues a block change for logging.
     *
     * @param change The block change to log
     */
    public void logBlockChange(BlockChange change) {
        if (shuttingDown.get()) {
            FLogger.ERROR.log("Cannot log block change - system is shutting down");
            return;
        }

        long currentSize = queueSize.incrementAndGet();
        blockQueue.offer(change);

        if (currentSize > MAX_QUEUE_SIZE) {
            FLogger.ERROR.log("Block log queue exceeded maximum size (" + MAX_QUEUE_SIZE + "), writing to emergency buffer");
            writeToEmergencyBuffer();
        }
    }

    /**
     * Suppresses external edit logging (e.g. WorldEdit hooks) for the current operation scope.
     * Use with try-with-resources and keep the scope as narrow as possible.
     */
    public AutoCloseable suppressExternalEditLogging() {
        externalEditLoggingSuppressionDepth.incrementAndGet();
        return () -> externalEditLoggingSuppressionDepth.updateAndGet(v -> Math.max(0, v - 1));
    }

    public boolean isExternalEditLoggingSuppressed() {
        return externalEditLoggingSuppressionDepth.get() > 0;
    }

    /**
     * Logs a block placement.
     */
    public void logPlace(int regionId, int x, int y, int z, String worldName,
                         byte[] oldBlockData, byte[] newBlockData,
                         UUID causeUuid, String causeName, int baselineVersion) {
        int oldId = palette.getOrCreate(oldBlockData);
        int newId = palette.getOrCreate(newBlockData);
        BlockChange change = new BlockChange(
                0, regionId, x, y, z, worldName,
                oldId, newId,
                ChangeType.PLAYER_PLACE, causeUuid, causeName,
                Timestamp.from(Instant.now()), baselineVersion
        );
        logBlockChange(change);
    }

    /**
     * Logs a block break.
     */
    public void logBreak(int regionId, int x, int y, int z, String worldName,
                        byte[] oldBlockData, byte[] newBlockData,
                        UUID causeUuid, String causeName, int baselineVersion) {
        int oldId = palette.getOrCreate(oldBlockData);
        int newId = palette.getOrCreate(newBlockData);
        BlockChange change = new BlockChange(
                0, regionId, x, y, z, worldName,
                oldId, newId,
                ChangeType.PLAYER_BREAK, causeUuid, causeName,
                Timestamp.from(Instant.now()), baselineVersion
        );
        logBlockChange(change);
    }

    /**
     * Starts the batch processor thread.
     */
    private void startBatchProcessor() {
        batchProcessorThread = new Thread(() -> {
            FLogger.REGION.log("Batch processor thread started");
            long lastBatch = System.currentTimeMillis();

            while (!shuttingDown.get() || !blockQueue.isEmpty()) {
                try {
                    long now = System.currentTimeMillis();
                    long timeSinceLastBatch = now - lastBatch;
                    long queueCount = queueSize.get();

                    // Process batch if we hit size threshold or time threshold
                    if (queueCount >= BATCH_SIZE || (queueCount > 0 && timeSinceLastBatch >= BATCH_INTERVAL_MS)) {
                        processBatch();
                        lastBatch = now;
                    }

                    // Sleep for a bit to avoid spinning
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    FLogger.ERROR.log("Batch processor interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    FLogger.ERROR.log("Error in batch processor: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            FLogger.REGION.log("Batch processor thread stopped");
        }, "BlockLog-BatchProcessor");

        batchProcessorThread.setDaemon(false); // Important: don't make this daemon so it can finish on shutdown
        batchProcessorThread.start();
    }

    /**
     * Processes a batch of block changes from the queue.
     */
    private void processBatch() {
        List<BlockChange> batch = new ArrayList<>(BATCH_SIZE);
        int count = 0;

        // Drain up to BATCH_SIZE items from queue
        while (count < BATCH_SIZE && !blockQueue.isEmpty()) {
            BlockChange change = blockQueue.poll();
            if (change != null) {
                batch.add(change);
                queueSize.decrementAndGet();
                count++;
            }
        }

        if (batch.isEmpty()) {
            return;
        }

        // Insert batch
        try {
            jdbi.useHandle(handle -> {
                BlockLogDAO dao = handle.attach(BlockLogDAO.class);
                dao.batchInsert(batch);
            });
            FLogger.REGION.log("Processed batch of " + batch.size() + " block changes");
        } catch (Exception e) {
            FLogger.ERROR.log("Failed to insert batch of " + batch.size() + " block changes: " + e.getMessage());
            e.printStackTrace();
            // Re-queue the failed changes
            blockQueue.addAll(batch);
            queueSize.addAndGet(batch.size());
        }
    }

    /**
     * Writes the current queue to an emergency buffer file on disk.
     */
    private void writeToEmergencyBuffer() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(emergencyBufferFile, true))) {
            List<BlockChange> overflow = new ArrayList<>();
            // Drain excess items
            while (queueSize.get() > MAX_QUEUE_SIZE / 2) {
                BlockChange change = blockQueue.poll();
                if (change != null) {
                    overflow.add(change);
                    queueSize.decrementAndGet();
                }
            }
            oos.writeObject(overflow);
            FLogger.REGION.log("Wrote " + overflow.size() + " block changes to emergency buffer");
        } catch (IOException e) {
            FLogger.ERROR.log("Failed to write emergency buffer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Shuts down the block log manager gracefully.
     */
    public void shutdown() {
        FLogger.REGION.log("Shutting down BlockLogManager...");
        shuttingDown.set(true);

        // Wait for batch processor to finish
        if (batchProcessorThread != null && batchProcessorThread.isAlive()) {
            try {
                batchProcessorThread.join(30000); // Wait up to 30 seconds
                if (batchProcessorThread.isAlive()) {
                    FLogger.ERROR.log("Batch processor thread did not finish in time, forcing shutdown");
                }
            } catch (InterruptedException e) {
                FLogger.ERROR.log("Interrupted while waiting for batch processor: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        // Process any remaining items in queue
        if (!blockQueue.isEmpty()) {
            FLogger.REGION.log("Processing remaining " + queueSize.get() + " block changes before shutdown");
            processBatch();
        }

        close();
        FLogger.REGION.log("BlockLogManager shut down successfully");
    }

    // Helper methods for partition management
    private String getCurrentMonthPartitionName() {
        return "block_log_" + getCurrentYearMonth().replace("-", "_");
    }

    private String getNextMonthPartitionName() {
        return "block_log_" + getNextYearMonth().replace("-", "_");
    }

    private String getCurrentYearMonth() {
        Instant now = Instant.now();
        return String.format("%04d-%02d",
                now.atZone(java.time.ZoneId.systemDefault()).getYear(),
                now.atZone(java.time.ZoneId.systemDefault()).getMonthValue());
    }

    private String getNextYearMonth() {
        Instant now = Instant.now();
        java.time.LocalDate nextMonth = java.time.LocalDate.now().plusMonths(1);
        return String.format("%04d-%02d", nextMonth.getYear(), nextMonth.getMonthValue());
    }

    // Getters for DAOs
    public BlockLogDAO getBlockLogDAO() {
        return blockLogDAO;
    }

    public RegionBaselineDAO getRegionBaselineDAO() {
        return regionBaselineDAO;
    }

    public RenaturationProgressDAO getRenaturationProgressDAO() {
        return renaturationProgressDAO;
    }

    public BlockPalette getPalette() {
        return palette;
    }
}

