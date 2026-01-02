package de.erethon.factions.util;

import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.Map;

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

    public static Component getAllianceInfluenceBar(Map<Alliance, Double> distribution) {
        return getAllianceInfluenceBar(distribution, 50, '|');
    }

    public static Component getAllianceInfluenceBar(Map<Alliance, Double> distribution, int totalChars, char barChar) {
        Component bar = Component.text("");
        Map<Alliance, String> result = new HashMap<>();

        for (Map.Entry<Alliance, Double> entry : distribution.entrySet()) {
            StringBuilder sb = new StringBuilder();
            int charCount = (int) Math.round((entry.getValue() / 100) * totalChars);
            sb.append(String.valueOf(barChar).repeat(Math.max(0, charCount)));
            result.put(entry.getKey(), sb.toString());
        }
        for (Alliance alliance : distribution.keySet()) {
            bar = bar.append(Component.text(result.get(alliance), alliance.getColor()));
        }
        return bar;
    }
}
