package de.erethon.factions.war;

import de.erethon.bedrock.misc.FileUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.WarRegion;
import de.erethon.factions.war.entities.caravans.CaravanRouting;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class War {

    private static final Factions plugin = Factions.get();

    // I hate it otherwise
    public static Alliance ALLIANCE_RED;
    public static Alliance ALLIANCE_GREEN;
    public static Alliance ALLIANCE_BLUE;

    private WarPhaseManager phaseManager;
    private WarScore score;
    private CaravanRouting caravanRouting;
    private YamlConfiguration storage;
    private final Set<RegionalWarTracker> regionsAtWar = new HashSet<>();
    private final Region theanorRegion;

    public War() {
        File scheduleFile = FileUtil.initFile(plugin, new File(Factions.WAR, "schedule.yml"), "defaults/schedule.yml");
        File file = new File(Factions.WAR, "war.yml");
        if (file.exists()) {
            storage = YamlConfiguration.loadConfiguration(file);
        } else {
            storage = new YamlConfiguration();
        }
        try { // With how broken this is, I'm not even going to try to fix it
            phaseManager = new WarPhaseManager(scheduleFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load war schedule. Please fix me.");
            e.printStackTrace();
        }
        theanorRegion = plugin.getRegionManager().getRegionByName("Theanor");
        if (theanorRegion == null) {
            plugin.getLogger().warning("Region named Theanor region not found. Please create it.");
            return;
        }
        caravanRouting = new CaravanRouting();
        load();
    }

    public WarPhaseManager getPhaseManager() {
        return phaseManager;
    }

    public WarScore getScore() {
        return score;
    }

    public CaravanRouting getCaravanRouting() {
        return caravanRouting;
    }

    public WarPhase getCurrentPhase() {
        return phaseManager.getCurrentWarPhase();
    }

    public void registerRegion(RegionalWarTracker tracker) {
        regionsAtWar.add(tracker);
    }

    public void unregisterRegion(RegionalWarTracker tracker) {
        regionsAtWar.remove(tracker);
    }

    public @Nullable WarRegion getNearestWaypoint(@Nullable Location location, @Nullable Alliance alliance) {
        if (location == null || location.getWorld() == null || alliance == null) {
            return null;
        }
        WarRegion nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (RegionalWarTracker tracker : regionsAtWar) {
            Region region = tracker.getRegion();
            if (!(region instanceof WarRegion warRegion)) {
                continue;
            }
            Location waypoint = tracker.getWaypointSpawn();
            if (waypoint == null || waypoint.getWorld() == null || waypoint.getWorld() != location.getWorld()) {
                continue;
            }
            if (!tracker.isWaypointAvailable(alliance)) {
                continue;
            }
            double distance = waypoint.distanceSquared(location);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = warRegion;
            }
        }
        return nearest;
    }

    public @Nullable Location getNearestWaypointLocation(@Nullable Location location, @Nullable Alliance alliance) {
        WarRegion region = getNearestWaypoint(location, alliance);
        return region == null ? null : region.getRegionalWarTracker().getWaypointSpawn();
    }

    public void save() {
        phaseManager.save();
        storage.set("score", score.save());
        try {
            storage.save(new File(Factions.WAR, "war.yml"));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save war data.");
            e.printStackTrace();
        }
    }

    public void load() {
        phaseManager.load();
        if (storage.contains("score")) {
            score = new WarScore(storage.getConfigurationSection("score"));
        } else {
            score = new WarScore(null);
        }
    }

    public static void initializeAlliances() {
        ALLIANCE_RED = plugin.getAllianceCache().getById(0);
        ALLIANCE_GREEN = plugin.getAllianceCache().getById(1);
        ALLIANCE_BLUE = plugin.getAllianceCache().getById(2);
    }
}
