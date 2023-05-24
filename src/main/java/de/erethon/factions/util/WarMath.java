package de.erethon.factions.util;

import de.erethon.factions.Factions;

/**
 * @author Fyreum
 */
public class WarMath {

    public static double roundToFraction(double x, int fraction) {
        return (double) Math.round(x * fraction) / fraction;
    }

    public static double scoreEfficiencyForKills(int kills) {
        return Math.max(-Math.pow(Math.E, 0.035 * (kills - 1)) + 2, 0);
    }

    public static double scoreForKills(int kills) {
        return scoreForKills(kills, Factions.get().getFConfig().getWarScorePerKill());
    }

    public static double scoreForKills(int kills, double scorePerKill) {
        return roundToFraction(scoreEfficiencyForKills(kills) * scorePerKill, 2);
    }
}
