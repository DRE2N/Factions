package de.erethon.factions.data.db;

import de.erethon.bedrock.database.BedrockDBConnection;
import de.erethon.bedrock.database.EDatabaseManager;
import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.data.db.dao.FStateDao;
import de.erethon.factions.entity.FLegalEntity;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.ClaimableRegion;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionCache;
import de.erethon.factions.region.WarRegion;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.war.RegionalWarTracker;
import de.erethon.factions.war.WarHistory;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jdbi.v3.core.Jdbi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

public class FDatabaseManager extends EDatabaseManager {

    private final Factions plugin;
    private FStateDao stateDao;

    public FDatabaseManager(Factions plugin, BedrockDBConnection connection) {
        super(connection, new ThreadPoolExecutor(2, 4, 60L, java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<>()));
        this.plugin = plugin;
        getInitializationFuture().whenComplete((unused, throwable) -> {
            if (throwable != null) {
                FLogger.ERROR.log("Factions database initialization failed: " + throwable.getMessage());
                throwable.printStackTrace();
                return;
            }
            stateDao = getDao(FStateDao.class);
            FLogger.INFO.log("Factions database initialized");
        });
    }

    @Override
    protected CompletableFuture<Void> initializeSchema() {
        return CompletableFuture.runAsync(() -> jdbi.useHandle(handle -> {
            handle.execute("""
                CREATE TABLE IF NOT EXISTS f_schema_version (
                    version INTEGER PRIMARY KEY,
                    applied_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);

            handle.execute("""
                CREATE TABLE IF NOT EXISTS f_alliance_state (
                    alliance_id INTEGER PRIMARY KEY,
                    uuid UUID NOT NULL,
                    name TEXT NOT NULL,
                    description TEXT,
                    state TEXT NOT NULL DEFAULT '',
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);

            handle.execute("""
                CREATE TABLE IF NOT EXISTS f_factions (
                    faction_id INTEGER PRIMARY KEY,
                    uuid UUID NOT NULL,
                    alliance_id INTEGER,
                    name TEXT NOT NULL,
                    description TEXT,
                    state TEXT NOT NULL DEFAULT '',
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);

            handle.execute("""
                CREATE TABLE IF NOT EXISTS f_players (
                    player_uuid UUID PRIMARY KEY,
                    state TEXT NOT NULL DEFAULT '',
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);

            handle.execute("""
                CREATE TABLE IF NOT EXISTS f_region_ownership (
                    world_id UUID NOT NULL,
                    region_id INTEGER NOT NULL,
                    owner_faction_id INTEGER,
                    alliance_id INTEGER,
                    last_claiming_price DOUBLE PRECISION NOT NULL DEFAULT 0,
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    PRIMARY KEY (world_id, region_id)
                )
                """);

            handle.execute("""
                CREATE TABLE IF NOT EXISTS f_faction_economy (
                    faction_id INTEGER PRIMARY KEY,
                    state TEXT NOT NULL DEFAULT '',
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);

            handle.execute("""
                CREATE TABLE IF NOT EXISTS f_build_sites (
                    uuid UUID PRIMARY KEY,
                    state TEXT NOT NULL DEFAULT '',
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);

            handle.execute("""
                CREATE TABLE IF NOT EXISTS f_build_site_sections (
                    build_site_uuid UUID NOT NULL,
                    section_id TEXT NOT NULL,
                    state TEXT NOT NULL DEFAULT '',
                    PRIMARY KEY (build_site_uuid, section_id)
                )
                """);

            handle.execute("""
                CREATE TABLE IF NOT EXISTS f_build_site_positions (
                    build_site_uuid UUID NOT NULL,
                    position_id TEXT NOT NULL,
                    state TEXT NOT NULL DEFAULT '',
                    PRIMARY KEY (build_site_uuid, position_id)
                )
                """);

            handle.execute("""
                CREATE TABLE IF NOT EXISTS f_build_site_material_progress (
                    build_site_uuid UUID NOT NULL,
                    progress_key TEXT NOT NULL,
                    amount INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (build_site_uuid, progress_key)
                )
                """);

            handle.execute("""
                CREATE TABLE IF NOT EXISTS f_build_site_item_buffers (
                    build_site_uuid UUID NOT NULL,
                    buffer_name TEXT NOT NULL,
                    state TEXT NOT NULL DEFAULT '',
                    PRIMARY KEY (build_site_uuid, buffer_name)
                )
                """);

            handle.execute("""
                CREATE TABLE IF NOT EXISTS f_war_state (
                    id INTEGER PRIMARY KEY DEFAULT 1,
                    state TEXT NOT NULL DEFAULT '',
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    CHECK (id = 1)
                )
                """);

            handle.execute("""
                CREATE TABLE IF NOT EXISTS f_war_scores (
                    alliance_id INTEGER PRIMARY KEY,
                    total_score INTEGER NOT NULL DEFAULT 0,
                    objective_score INTEGER NOT NULL DEFAULT 0,
                    player_kill_score INTEGER NOT NULL DEFAULT 0,
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);

            handle.execute("""
                CREATE TABLE IF NOT EXISTS f_region_war_state (
                    world_id UUID NOT NULL,
                    region_id INTEGER NOT NULL,
                    state TEXT NOT NULL DEFAULT '',
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    PRIMARY KEY (world_id, region_id)
                )
                """);

            handle.execute("""
                CREATE TABLE IF NOT EXISTS f_war_structure_state (
                    world_id UUID NOT NULL,
                    region_id INTEGER NOT NULL,
                    structure_name TEXT NOT NULL,
                    state TEXT NOT NULL DEFAULT '',
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    PRIMARY KEY (world_id, region_id, structure_name)
                )
                """);

            handle.execute("""
                CREATE TABLE IF NOT EXISTS f_caravan_routes (
                    route_id TEXT PRIMARY KEY,
                    state TEXT NOT NULL DEFAULT '',
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);

            handle.execute("""
                CREATE TABLE IF NOT EXISTS f_active_caravan_routes (
                    route_id TEXT PRIMARY KEY,
                    state TEXT NOT NULL DEFAULT '',
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);

            handle.execute("""
                CREATE TABLE IF NOT EXISTS f_war_history (
                    end_date BIGINT PRIMARY KEY,
                    state TEXT NOT NULL DEFAULT ''
                )
                """);

            handle.execute("INSERT INTO f_schema_version (version) VALUES (1) ON CONFLICT (version) DO NOTHING");
        }), asyncExecutor);
    }

    @Override
    protected void registerCustomMappers() {
    }

    @Override
    protected void configureJdbiPlugins(Jdbi jdbiInstance) {
        super.configureJdbiPlugins(jdbiInstance);
    }

    public void awaitReady() {
        getInitializationFuture().join();
        if (stateDao == null) {
            stateDao = getDao(FStateDao.class);
        }
    }

    public List<FStateDao.EntityState> loadFactions() {
        awaitReady();
        return stateDao.getFactions();
    }

    public int nextFactionId() {
        awaitReady();
        return stateDao.getNextFactionId();
    }

    public void saveLegalEntity(FLegalEntity entity, String state) {
        awaitReady();
        if (entity instanceof Faction faction) {
            stateDao.upsertFaction(faction.getId(), faction.getUniqueId(), faction.getAlliance() == null ? null : faction.getAlliance().getId(), faction.getName(), faction.getDescription(), state);
            saveFactionEconomy(faction, state);
            return;
        }
        if (entity instanceof Alliance alliance) {
            stateDao.upsertAlliance(alliance.getId(), alliance.getUniqueId(), alliance.getName(), alliance.getDescription(), state);
        }
    }

    public Optional<String> loadAllianceState(int id) {
        awaitReady();
        return stateDao.getAlliance(id).map(FStateDao.EntityState::state);
    }

    public void deleteFaction(int id) {
        awaitReady();
        stateDao.deleteFaction(id);
    }

    public Optional<String> loadPlayerState(UUID uuid) {
        awaitReady();
        return stateDao.getPlayer(uuid).map(FStateDao.PlayerState::state);
    }

    public void savePlayer(FPlayer player, String state) {
        awaitReady();
        stateDao.upsertPlayer(player.getUniqueId(), state);
    }

    public Optional<String> loadBuildSiteState(UUID uuid) {
        awaitReady();
        return stateDao.getBuildSite(uuid).map(FStateDao.BuildSiteState::state);
    }

    public void saveBuildSite(BuildSite site, String state) {
        awaitReady();
        stateDao.upsertBuildSite(site.getUuid(), state);
    }

    public Optional<String> loadWarState() {
        awaitReady();
        return stateDao.getWarState();
    }

    public void saveWarState(String state) {
        awaitReady();
        stateDao.upsertWarState(state);
    }

    public Optional<String> loadCaravanRoutesState() {
        awaitReady();
        return jdbi.withHandle(handle -> handle.createQuery("SELECT state FROM f_caravan_routes WHERE route_id = 'routes'")
                .mapTo(String.class)
                .findOne());
    }

    public void saveCaravanRoutesState(String state) {
        awaitReady();
        jdbi.useHandle(handle -> handle.createUpdate("""
                INSERT INTO f_caravan_routes (route_id, state, updated_at)
                VALUES ('routes', :state, NOW())
                ON CONFLICT (route_id) DO UPDATE SET state = EXCLUDED.state, updated_at = NOW()
                """)
                .bind("state", state)
                .execute());
    }

    public Optional<String> loadActiveCaravanRoutesState() {
        awaitReady();
        return jdbi.withHandle(handle -> handle.createQuery("SELECT state FROM f_active_caravan_routes WHERE route_id = 'active'")
                .mapTo(String.class)
                .findOne());
    }

    public void saveActiveCaravanRoutesState(String state) {
        awaitReady();
        jdbi.useHandle(handle -> handle.createUpdate("""
                INSERT INTO f_active_caravan_routes (route_id, state, updated_at)
                VALUES ('active', :state, NOW())
                ON CONFLICT (route_id) DO UPDATE SET state = EXCLUDED.state, updated_at = NOW()
                """)
                .bind("state", state)
                .execute());
    }

    public Optional<String> loadRegionWarState(WarRegion region) {
        awaitReady();
        return stateDao.getRegionWarState(region.getWorldId(), region.getId());
    }

    public void saveRegionWarState(WarRegion region, String state) {
        awaitReady();
        stateDao.upsertRegionWarState(region.getWorldId(), region.getId(), state);
    }

    public void saveRegionWarTracker(RegionalWarTracker tracker) {
        Region region = tracker.getRegion();
        if (!(region instanceof WarRegion warRegion)) {
            return;
        }
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("tracker", tracker.serialize());
        saveRegionWarState(warRegion, cfg.saveToString());
    }

    public void applyRegionOwnership() {
        awaitReady();
        for (FStateDao.RegionState row : stateDao.getRegionStates()) {
            Region region = plugin.getRegionManager().getCache(row.worldId()) == null ? null : plugin.getRegionManager().getCache(row.worldId()).getById(row.regionId());
            if (region == null) {
                continue;
            }
            region.setAlliance(row.allianceId() == null ? null : plugin.getAllianceCache().getById(row.allianceId()));
            if (region instanceof ClaimableRegion claimable) {
                claimable.setOwnerRaw(row.ownerFactionId() == null ? null : plugin.getFactionCache().getById(row.ownerFactionId()));
                claimable.setLastClaimingPrice(row.lastClaimingPrice());
            }
        }
    }

    public void saveRegionState(Region region) {
        awaitReady();
        Integer allianceId = region.getAlliance() == null ? null : region.getAlliance().getId();
        Integer ownerId = region.getOwner() == null ? null : region.getOwner().getId();
        double lastClaimingPrice = region instanceof ClaimableRegion claimable ? claimable.getLastClaimingPrice() : 0;
        stateDao.upsertRegionState(region.getWorldId(), region.getId(), ownerId, allianceId, lastClaimingPrice);
    }

    public void loadWarHistoryInto(WarHistory history) {
        awaitReady();
        for (FStateDao.WarHistoryState row : stateDao.getWarHistory()) {
            YamlConfiguration cfg = fromString(row.state());
            history.addLoadedEntry(row.endDate(), cfg);
        }
    }

    public void saveWarHistoryEntry(WarHistory.Entry entry, String state) {
        awaitReady();
        stateDao.upsertWarHistory(entry.getEndDate(), state);
    }

    private void saveFactionEconomy(Faction faction, String state) {
        jdbi.useHandle(handle -> handle.createUpdate("""
                INSERT INTO f_faction_economy (faction_id, state, updated_at)
                VALUES (:id, :state, NOW())
                ON CONFLICT (faction_id) DO UPDATE SET state = EXCLUDED.state, updated_at = NOW()
                """)
                .bind("id", faction.getId())
                .bind("state", state)
                .execute());
    }

    public static YamlConfiguration fromString(String state) {
        YamlConfiguration cfg = new YamlConfiguration();
        if (state == null || state.isBlank()) {
            return cfg;
        }
        try {
            cfg.loadFromString(state);
        } catch (InvalidConfigurationException e) {
            FLogger.ERROR.log("Failed to parse DB state: " + e.getMessage());
        }
        return cfg;
    }

    public void saveAllRegionStates() {
        for (RegionCache cache : plugin.getRegionManager()) {
            for (Region region : cache) {
                saveRegionState(region);
                if (region instanceof WarRegion warRegion) {
                    saveRegionWarTracker(warRegion.getRegionalWarTracker());
                }
            }
        }
    }
}
