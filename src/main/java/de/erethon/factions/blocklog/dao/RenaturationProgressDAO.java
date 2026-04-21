package de.erethon.factions.blocklog.dao;

import de.erethon.factions.blocklog.model.RenaturationPhase;
import de.erethon.factions.blocklog.model.RenaturationProgress;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * DAO for renaturation progress operations.
 *
 * @author Malfrador
 */
@RegisterBeanMapper(RenaturationProgress.class)
public interface RenaturationProgressDAO {

    /**
     * Creates a new renaturation progress entry.
     */
    @SqlUpdate("""
        INSERT INTO renaturation_progress 
        (region_id, started_at, current_y_level, max_y_processed, min_y_target, 
         phase, decay_rules_json, last_processed, estimated_completion)
        VALUES 
        (:regionId, :startedAt, :currentYLevel, :maxYProcessed, :minYTarget,
         :phase, CAST(:decayRulesJson AS jsonb), :lastProcessed, :estimatedCompletion)
        """)
    void create(@BindBean RenaturationProgress progress);

    /**
     * Gets renaturation progress for a region.
     */
    @SqlQuery("""
        SELECT * FROM renaturation_progress
        WHERE region_id = :regionId
        """)
    Optional<RenaturationProgress> get(@Bind("regionId") int regionId);

    /**
     * Updates the current Y level and last processed timestamp.
     */
    @SqlUpdate("""
        UPDATE renaturation_progress
        SET current_y_level = :yLevel,
            last_processed = NOW()
        WHERE region_id = :regionId
        """)
    void updateYLevel(@Bind("regionId") int regionId,
                      @Bind("yLevel") short yLevel);

    /**
     * Updates the phase of renaturation.
     */
    @SqlUpdate("""
        UPDATE renaturation_progress
        SET phase = :phase,
            last_processed = NOW()
        WHERE region_id = :regionId
        """)
    void updatePhase(@Bind("regionId") int regionId,
                     @Bind("phase") RenaturationPhase phase);

    /**
     * Gets all active renaturation processes.
     */
    @SqlQuery("""
        SELECT * FROM renaturation_progress
        WHERE phase != 'COMPLETE'
        ORDER BY last_processed ASC
        """)
    List<RenaturationProgress> getAllActive();

    /**
     * Deletes a renaturation progress entry.
     */
    @SqlUpdate("""
        DELETE FROM renaturation_progress
        WHERE region_id = :regionId
        """)
    void delete(@Bind("regionId") int regionId);

    /**
     * Checks if a region has active renaturation.
     */
    @SqlQuery("""
        SELECT EXISTS(
            SELECT 1 FROM renaturation_progress 
            WHERE region_id = :regionId 
            AND phase != 'COMPLETE'
        )
        """)
    boolean hasActiveRenaturation(@Bind("regionId") int regionId);
}

