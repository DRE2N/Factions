package de.erethon.factions.war;

import de.erethon.factions.util.WarMath;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author Fyreum
 */
public class RegionalScore {

    private int kills;
    private double totalScore;

    public RegionalScore(int kills, double totalScore) {
        this.kills = kills;
        this.totalScore = totalScore;
    }

    /* Serialization */

    public @NotNull Map<String, Object> serialize() {
        return Map.of("kills", kills, "totalScore", totalScore);
    }

    /* Getters and setters */

    public int getKills() {
        return kills;
    }

    public void addKill() {
        totalScore += WarMath.scoreForKills(++kills);
    }

    public double getTotalScore() {
        return totalScore;
    }

    public void addScore(double score) {
        totalScore += score;
    }
}
