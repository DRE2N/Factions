package de.erethon.factions.blocklog.dao;

import de.erethon.factions.blocklog.model.BlockChange;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * DAO for block log operations.
 *
 * @author Malfrador
 */
@RegisterRowMapper(BlockChangeMapper.class)
public interface BlockLogDAO {

    /**
     * Batch inserts block changes.
     */
    @SqlBatch("""
        INSERT INTO block_log 
        (region_id, x, y, z, world_name, old_block_id, new_block_id, 
         change_type, cause_uuid, cause_name, timestamp, baseline_version)
        VALUES 
        (:regionId, :x, :y, :z, :worldName, :oldBlockId, :newBlockId,
         :changeType, :causeUuid, :causeName, :timestamp, :baselineVersion)
        """)
    void batchInsert(@BindMethods List<BlockChange> changes);

    /**
     * Gets all block changes for a region within a time range.
     */
    @SqlQuery("""
        SELECT * FROM block_log
        WHERE region_id = :regionId
        AND timestamp >= :startTime
        AND timestamp <= :endTime
        ORDER BY timestamp DESC
        """)
    List<BlockChange> getRegionChanges(@Bind("regionId") int regionId,
                                       @Bind("startTime") Timestamp startTime,
                                       @Bind("endTime") Timestamp endTime);

    /**
     * Gets player-placed blocks for a region at a specific Y level.
     */
    @SqlQuery("""
        SELECT * FROM block_log
        WHERE region_id = :regionId
        AND y = :yLevel
        AND change_type = 'PLAYER_PLACE'
        AND reverted = FALSE
        ORDER BY timestamp ASC
        """)
    List<BlockChange> getPlayerPlacedBlocksAtY(@Bind("regionId") int regionId,
                                               @Bind("yLevel") int yLevel);

    /**
     * Gets player-placed blocks in a specific chunk (optimized for lazy renaturation).
     */
    @SqlQuery("""
        SELECT * FROM block_log
        WHERE region_id = :regionId
        AND world_name = :worldName
        AND x >= :minX AND x <= :maxX
        AND z >= :minZ AND z <= :maxZ
        AND change_type = 'PLAYER_PLACE'
        AND reverted = FALSE
        ORDER BY y DESC, timestamp ASC
        """)
    List<BlockChange> getPlayerPlacedBlocksInChunk(@Bind("regionId") int regionId,
                                                   @Bind("worldName") String worldName,
                                                   @Bind("minX") int minX,
                                                   @Bind("maxX") int maxX,
                                                   @Bind("minZ") int minZ,
                                                   @Bind("maxZ") int maxZ);

    /**
     * Gets player modifications (place + break) in a chunk above a certain Y level.
     * Used for renaturation chunk catch-up: only process blocks above the current sweep line.
     */
    @SqlQuery("""
        SELECT * FROM block_log
        WHERE region_id = :regionId
        AND world_name = :worldName
        AND x >= :minX AND x <= :maxX
        AND z >= :minZ AND z <= :maxZ
        AND y >= :minY
        AND (change_type = 'PLAYER_PLACE' OR change_type = 'PLAYER_BREAK')
        AND reverted = FALSE
        ORDER BY y DESC, timestamp ASC
        """)
    List<BlockChange> getPlayerChangesInChunkAboveY(@Bind("regionId") int regionId,
                                                    @Bind("worldName") String worldName,
                                                    @Bind("minX") int minX,
                                                    @Bind("maxX") int maxX,
                                                    @Bind("minZ") int minZ,
                                                    @Bind("maxZ") int maxZ,
                                                    @Bind("minY") int minY);

    /**
     * Gets player modifications (place + break) in a region between two Y levels.
     * Used for renaturation sweep: process blocks in a Y-level band.
     */
    @SqlQuery("""
        SELECT * FROM block_log
        WHERE region_id = :regionId
        AND y >= :minY AND y <= :maxY
        AND (change_type = 'PLAYER_PLACE' OR change_type = 'PLAYER_BREAK')
        AND reverted = FALSE
        ORDER BY y DESC, timestamp ASC
        """)
    List<BlockChange> getPlayerChangesInYBand(@Bind("regionId") int regionId,
                                              @Bind("minY") int minY,
                                              @Bind("maxY") int maxY);

    /**
     * Gets all player changes for a region since a baseline version.
     */
    @SqlQuery("""
        SELECT * FROM block_log
        WHERE region_id = :regionId
        AND baseline_version = :baselineVersion
        AND (change_type = 'PLAYER_PLACE' OR change_type = 'PLAYER_BREAK')
        AND reverted = FALSE
        ORDER BY timestamp ASC
        """)
    List<BlockChange> getPlayerChangesSinceBaseline(@Bind("regionId") int regionId,
                                                    @Bind("baselineVersion") int baselineVersion);

