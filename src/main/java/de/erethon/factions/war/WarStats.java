package de.erethon.factions.war;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Fyreum
 */
public class WarStats {

    protected int kills, assists, deaths, killStreak, highestKillStreak;
    protected double damage;
    protected long capturingTime;

    public WarStats() {
    }

    public WarStats(@Nullable ConfigurationSection section) {
        if (section == null) {
            return;
        }
        this.kills = section.getInt("kills");
        this.assists = section.getInt("assists");
        this.deaths = section.getInt("deaths");
        this.killStreak = section.getInt("killStreak");
        this.highestKillStreak = section.getInt("highestKillStreak");
        this.damage = section.getDouble("damage");
        this.capturingTime = section.getLong("capturingTime");
    }

    /* Serialization */

    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put("kills", kills);
        serialized.put("assists", assists);
        serialized.put("deaths", deaths);
        serialized.put("killStreak", killStreak);
        serialized.put("highestKillStreak", highestKillStreak);
        serialized.put("damage", damage);
        serialized.put("capturingTime", capturingTime);
        return serialized;
    }

    /* Getters and setters */

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getAssists() {
        return assists;
    }

    public void setAssists(int assists) {
        this.assists = assists;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public double getKDRatio() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }

    public double getKDARatio() {
        return deaths == 0 ? kills + assists : (double) (kills + assists) / deaths;
    }

    public int getKillStreak() {
        return killStreak;
    }

    public void setKillStreak(int killStreak) {
        this.killStreak = killStreak;
    }

    public int getHighestKillStreak() {
        return highestKillStreak;
    }

    public void setHighestKillStreak(int highestKillStreak) {
        this.highestKillStreak = highestKillStreak;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public long getCapturingTime() {
        return capturingTime;
    }

    public void incrementCapturingTime(long amount) {
        capturingTime += amount;
    }

    public void setCapturingTime(long capturingTime) {
        this.capturingTime = capturingTime;
    }
}
