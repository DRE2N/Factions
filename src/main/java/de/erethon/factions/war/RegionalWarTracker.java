package de.erethon.factions.war;

import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.economy.FEconomy;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionType;
import de.erethon.factions.region.WarRegion;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.util.WarMath;
import de.erethon.factions.war.entities.CrystalChargeCarrier;
import de.erethon.factions.war.structure.CrystalWarStructure;
import de.erethon.factions.war.structure.OccupyWarStructure;
import de.erethon.factions.war.structure.WarCastleStructure;
import de.erethon.factions.war.structure.WarFortressStructure;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Fyreum
 */
public class RegionalWarTracker {

    public static final double DEFAULT_CAPTURE_CAP = 200.0;
    public static final int DEFAULT_REGION_VALUE = 2;
    public static final double WAYPOINT_CONTESTED_DISABLE_PERCENT = 50.0;

    protected final Factions plugin = Factions.get();

    private final WarRegion region;
    private final Map<Alliance, Integer> kills = new HashMap<>();
    private final Map<Alliance, Double> scores = new HashMap<>();
    private final Map<WarObjectiveUpgrade, Integer> upgrades = new HashMap<>();
    private final Map<Integer, Map<String, Integer>> factionContributions = new HashMap<>();
    private final Set<Player> crystalCarriers = new HashSet<>();
    private Faction operatingFaction;
    private double captureCap = DEFAULT_CAPTURE_CAP;
    private int regionValue = DEFAULT_REGION_VALUE;
    private WarObjectiveTier tier;
    private Location waypointSpawn;
    private int repairSupplies;
    private long cooldownDate = 0;
    private WarScore warScore;

    public RegionalWarTracker(@NotNull WarRegion region) {
        this.region = region;
    }

    public void reset(boolean applyCooldown) {
        if (applyCooldown) {
            cooldownDate = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(plugin.getFConfig().getWarRegionCooldownAfterOccupy());
        }
        kills.clear();
        scores.clear();
        for (Player carrier : crystalCarriers) {
            CrystalWarStructure.removeCarryingPlayerBuffs(carrier);
        }
        crystalCarriers.clear();
        plugin.getDatabaseManager().saveRegionWarTracker(this);
    }

    /* Serialization */

    public void load(@Nullable ConfigurationSection config) {
        if (config == null) {
            loadDatabaseState();
            return;
        }
        this.tier = WarObjectiveTier.getByName(config.getString("tier", ""));
        this.waypointSpawn = config.getLocation("waypointSpawn");
        this.captureCap = config.getDouble("captureCap", captureCap);
        this.operatingFaction = plugin.getFactionCache().getById(config.getInt("operatingFaction", -1));
        this.repairSupplies = config.getInt("repairSupplies", repairSupplies);
        ConfigurationSection upgradesSection = config.getConfigurationSection("upgrades");
        if (upgradesSection != null) {
            for (String key : upgradesSection.getKeys(false)) {
                WarObjectiveUpgrade upgrade = WarObjectiveUpgrade.getByName(key);
                if (upgrade != null) {
                    upgrades.put(upgrade, upgradesSection.getInt(key));
                }
            }
        }
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
        ConfigurationSection contributionsSection = config.getConfigurationSection("contributions");
        if (contributionsSection != null) {
            for (String factionId : contributionsSection.getKeys(false)) {
                int parsedFactionId = NumberUtil.parseInt(factionId, -1);
                if (parsedFactionId < 0) {
                    continue;
                }
                ConfigurationSection factionSection = contributionsSection.getConfigurationSection(factionId);
                if (factionSection == null) {
                    continue;
                }
                Map<String, Integer> stats = new HashMap<>();
                for (String key : factionSection.getKeys(false)) {
                    stats.put(key, factionSection.getInt(key));
                }
                factionContributions.put(parsedFactionId, stats);
            }
        }
        this.regionValue = config.getInt("regionValue", regionValue);
        loadDatabaseState();
    }