    /**
     * Gets block changes by a specific player.
     */
    @SqlQuery("""
        SELECT * FROM block_log
        WHERE cause_uuid = :playerUuid
        AND timestamp >= :startTime
        AND timestamp <= :endTime
        ORDER BY timestamp DESC
        LIMIT :limit
        """)
    List<BlockChange> getPlayerChanges(@Bind("playerUuid") UUID playerUuid,
                                       @Bind("startTime") Timestamp startTime,
                                       @Bind("endTime") Timestamp endTime,
                                       @Bind("limit") int limit);

    /**
     * Gets block changes at a specific position.
     */
    @SqlQuery("""
        SELECT * FROM block_log
        WHERE world_name = :worldName
        AND x = :x
        AND y = :y
        AND z = :z
        ORDER BY timestamp DESC
        LIMIT :limit
        """)
    List<BlockChange> getBlockHistory(@Bind("worldName") String worldName,
                                      @Bind("x") int x,
                                      @Bind("y") int y,
                                      @Bind("z") int z,
                                      @Bind("limit") int limit);

    /**
     * Gets ALL player changes (place + break) for a region within a bounding box,
     * ordered oldest-first. Used for map update replay scoped to a specific area.
     */
    @SqlQuery("""
        SELECT * FROM block_log
        WHERE region_id = :regionId
        AND x >= :minX AND x <= :maxX
        AND z >= :minZ AND z <= :maxZ
        AND (change_type = 'PLAYER_PLACE' OR change_type = 'PLAYER_BREAK')
        AND reverted = FALSE
        ORDER BY timestamp ASC
        """)
    List<BlockChange> getAllPlayerChangesInBounds(@Bind("regionId") int regionId,
                                                  @Bind("minX") int minX,
                                                  @Bind("maxX") int maxX,
                                                  @Bind("minZ") int minZ,
                                                  @Bind("maxZ") int maxZ);

    /**
     * Gets the latest (most recent) player change per block position within a bounding box.
     * Uses a window function to avoid loading all historical rows per coordinate.
     * The result is already deduplicated — one row per (x, y, z), the newest change wins.
     * Used for map update replay so the final player-visible state is restored.
     */
    @SqlQuery("""
        SELECT DISTINCT ON (x, y, z) *
        FROM block_log
        WHERE region_id = :regionId
        AND x >= :minX AND x <= :maxX
        AND z >= :minZ AND z <= :maxZ
        AND (change_type = 'PLAYER_PLACE' OR change_type = 'PLAYER_BREAK')
        AND reverted = FALSE
        ORDER BY x, y, z, timestamp DESC
        """)
    List<BlockChange> getLatestPlayerChangesInBounds(@Bind("regionId") int regionId,
                                                      @Bind("minX") int minX,
                                                      @Bind("maxX") int maxX,
                                                      @Bind("minZ") int minZ,
                                                      @Bind("maxZ") int maxZ);

    /**
     * Gets ALL player changes (place + break) for a region across all baseline versions,
     * ordered oldest-first. Avoid for large regions — prefer getLatestPlayerChangesInBounds
     * per chunk instead.
     */
    @SqlQuery("""
        SELECT * FROM block_log
        WHERE region_id = :regionId
        AND (change_type = 'PLAYER_PLACE' OR change_type = 'PLAYER_BREAK')
        AND reverted = FALSE
        ORDER BY timestamp ASC
        """)
    List<BlockChange> getAllPlayerChanges(@Bind("regionId") int regionId);

    @SqlUpdate("""
        UPDATE block_log
        SET reverted = TRUE,
            reverted_at = NOW(),
            reverted_by_uuid = :revertedBy,
            reverted_by_name = :revertedByName
        WHERE id IN (<ids>)
        AND reverted = FALSE
        """)
    int markReverted(@BindList("ids") List<Long> ids,
                     @Bind("revertedBy") UUID revertedBy,
                     @Bind("revertedByName") String revertedByName);

    /**
     * Counts total logged changes for a region.
     */
    @SqlQuery("""
        SELECT COUNT(*) FROM block_log
        WHERE region_id = :regionId
        """)
    long countRegionChanges(@Bind("regionId") int regionId);

    /**
     * Deletes old block log entries before a timestamp.
     */
    @SqlQuery("""
        DELETE FROM block_log
        WHERE timestamp < :cutoffTime
        """)
    int deleteOldEntries(@Bind("cutoffTime") Timestamp cutoffTime);
}

