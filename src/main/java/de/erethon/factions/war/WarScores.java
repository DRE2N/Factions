package de.erethon.factions.war;

import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Fyreum
 */
public class WarScores {

    final Factions plugin = Factions.get();
    private final Alliance alliance;
    private final ConfigurationSection config;
    private final Map<Region, RegionalScore> regionalScores = new HashMap<>();

    public WarScores(@NotNull Alliance alliance, @Nullable ConfigurationSection config) {
        this.alliance = alliance;
        this.config = config;
        load();
    }

    public void load() {
        if (config == null) {
            return;
        }
        ConfigurationSection regions = config.getConfigurationSection("regions");
        if (regions == null) {
            return;
        }
        for (String regionId : regions.getKeys(false)) {
            int id = NumberUtil.parseInt(regionId, -1);
            if (id < 0) {
                FLogger.ERROR.log("Illegal region id found: " + regionId);
                continue;
            }
            Region region = plugin.getRegionManager().getRegionById(id);
            if (region == null) {
                FLogger.ERROR.log("Unknown region id found: " + regionId);
                continue;
            }
            RegionalScore score = new RegionalScore(regions.getInt(regionId + ".kills"), regions.getDouble(regionId + ".totalScore"));
            regionalScores.put(region, score);
        }
    }

    public @Nullable RegionalScore get(@NotNull Region region) {
        return regionalScores.get(region);
    }

    public @NotNull RegionalScore getOrCreate(@NotNull Region region) {
        regionalScores.putIfAbsent(region, new RegionalScore(0, 0));
        return regionalScores.get(region);
    }

    public @NotNull WarScores reset(@NotNull Region region) {
        regionalScores.put(region, null);
        return this;
    }

    /* Serialization */

    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> serialized = new HashMap<>(regionalScores.size());
        regionalScores.forEach((region, score) -> serialized.put(String.valueOf(region.getId()), score.serialize()));
        return serialized;
    }

    /* Getters and setters */

    public @NotNull Alliance getAlliance() {
        return alliance;
    }

    public @NotNull Map<Region, RegionalScore> getRegionalScores() {
        return regionalScores;
    }
}