    private void loadDatabaseState() {
        plugin.getDatabaseManager().loadRegionWarState(region).ifPresent(state -> {
            ConfigurationSection config = de.erethon.factions.data.db.FDatabaseManager.fromString(state).getConfigurationSection("tracker");
            if (config == null) {
                return;
            }
            this.tier = WarObjectiveTier.getByName(config.getString("tier", ""));
            this.waypointSpawn = config.getLocation("waypointSpawn", waypointSpawn);
            this.captureCap = config.getDouble("captureCap", captureCap);
            this.operatingFaction = plugin.getFactionCache().getById(config.getInt("operatingFaction", -1));
            this.repairSupplies = config.getInt("repairSupplies", repairSupplies);
            this.regionValue = config.getInt("regionValue", regionValue);
            kills.clear();
            scores.clear();
            upgrades.clear();
            factionContributions.clear();
            ConfigurationSection upgradesSection = config.getConfigurationSection("upgrades");
            if (upgradesSection != null) {
                for (String key : upgradesSection.getKeys(false)) {
                    WarObjectiveUpgrade upgrade = WarObjectiveUpgrade.getByName(key);
                    if (upgrade != null) {
                        upgrades.put(upgrade, upgradesSection.getInt(key));
                    }
                }
            }
            ConfigurationSection killsSection = config.getConfigurationSection("kills");
            if (killsSection != null) {
                for (String key : killsSection.getKeys(false)) {
                    Alliance alliance = plugin.getAllianceCache().getById(de.erethon.bedrock.misc.NumberUtil.parseInt(key, -1));
                    if (alliance != null) {
                        kills.put(alliance, killsSection.getInt(key));
                    }
                }
            }
            ConfigurationSection scoresSection = config.getConfigurationSection("scores");
            if (scoresSection != null) {
                for (String key : scoresSection.getKeys(false)) {
                    Alliance alliance = plugin.getAllianceCache().getById(de.erethon.bedrock.misc.NumberUtil.parseInt(key, -1));
                    if (alliance != null) {
                        scores.put(alliance, scoresSection.getDouble(key));
                    }
                }
            }
            ConfigurationSection contributionsSection = config.getConfigurationSection("contributions");
            if (contributionsSection != null) {
                for (String factionId : contributionsSection.getKeys(false)) {
                    int parsedFactionId = de.erethon.bedrock.misc.NumberUtil.parseInt(factionId, -1);
                    ConfigurationSection factionSection = contributionsSection.getConfigurationSection(factionId);
                    if (parsedFactionId < 0 || factionSection == null) {
                        continue;
                    }
                    Map<String, Integer> stats = new HashMap<>();
                    for (String key : factionSection.getKeys(false)) {
                        stats.put(key, factionSection.getInt(key));
                    }
                    factionContributions.put(parsedFactionId, stats);
                }
            }
        });
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
        serialized.put("tier", getTier().name());
        serialized.put("waypointSpawn", waypointSpawn);
        serialized.put("operatingFaction", operatingFaction == null ? null : operatingFaction.getId());
        serialized.put("repairSupplies", repairSupplies);
        Map<String, Object> serializedUpgrades = new HashMap<>(upgrades.size());
        upgrades.forEach((upgrade, level) -> serializedUpgrades.put(upgrade.name(), level));
        Map<String, Object> serializedContributions = new HashMap<>(factionContributions.size());
        factionContributions.forEach((factionId, stats) -> serializedContributions.put(String.valueOf(factionId), new HashMap<>(stats)));
        serialized.put("upgrades", serializedUpgrades);
        serialized.put("contributions", serializedContributions);
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
        plugin.getDatabaseManager().saveRegionWarTracker(this);
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
        if (cooldownDate > System.currentTimeMillis()) {
            return;
        }
        double newScore = getScore(alliance) + score;
        scores.put(alliance, newScore);
        if (newScore >= captureCap) {
            alliance.temporaryOccupy(region);
        }
        plugin.getDatabaseManager().saveRegionWarTracker(this);
    }

