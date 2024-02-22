package de.erethon.factions.war;

import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.util.WarMath;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Fyreum
 */
public class RegionalWarTracker {

    public static final double DEFAULT_CAPTURE_CAP = 200.0;
    public static final int DEFAULT_REGION_VALUE = 2;

    protected final Factions plugin = Factions.get();

    private final Region region;
    private final Map<Alliance, Integer> kills = new HashMap<>();
    private final Map<Alliance, Double> scores = new HashMap<>();
    private final Set<Player> crystalCarriers = new HashSet<>();
    private double captureCap = DEFAULT_CAPTURE_CAP;
    private int regionValue = DEFAULT_REGION_VALUE;

    public RegionalWarTracker(@NotNull Region region) {
        this.region = region;
    }

    public void reset() {
        kills.clear();
        scores.clear();
    }

    /* Serialization */

    public void load(@Nullable ConfigurationSection config) {
        if (config == null) {
            return;
        }
        this.captureCap = config.getDouble("captureCap", captureCap);
        ConfigurationSection killsSection = config.getConfigurationSection("kills");
        if (killsSection != null) {
            for (String key : killsSection.getKeys(false)) {
                int allianceId = NumberUtil.parseInt(key, -1);
                Alliance alliance = plugin.getAllianceCache().getById(allianceId);
                if (alliance == null) {
                    FLogger.ERROR.log("Unknown alliance ID in region '" + region.getId() + "' found: " + key);
                    continue;
                }
                kills.put(alliance, config.getInt("kills." + key));
            }
        }
        this.regionValue = config.getInt("regionValue", regionValue);
        ConfigurationSection scoresSection = config.getConfigurationSection("scores");
        if (scoresSection != null) {
            for (String key : scoresSection.getKeys(false)) {
                int allianceId = NumberUtil.parseInt(key, -1);
                Alliance alliance = plugin.getAllianceCache().getById(allianceId);
                if (alliance == null) {
                    FLogger.ERROR.log("Unknown alliance ID in region '" + region.getId() + "' found: " + key);
                    continue;
                }
                scores.put(alliance, config.getDouble("scores." + key));
            }
        }
    }

    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> serialized = new HashMap<>();
        Map<String, Object> serializedKills = new HashMap<>(kills.size());
        Map<String, Object> serializedScores = new HashMap<>(scores.size());

        kills.forEach((alliance, kills) -> serializedKills.put(String.valueOf(alliance.getId()), kills));
        scores.forEach((alliance, score) -> serializedScores.put(String.valueOf(alliance.getId()), score));

        if (captureCap != DEFAULT_CAPTURE_CAP) {
            serialized.put("captureCap", captureCap);
        }
        if (regionValue != DEFAULT_REGION_VALUE) {
            serialized.put("regionValue", regionValue);
        }
        serialized.put("kills", serializedKills);
        serialized.put("scores", serializedScores);
        return serialized;
    }

    /* Getters and setters */

    public @NotNull Region getRegion() {
        return region;
    }

    public @NotNull Map<Alliance, Integer> getKills() {
        return kills;
    }

    public int getKills(@NotNull Alliance alliance) {
        return kills.getOrDefault(alliance, 0);
    }

    public double getKillsAsScore(@NotNull Alliance alliance) {
        int kills = getKills(alliance);
        if (kills == 0) {
            return 0;
        }
        double score = 0;
        while (kills > 0) {
            score += WarMath.scoreForKills(kills);
            kills--;
        }
        return score;
    }

    public void addKill(@NotNull Alliance alliance) {
        int newKills = getKills(alliance) + 1;
        kills.put(alliance, newKills);
        addScore(alliance, WarMath.scoreForKills(newKills));
    }

    public @NotNull Map<Alliance, Double> getScores() {
        return scores;
    }

    public double getScore(@NotNull Alliance alliance) {
        return scores.getOrDefault(alliance, 0.0);
    }

    // Returns a percentage [0 - 100]%, not a factor [0 - 1]
    public double getScoreAsPercentage(@NotNull Alliance alliance) {
        double score = getScore(alliance);
        if (score == 0) {
            return 0;
        }
        return de.erethon.aergia.util.NumberUtil.round(100 / captureCap * score);
    }

    public void addScore(@NotNull Alliance alliance, double score) {
        double newScore = getScore(alliance) + score;
        scores.put(alliance, newScore);
        if (newScore >= captureCap) {
            alliance.temporaryOccupy(region);
        }
    }

    public @Nullable Alliance getLeader() {
        Alliance winner = null;
        double score = -1;
        double secondScore = 0;
        for (Alliance alliance : plugin.getAllianceCache()) {
            double currentScore = getScore(alliance);
            if (currentScore <= 0) {
                continue;
            }
            if (currentScore > score) {
                winner = alliance;
                score = currentScore;
            } else if (currentScore > secondScore) {
                secondScore = currentScore;
            }
        }
        return score > secondScore ? winner : null;
    }

    public double getCaptureCap() {
        return captureCap;
    }

    public void setCaptureCap(double captureCap) {
        this.captureCap = captureCap;
    }

    public int getRegionValue() {
        return regionValue;
    }

    public void setRegionValue(int regionValue) {
        this.regionValue = regionValue;
    }

    public boolean isCrystalCarrier(@NotNull Player player) {
        return crystalCarriers.contains(player);
    }

    public void addCrystalCarrier(@NotNull Player player) {
        crystalCarriers.add(player);
    }

    public void removeCrystalCarrier(@NotNull Player player) {
        crystalCarriers.remove(player);
    }
}
