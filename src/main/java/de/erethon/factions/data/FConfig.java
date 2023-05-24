package de.erethon.factions.data;

import de.erethon.aergia.util.TickUtil;
import de.erethon.bedrock.config.EConfig;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Read-only configuration for general plugin properties.
 *
 * @author Fyreum
 */
public class FConfig extends EConfig {

    public static final int CONFIG_VERSION = 1;

    /* General */
    private List<String> excludedWorlds = new ArrayList<>();
    private String language = "german";

    /* Alliance */
    private long allianceJoinCooldown = TimeUnit.DAYS.toMillis(30);

    /* Faction */
    private long factionJoinCooldown = TimeUnit.HOURS.toMillis(1);
    private List<String> forbiddenNames = new ArrayList<>();
    private long inactiveAdminKickDuration = TimeUnit.DAYS.toMicros(40);
    private long inactiveKickDuration = TimeUnit.DAYS.toMicros(30);
    private long inactiveKickTimer = TickUtil.HOUR;

    /* Economy */
    private double regionPriceBase = 250.0;
    private double regionPricePerChunk = 0.3;
    private double regionPricePerRegion = 50.0;
    private double regionPricePerRegionFactor = 0.25;
    private double regionPriceTotalMultiplier = 1.0;

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
        initValue("regionPrice.totalMultiplier", regionPriceTotalMultiplier);
        initValue("war.scorePerKill", warScorePerKill);
        initValue("war.capturedRegionsPerBattle", warCapturedRegionsPerBattle);
        save();
    }

    @Override
    public void load() {
        excludedWorlds = getStringList("excludedWorlds", excludedWorlds);
        language = config.getString("language", language);
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
        regionPriceTotalMultiplier = config.getDouble("regionPrice.totalMultiplier", regionPriceTotalMultiplier);
        warScorePerKill = config.getDouble("war.scorePerKill", warScorePerKill);
        warCapturedRegionsPerBattle = config.getInt("war.capturedRegionsPerBattle", warCapturedRegionsPerBattle);
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

    public double getRegionPriceTotalMultiplier() {
        return regionPriceTotalMultiplier;
    }

    public double getWarScorePerKill() {
        return warScorePerKill;
    }

    public int getWarCapturedRegionsPerBattle() {
        return warCapturedRegionsPerBattle;
    }
}
