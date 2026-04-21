package de.erethon.factions.blocklog.dao;

import de.erethon.factions.blocklog.model.RegionBaseline;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.Timestamp;
import java.util.Optional;

/**
 * DAO for region baseline operations.
 *
 * @author Malfrador
 */
@RegisterBeanMapper(RegionBaseline.class)
public interface RegionBaselineDAO {

    /**
     * Creates a new baseline entry for a region.
     */
    @SqlUpdate("""
        INSERT INTO region_baseline (region_id, current_version, current_schematic_path, created_at, updated_at)
        VALUES (:regionId, :version, :schematicPath, NOW(), NOW())
        ON CONFLICT (region_id) DO NOTHING
        """)
    void create(@Bind("regionId") int regionId,
                @Bind("version") int version,
                @Bind("schematicPath") String schematicPath);

    @SqlUpdate("""
        INSERT INTO region_baseline (region_id, current_version, current_schematic_path, created_at, updated_at)
        VALUES (:regionId, 0, NULL, NOW(), NOW())
        ON CONFLICT (region_id) DO NOTHING
        """)
    void ensureExists(@Bind("regionId") int regionId);

    /**
     * Gets the baseline for a region.
     */
    @SqlQuery("""
        SELECT * FROM region_baseline
        WHERE region_id = :regionId
        """)
    Optional<RegionBaseline> get(@Bind("regionId") int regionId);

    /**
     * Updates the baseline version for a region.
     */
    @SqlUpdate("""
        UPDATE region_baseline
        SET current_version = :version,
            current_schematic_path = :schematicPath,
            updated_at = NOW()
        WHERE region_id = :regionId
        """)
    void updateVersion(@Bind("regionId") int regionId,
                       @Bind("version") int version,
                       @Bind("schematicPath") String schematicPath);

    /**
     * Adds a baseline version to history.
     */
    @SqlUpdate("""
        INSERT INTO region_baseline_history (region_id, version, schematic_path, created_at, notes)
        VALUES (:regionId, :version, :schematicPath, NOW(), :notes)
        ON CONFLICT (region_id, version) DO UPDATE
        SET schematic_path = EXCLUDED.schematic_path,
            created_at = NOW(),
            notes = EXCLUDED.notes
        """)
    void addToHistory(@Bind("regionId") int regionId,
                      @Bind("version") int version,
                      @Bind("schematicPath") String schematicPath,
                      @Bind("notes") String notes);

    @SqlQuery("""
        SELECT COALESCE(MAX(version), 0) FROM region_baseline_history
        WHERE region_id = :regionId
        """)
    int getLatestHistoryVersion(@Bind("regionId") int regionId);

    /**
     * Gets the current baseline version for a region.
     */
    @SqlQuery("""
        SELECT current_version FROM region_baseline
        WHERE region_id = :regionId
        """)
    Optional<Integer> getCurrentVersion(@Bind("regionId") int regionId);

    /**
     * Checks if a region has a baseline.
     */
    @SqlQuery("""
        SELECT EXISTS(
            SELECT 1 FROM region_baseline
            WHERE region_id = :regionId AND current_version > 0
        )
        """)
    boolean hasBaseline(@Bind("regionId") int regionId);
}

