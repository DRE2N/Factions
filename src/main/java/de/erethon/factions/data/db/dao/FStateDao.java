package de.erethon.factions.data.db.dao;

import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RegisterConstructorMapper(FStateDao.EntityState.class)
@RegisterConstructorMapper(FStateDao.PlayerState.class)
@RegisterConstructorMapper(FStateDao.RegionState.class)
@RegisterConstructorMapper(FStateDao.BuildSiteState.class)
@RegisterConstructorMapper(FStateDao.WarHistoryState.class)
public interface FStateDao {

    record EntityState(int id, UUID uuid, String name, String description, String state) {
    }

    record PlayerState(UUID uuid, String state) {
    }

    record RegionState(UUID worldId, int regionId, Integer ownerFactionId, Integer allianceId, double lastClaimingPrice) {
    }

    record BuildSiteState(UUID uuid, String state) {
    }

    record WarHistoryState(long endDate, String state) {
    }

    @SqlQuery("SELECT faction_id AS id, uuid, name, description, state FROM f_factions ORDER BY faction_id")
    List<EntityState> getFactions();

    @SqlUpdate("""
        INSERT INTO f_factions (faction_id, uuid, alliance_id, name, description, state, updated_at)
        VALUES (:id, :uuid, :allianceId, :name, :description, :state, NOW())
        ON CONFLICT (faction_id) DO UPDATE SET
            uuid = EXCLUDED.uuid,
            alliance_id = EXCLUDED.alliance_id,
            name = EXCLUDED.name,
            description = EXCLUDED.description,
            state = EXCLUDED.state,
            updated_at = NOW()
        """)
    void upsertFaction(@Bind("id") int id,
                       @Bind("uuid") UUID uuid,
                       @Bind("allianceId") Integer allianceId,
                       @Bind("name") String name,
                       @Bind("description") String description,
                       @Bind("state") String state);

    @SqlUpdate("DELETE FROM f_factions WHERE faction_id = :id")
    void deleteFaction(@Bind("id") int id);

    @SqlQuery("SELECT COALESCE(MAX(faction_id), -1) + 1 FROM f_factions")
    int getNextFactionId();

    @SqlQuery("SELECT alliance_id AS id, uuid, name, description, state FROM f_alliance_state WHERE alliance_id = :id")
    Optional<EntityState> getAlliance(@Bind("id") int id);

    @SqlUpdate("""
        INSERT INTO f_alliance_state (alliance_id, uuid, name, description, state, updated_at)
        VALUES (:id, :uuid, :name, :description, :state, NOW())
        ON CONFLICT (alliance_id) DO UPDATE SET
            uuid = EXCLUDED.uuid,
            name = EXCLUDED.name,
            description = EXCLUDED.description,
            state = EXCLUDED.state,
            updated_at = NOW()
        """)
    void upsertAlliance(@Bind("id") int id,
                        @Bind("uuid") UUID uuid,
                        @Bind("name") String name,
                        @Bind("description") String description,
                        @Bind("state") String state);

    @SqlQuery("SELECT player_uuid AS uuid, state FROM f_players WHERE player_uuid = :uuid")
    Optional<PlayerState> getPlayer(@Bind("uuid") UUID uuid);

    @SqlUpdate("""
        INSERT INTO f_players (player_uuid, state, updated_at)
        VALUES (:uuid, :state, NOW())
        ON CONFLICT (player_uuid) DO UPDATE SET
            state = EXCLUDED.state,
            updated_at = NOW()
        """)
    void upsertPlayer(@Bind("uuid") UUID uuid, @Bind("state") String state);

    @SqlQuery("SELECT world_id AS worldId, region_id AS regionId, owner_faction_id AS ownerFactionId, alliance_id AS allianceId, last_claiming_price AS lastClaimingPrice FROM f_region_ownership")
    List<RegionState> getRegionStates();

    @SqlUpdate("""
        INSERT INTO f_region_ownership (world_id, region_id, owner_faction_id, alliance_id, last_claiming_price, updated_at)
        VALUES (:worldId, :regionId, :ownerFactionId, :allianceId, :lastClaimingPrice, NOW())
        ON CONFLICT (world_id, region_id) DO UPDATE SET
            owner_faction_id = EXCLUDED.owner_faction_id,
            alliance_id = EXCLUDED.alliance_id,
            last_claiming_price = EXCLUDED.last_claiming_price,
            updated_at = NOW()
        """)
    void upsertRegionState(@Bind("worldId") UUID worldId,
                           @Bind("regionId") int regionId,
                           @Bind("ownerFactionId") Integer ownerFactionId,
                           @Bind("allianceId") Integer allianceId,
                           @Bind("lastClaimingPrice") double lastClaimingPrice);

    @SqlQuery("SELECT uuid, state FROM f_build_sites WHERE uuid = :uuid")
    Optional<BuildSiteState> getBuildSite(@Bind("uuid") UUID uuid);

    @SqlQuery("SELECT uuid, state FROM f_build_sites")
    List<BuildSiteState> getBuildSites();

    @SqlUpdate("""
        INSERT INTO f_build_sites (uuid, state, updated_at)
        VALUES (:uuid, :state, NOW())
        ON CONFLICT (uuid) DO UPDATE SET
            state = EXCLUDED.state,
            updated_at = NOW()
        """)
    void upsertBuildSite(@Bind("uuid") UUID uuid, @Bind("state") String state);

    @SqlUpdate("DELETE FROM f_build_sites WHERE uuid = :uuid")
    void deleteBuildSite(@Bind("uuid") UUID uuid);

    @SqlQuery("SELECT state FROM f_war_state WHERE id = 1")
    Optional<String> getWarState();

    @SqlUpdate("""
        INSERT INTO f_war_state (id, state, updated_at)
        VALUES (1, :state, NOW())
        ON CONFLICT (id) DO UPDATE SET state = EXCLUDED.state, updated_at = NOW()
        """)
    void upsertWarState(@Bind("state") String state);

    @SqlQuery("SELECT state FROM f_region_war_state WHERE world_id = :worldId AND region_id = :regionId")
    Optional<String> getRegionWarState(@Bind("worldId") UUID worldId, @Bind("regionId") int regionId);

    @SqlUpdate("""
        INSERT INTO f_region_war_state (world_id, region_id, state, updated_at)
        VALUES (:worldId, :regionId, :state, NOW())
        ON CONFLICT (world_id, region_id) DO UPDATE SET state = EXCLUDED.state, updated_at = NOW()
        """)
    void upsertRegionWarState(@Bind("worldId") UUID worldId, @Bind("regionId") int regionId, @Bind("state") String state);

    @SqlQuery("SELECT end_date AS endDate, state FROM f_war_history ORDER BY end_date")
    List<WarHistoryState> getWarHistory();

    @SqlUpdate("""
        INSERT INTO f_war_history (end_date, state)
        VALUES (:endDate, :state)
        ON CONFLICT (end_date) DO UPDATE SET state = EXCLUDED.state
        """)
    void upsertWarHistory(@Bind("endDate") long endDate, @Bind("state") String state);
}
