package de.erethon.factions.war;

import de.erethon.bedrock.misc.FileUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.region.Region;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class War {

    private static final Factions plugin = Factions.get();

    // I hate it otherwise
    public static final Alliance ALLIANCE_RED = plugin.getAllianceCache().getById(0);
    public static final Alliance ALLIANCE_GREEN = plugin.getAllianceCache().getById(1);
    public static final Alliance ALLIANCE_BLUE = plugin.getAllianceCache().getById(2);

    private WarPhaseManager phaseManager;
    private WarScore score;
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
        load();
    }

    public WarPhaseManager getPhaseManager() {
        return phaseManager;
    }

    public WarScore getScore() {
        return score;
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
}