    public @Nullable Alliance getLeader() {
        Alliance winner = null;
        double score = -1;
        double secondScore = 0;
        for (Alliance alliance : scores.keySet()) {
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
        plugin.getDatabaseManager().saveRegionWarTracker(this);
    }

    public int getRegionValue() {
        return regionValue + getTier().getDefaultPointValue() + getUpgradeLevel(WarObjectiveUpgrade.POINT_VALUE);
    }

    public void setRegionValue(int regionValue) {
        this.regionValue = regionValue;
        plugin.getDatabaseManager().saveRegionWarTracker(this);
    }

    public @NotNull WarObjectiveTier getTier() {
        if (tier == null) {
            tier = inferTier();
        }
        return tier;
    }

    public void setTier(@NotNull WarObjectiveTier tier) {
        this.tier = tier;
        if (captureCap == DEFAULT_CAPTURE_CAP) {
            captureCap = tier.getDefaultCaptureCap();
        }
        plugin.getDatabaseManager().saveRegionWarTracker(this);
    }

    private @NotNull WarObjectiveTier inferTier() {
        if (region.getType() == RegionType.CAPITAL) {
            return WarObjectiveTier.CAPITAL;
        }
        if (!region.getStructures(WarFortressStructure.class).isEmpty() || !region.getStructures(WarCastleStructure.class).isEmpty()) {
            return WarObjectiveTier.FORTRESS;
        }
        if (!region.getStructures(CrystalWarStructure.class).isEmpty()) {
            return WarObjectiveTier.TOWER;
        }
        return WarObjectiveTier.CAMP;
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

    public @Nullable Faction getOperatingFaction() {
        return operatingFaction;
    }

    public void setOperatingFaction(@Nullable Faction operatingFaction) {
        this.operatingFaction = operatingFaction;
        plugin.getDatabaseManager().saveRegionWarTracker(this);
    }

    public boolean canOperate(@NotNull Faction faction) {
        return region.getAlliance() != null && faction.getAlliance() == region.getAlliance();
    }

    public int getUpgradeLevel(@NotNull WarObjectiveUpgrade upgrade) {
        return upgrades.getOrDefault(upgrade, 0);
    }

    public boolean buyUpgrade(@NotNull Faction faction, @NotNull WarObjectiveUpgrade upgrade, @NotNull Player initiator) {
        if (operatingFaction != faction || !canOperate(faction)) {
            return false;
        }
        if (upgrade == WarObjectiveUpgrade.WAYPOINT && !getTier().supportsWaypoint()) {
            return false;
        }
        int nextLevel = getUpgradeLevel(upgrade) + 1;
        if (nextLevel > upgrade.getMaxLevel()) {
            return false;
        }
        double cost = getModifiedUpgradeCost(faction.getAlliance(), upgrade, nextLevel);
        if (!faction.getFAccount().canAfford(cost, FEconomy.TAX_CURRENCY)) {
            return false;
        }
        faction.getFAccount().withdraw(cost, FEconomy.TAX_CURRENCY, "War objective upgrade " + upgrade.name() + " for " + region.getName(), initiator.getUniqueId());
        upgrades.put(upgrade, nextLevel);
        addContribution(faction, "upgrades_bought", 1);
        plugin.getDatabaseManager().saveRegionWarTracker(this);
        return true;
    }

    public double getModifiedUpgradeCost(@Nullable Alliance alliance, @NotNull WarObjectiveUpgrade upgrade, int nextLevel) {
        double cost = upgrade.getCost(nextLevel);
        if (isLowestRankedAlliance(alliance)) {
            cost *= 0.85;
        }
        return cost;
    }

    public boolean isLowestRankedAlliance(@Nullable Alliance alliance) {
        if (alliance == null) {
            return false;
        }
        double ownScore = alliance.getWarScore();
        double lowest = Double.MAX_VALUE;
        double highest = Double.MIN_VALUE;
        for (Alliance current : plugin.getAllianceCache()) {
            lowest = Math.min(lowest, current.getWarScore());
            highest = Math.max(highest, current.getWarScore());
        }
        return highest > lowest && ownScore <= lowest;
    }

    public int getRepairSupplies() {
        return repairSupplies;
    }

    public void addRepairSupplies(int supplies) {
        repairSupplies += Math.max(0, supplies);
        plugin.getDatabaseManager().saveRegionWarTracker(this);
    }

    public boolean consumeRepairSupply() {
        if (repairSupplies <= 0) {
            return false;
        }
        repairSupplies--;
        plugin.getDatabaseManager().saveRegionWarTracker(this);
        return true;
    }

    public int consumeRepairSupplies(int supplies) {
        if (supplies <= 0 || repairSupplies <= 0) {
            return 0;
        }
        int consumed = Math.min(repairSupplies, supplies);
        repairSupplies -= consumed;
        plugin.getDatabaseManager().saveRegionWarTracker(this);
        return consumed;
    }

    public void clearCycleState() {
        setOperatingFaction(null);
        waypointSpawn = null;
        repairSupplies = 0;
        upgrades.clear();
        factionContributions.clear();
        reset(false);
        plugin.getDatabaseManager().saveRegionWarTracker(this);
    }

    public boolean hasRequiredCrystals() {
        return !region.getStructures(CrystalWarStructure.class).isEmpty();
    }

    public boolean isCaptureUnlocked() {
        Map<String, CrystalWarStructure> crystals = region.getStructures(CrystalWarStructure.class);
        return !crystals.isEmpty() && crystals.values().stream().allMatch(CrystalWarStructure::isDepleted);
    }

    public boolean isContested() {
        return region.getStructures(OccupyWarStructure.class).values().stream()
                .anyMatch(occupy -> occupy.getActiveAlliances().stream()
                        .anyMatch(alliance -> alliance != region.getAlliance()));
    }

    public double getHighestCaptureProgressPercent() {
        return region.getStructures(OccupyWarStructure.class).values().stream()
                .mapToDouble(occupy -> occupy.getOccupyDuration() <= 0 ? 0.0 : (occupy.getCurrentProgress() * 100.0) / occupy.getOccupyDuration())
                .max()
                .orElse(0.0);
    }

    public boolean isWaypointAvailable(@Nullable Alliance alliance) {
        if (alliance == null || region.getAlliance() != alliance) {
            return false;
        }
        if (!plugin.getCurrentWarPhase().isAllowPvP()) {
            return false;
        }
        if (!getTier().supportsWaypoint() || getUpgradeLevel(WarObjectiveUpgrade.WAYPOINT) <= 0 || waypointSpawn == null) {
            return false;
        }
        if (isCaptureUnlocked() || getHighestCaptureProgressPercent() >= WAYPOINT_CONTESTED_DISABLE_PERCENT) {
            return false;
        }
        return isSafeWaypointSpawn();
    }

    public boolean isSafeWaypointSpawn() {
        if (waypointSpawn == null || waypointSpawn.getWorld() == null) {
            return false;
        }
        return waypointSpawn.getBlock().isPassable()
                && waypointSpawn.clone().add(0, 1, 0).getBlock().isPassable()
                && !waypointSpawn.clone().subtract(0, 1, 0).getBlock().isPassable();
    }

    public @Nullable Location getWaypointSpawn() {
        return waypointSpawn == null ? null : waypointSpawn.clone();
    }

    public void setWaypointSpawn(@Nullable Location waypointSpawn) {
        this.waypointSpawn = waypointSpawn == null ? null : waypointSpawn.clone();
        plugin.getDatabaseManager().saveRegionWarTracker(this);
    }

    public @NotNull String getMapWarState(@Nullable Alliance viewerAlliance) {
        if (isCaptureUnlocked()) {
            return "crystal-down";
        }
        if (getHighestCaptureProgressPercent() >= WAYPOINT_CONTESTED_DISABLE_PERCENT || isContested()) {
            return "contested";
        }
        if (isWaypointAvailable(viewerAlliance)) {
            return "waypoint";
        }
        if (hasRequiredCrystals()) {
            return "crystal-up";
        }
        return "stable";
    }

    public void addContribution(@Nullable Faction faction, @NotNull String key, int amount) {
        if (faction == null || amount <= 0) {
            return;
        }
        factionContributions.computeIfAbsent(faction.getId(), ignored -> new HashMap<>())
                .merge(key, amount, Integer::sum);
        plugin.getDatabaseManager().saveRegionWarTracker(this);
    }

    public @NotNull Map<Integer, Map<String, Integer>> getFactionContributions() {
        return factionContributions;
    }
}
