package de.erethon.factions.data;

import de.erethon.aergia.util.TickUtil;
import de.erethon.bedrock.config.EConfig;
import de.erethon.factions.Factions;
import de.erethon.factions.building.Building;
import de.erethon.factions.economy.FactionLevel;
import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.economy.resource.Resource;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Read-only configuration for general plugin properties.
 *
 * @author Fyreum
 */
public class FConfig extends EConfig {

    public static final int CONFIG_VERSION = 1;

    private final Factions plugin = Factions.get();

    /* General */
    private List<String> excludedWorlds = new ArrayList<>();
    private String language = "german";
    private int maximumAutomatedChunkManagerRadius = 5;
    private int maximumDescriptionChars = 256;
    private int maximumLongNameChars = 128;
    private int maximumNameChars = 64;
    private int maximumShortNameChars = 32;

    /* Alliance */
    private long allianceJoinCooldown = TimeUnit.DAYS.toMillis(30);

    /* Faction */
    private long factionJoinCooldown = TimeUnit.HOURS.toMillis(1);
    private List<String> forbiddenNames = new ArrayList<>();
    private long inactiveAdminKickDuration = TimeUnit.DAYS.toMicros(40);
    private long inactiveKickDuration = TimeUnit.DAYS.toMicros(30);
    private long inactiveKickTimer = TickUtil.HOUR;

    /* Economy */
    private Map<FactionLevel, Double> maximumFactionDebts = new HashMap<>();
    private double regionPriceBase = 250.0;
    private double regionPricePerChunk = 0.3;
    private double regionPricePerRegion = 50.0;
    private double regionPricePerRegionFactor = 0.25;
    private double regionPriceTaxRate = 0.02;
    private double regionPriceTotalMultiplier = 1.0;
    private Map<Resource, Integer> defaultResourceLimits = new HashMap<>();
    private Map<FactionLevel, HashMap<PopulationLevel, Integer>> requiredPopulation = new HashMap<>();
    private Map<FactionLevel, Set<Building>> requiredBuildings = new HashMap<>();
    private double taxConversionRate = 0.95;

    /* War */
    private double warScorePerKill = 5.0;
    private int warCapturedRegionsPerBattle = 5;

    public FConfig(File file) {
        super(file, CONFIG_VERSION);
        initialize();
        load();
    }

    @Override
    public void initialize() {
        initValue("excludedWorlds", excludedWorlds);
        initValue("language", language);
        initValue("maximumAutomatedChunkManagerRadius", maximumAutomatedChunkManagerRadius);
        initValue("maximumDescriptionChars", maximumDescriptionChars);
        initValue("maximumNameChars", maximumNameChars);
        initValue("maximumShortNameChars", maximumShortNameChars);
        initValue("allianceJoinCooldown", allianceJoinCooldown);
        initValue("factionJoinCooldown", factionJoinCooldown);
        initValue("forbiddenNames", forbiddenNames);
        initValue("inactiveAdminKickDuration", inactiveAdminKickDuration);
        initValue("inactiveKickDuration", inactiveKickDuration);
        initValue("inactiveKickTimer", inactiveKickTimer);
        initValue("regionPrice.base", regionPriceBase);
        initValue("regionPrice.perChunk", regionPricePerChunk);
        initValue("regionPrice.perRegion", regionPricePerRegion);
        initValue("regionPrice.perRegionFactor", regionPricePerRegionFactor);
        initValue("regionPrice.taxRate", regionPriceTaxRate);
        initValue("regionPrice.totalMultiplier", regionPriceTotalMultiplier);
        initValue("taxConversionRate", taxConversionRate);
        initValue("war.scorePerKill", warScorePerKill);
        initValue("war.capturedRegionsPerBattle", warCapturedRegionsPerBattle);
        for (Resource resource : Resource.values()) {
            initValue("defaultResourceLimits." + resource.name(), 512);
        }
        save();
    }

    public void lateInit() { // Config values that depend on FConfig already being initialized
        for (FactionLevel factionLevel : FactionLevel.values()) {
            for (PopulationLevel populationLevel : PopulationLevel.values()) {
                initValue("requiredPopulation." + factionLevel.name() + "." + populationLevel.name(), 0);
            }
        }
    }

    @Override
    public void load() {
        excludedWorlds = getStringList("excludedWorlds", excludedWorlds);
        language = config.getString("language", language);
        maximumAutomatedChunkManagerRadius = Math.max(config.getInt("maximumAutomatedChunkManagerRadius", maximumAutomatedChunkManagerRadius), 1);
        maximumDescriptionChars = config.getInt("maximumDescriptionChars", maximumDescriptionChars);
        maximumLongNameChars = config.getInt("maximumLongNameChars", maximumLongNameChars);
        maximumNameChars = config.getInt("maximumNameChars", maximumNameChars);
        maximumShortNameChars = config.getInt("maximumShortNameChars", maximumShortNameChars);
        allianceJoinCooldown = config.getLong("allianceJoinCooldown", allianceJoinCooldown);
        factionJoinCooldown = config.getLong("factionJoinCooldown", factionJoinCooldown);
        forbiddenNames = getStringList("forbiddenNames", forbiddenNames);
        inactiveAdminKickDuration = config.getLong("inactiveAdminKickDuration", inactiveAdminKickDuration);
        inactiveKickDuration = config.getLong("inactiveKickDuration", inactiveKickDuration);
        inactiveKickTimer = config.getLong("inactiveKickTimer", inactiveKickTimer);
        regionPriceBase = config.getDouble("regionPrice.base", regionPriceBase);
        regionPricePerChunk = config.getDouble("regionPrice.perChunk", regionPricePerChunk);
        regionPricePerRegion = config.getDouble("regionPrice.perRegion", regionPricePerRegion);
        regionPricePerRegionFactor = config.getDouble("regionPrice.perRegionFactor", regionPricePerRegionFactor);
        regionPriceTaxRate = config.getDouble("regionPrice.taxRate", regionPriceTaxRate);
        regionPriceTotalMultiplier = config.getDouble("regionPrice.totalMultiplier", regionPriceTotalMultiplier);
        taxConversionRate = config.getDouble("taxConversionRate", taxConversionRate);
        warScorePerKill = config.getDouble("war.scorePerKill", warScorePerKill);
        warCapturedRegionsPerBattle = config.getInt("war.capturedRegionsPerBattle", warCapturedRegionsPerBattle);
        defaultResourceLimits = new HashMap<>();
        for (Resource resource : Resource.values()) {
            defaultResourceLimits.put(resource, config.getInt("defaultResourceLimits." + resource.name(), 512));
        }
    }

    public void lateLoad() { // Config values that depend on FConfig already being loaded
        maximumFactionDebts = new HashMap<>();
        for (FactionLevel factionLevel : FactionLevel.values()) {
            maximumFactionDebts.put(factionLevel, config.getDouble("maximumFactionDebt." + factionLevel.name(), 1000));
        }
        requiredPopulation = new HashMap<>();
        for (FactionLevel factionLevel : FactionLevel.values()) {
            HashMap<PopulationLevel, Integer> populationLevelIntegerHashMap = new HashMap<>();
            for (PopulationLevel populationLevel : PopulationLevel.values()) {
                populationLevelIntegerHashMap.put(populationLevel, config.getInt("requiredPopulation." + factionLevel.name() + "." + populationLevel.name(), 0));
            }
            requiredPopulation.put(factionLevel, populationLevelIntegerHashMap);
        }
        requiredBuildings = new HashMap<>();
        for (FactionLevel factionLevel : FactionLevel.values()) {
            Set<Building> buildings = new HashSet<>();
            for (String s : config.getStringList("requiredBuildings." + factionLevel.name())) {
                buildings.add(plugin.getBuildingManager().getById(s));
            }
            requiredBuildings.put(factionLevel, buildings);

        }
    }

    /* Getters */

    public @NotNull List<String> getExcludedWorlds() {
        return excludedWorlds;
    }

    public boolean isExcludedWorld(@NotNull World world) {
        for (String excluded : excludedWorlds) {
            if (world.getName().matches(excluded)) {
                return true;
            }
        }
        return false;
    }

    public @NotNull String getLanguage() {
        return language;
    }

    public int getMaximumAutomatedChunkManagerRadius() {
        return maximumAutomatedChunkManagerRadius;
    }

    public int getMaximumDescriptionChars() {
        return maximumDescriptionChars;
    }

    public int getMaximumLongNameChars() {
        return maximumLongNameChars;
    }

    public int getMaximumNameChars() {
        return maximumNameChars;
    }

    public int getMaximumShortNameChars() {
        return maximumShortNameChars;
    }

    public long getAllianceJoinCooldown() {
        return allianceJoinCooldown;
    }

    public long getFactionJoinCooldown() {
        return factionJoinCooldown;
    }

    /**
     * @return a List of regex faction names that are not allowed in tags and long tags.
     */
    public @NotNull List<String> getForbiddenNames() {
        return forbiddenNames;
    }

    /**
     * @param name the name to check
     * @return if the name matches a string in the list of forbidden faction names
     */
    public boolean isNameForbidden(@NotNull String name) {
        for (String regex : forbiddenNames) {
            if (name.matches(regex)) {
                return true;
            }
        }
        return false;
    }

    public long getInactiveAdminKickDuration() {
        return inactiveAdminKickDuration;
    }

    public long getInactiveKickDuration() {
        return inactiveKickDuration;
    }

    public long getInactiveKickTimer() {
        return inactiveKickTimer;
    }

    public @NotNull Map<FactionLevel, Double> getMaximumFactionDebts() {
        return maximumFactionDebts;
    }

    public double getRegionPriceBase() {
        return regionPriceBase;
    }

    public double getRegionPricePerChunk() {
        return regionPricePerChunk;
    }

    public double getRegionPricePerRegion() {
        return regionPricePerRegion;
    }

    public double getRegionPricePerRegionFactor() {
        return regionPricePerRegionFactor;
    }

    public double getRegionPriceTaxRate() {
        return regionPriceTaxRate;
    }

    public double getRegionPriceTotalMultiplier() {
        return regionPriceTotalMultiplier;
    }

    public @NotNull Map<Resource, Integer> getDefaultResourceLimits() {
        return defaultResourceLimits;
    }

    public @Nullable Map<PopulationLevel, Integer> getRequiredPopulation(@NotNull FactionLevel level) {
        return requiredPopulation.get(level);
    }

    public double getTaxConversionRate() {
        return taxConversionRate;
    }

    public double getWarScorePerKill() {
        return warScorePerKill;
    }

    public int getWarCapturedRegionsPerBattle() {
        return warCapturedRegionsPerBattle;
    }

    public @Nullable Set<Building> getRequiredBuildings(@NotNull FactionLevel factionLevel) {
        return requiredBuildings.get(factionLevel);
    }
}
