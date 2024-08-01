package de.erethon.factions.war;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.entity.Relation;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.NumberConversions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WarScore {

    private final Factions plugin = Factions.get();
    private static final int TICK_DURATION = (20 * 60) * 20; // 20 minutes, in ticks

    // Scoring
    private final HashMap<Alliance, Integer> totalScore = new HashMap<>();
    private final HashMap<Alliance, Integer> objectiveScore = new HashMap<>();
    private final HashMap<Alliance, Integer> playerKillScore = new HashMap<>();

    // Ticking
    private final HashMap<Alliance, Integer> potentialPointsNextTick = new HashMap<>();
    private final HashMap<Alliance, Set<Region>> currentOwnership = new HashMap<>();

    private BukkitRunnable tickTask;
    private BukkitRunnable displayTask;

    public WarScore(ConfigurationSection section) {
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Alliance alliance = plugin.getAllianceCache().getById(NumberConversions.toInt(key));
                if (alliance == null) {
                    continue;
                }
                totalScore.put(alliance, section.getInt("total." + key));
                objectiveScore.put(alliance, section.getInt("objective." + key));
                playerKillScore.put(alliance, section.getInt("playerKill." + key));
            }
        } else {
            potentialPointsNextTick.put(plugin.getAllianceCache().getById(0), 0);
            potentialPointsNextTick.put(plugin.getAllianceCache().getById(1), 0);
            potentialPointsNextTick.put(plugin.getAllianceCache().getById(2), 0);
            totalScore.put(plugin.getAllianceCache().getById(0), 0);
            totalScore.put(plugin.getAllianceCache().getById(1), 0);
            totalScore.put(plugin.getAllianceCache().getById(2), 0);
            objectiveScore.put(plugin.getAllianceCache().getById(0), 0);
            objectiveScore.put(plugin.getAllianceCache().getById(1), 0);
            objectiveScore.put(plugin.getAllianceCache().getById(2), 0);
            playerKillScore.put(plugin.getAllianceCache().getById(0), 0);
            playerKillScore.put(plugin.getAllianceCache().getById(1), 0);
            playerKillScore.put(plugin.getAllianceCache().getById(2), 0);
            currentOwnership.put(plugin.getAllianceCache().getById(0), new HashSet<>());
            currentOwnership.put(plugin.getAllianceCache().getById(1), new HashSet<>());
            currentOwnership.put(plugin.getAllianceCache().getById(2), new HashSet<>());
        }
        displayTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickDisplays();
            }
        };
        displayTask.runTaskTimer(plugin, 0, 20);
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickScoring();
            }
        };
        tickTask.runTaskTimer(plugin, 0, TICK_DURATION);
    }

    private void tickDisplays() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendPlayerListFooter(getTabFooter());
        }
    }

    private void tickScoring() {
        for (Alliance alliance : totalScore.keySet()) {
            int potential = potentialPointsNextTick.getOrDefault(alliance, 0);
            potentialPointsNextTick.put(alliance, potential + 1);
        }
    }

    public void regionCaptured(Alliance newOwner, Alliance oldOwner, Region region) {
        Set<Region> regions = currentOwnership.getOrDefault(newOwner, new HashSet<>());
        regions.add(region);
        currentOwnership.put(newOwner, regions);
        if (oldOwner != null) {
            regions = currentOwnership.getOrDefault(oldOwner, new HashSet<>());
            regions.remove(region);
            currentOwnership.put(oldOwner, regions);
        }
        FLogger.WAR.log("Region " + region.getName() + " captured by " + newOwner.getName() + " from " + oldOwner.getName());
    }

    public void add(Alliance alliance, int score, WarScoreType type) {
        switch (type) {
            case OBJECTIVE:
                objectiveScore.put(alliance, objectiveScore.getOrDefault(alliance, 0) + score);
                break;
            case PLAYER_KILL:
                playerKillScore.put(alliance, playerKillScore.getOrDefault(alliance, 0) + score);
                break;
        }
        totalScore.put(alliance, totalScore.getOrDefault(alliance, 0) + score);
        FLogger.WAR.log("Added " + score + " points to " + alliance.getName() + " for " + type.name());
    }

    public ConfigurationSection save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Alliance alliance : totalScore.keySet()) {
            config.set("total." + alliance.getId(), totalScore.get(alliance));
            config.set("objective." + alliance.getId(), objectiveScore.get(alliance));
            config.set("playerKill." + alliance.getId(), playerKillScore.get(alliance));
        }
        return config;
    }

    private Component getTabFooter() {
        Component footer = Component.text("\n");
        // Current victory points
        Component vpRed = Component.text(totalScore.get(War.ALLIANCE_RED), War.ALLIANCE_RED.getColor());
        Component vpGreen = Component.text(" " + totalScore.get(War.ALLIANCE_GREEN), War.ALLIANCE_GREEN.getColor());
        Component vpBlue = Component.text(" " + totalScore.get(War.ALLIANCE_BLUE), War.ALLIANCE_BLUE.getColor());
        Component vpIcon = Component.text("👑", NamedTextColor.GOLD);
        footer = footer.append(Component.translatable("factions.war.score.victoryPointsHeading"));
        footer = footer.append(Component.text("\n"));
        footer = footer.append(vpRed.append(vpIcon).append(Component.text(" | ", NamedTextColor.DARK_GRAY)).append(vpGreen).append(vpIcon).append(Component.text(" | ", NamedTextColor.DARK_GRAY)).append(vpBlue).append(vpIcon));
        footer = footer.append(Component.text("\n"));
        Component bar = Component.text("");
        // Line that shows the current objective distribution with |||||| colored in the alliance color
        Map<Alliance, Double> distribution = getPotentialPointsDistribution();
        Map<Alliance, String> result = new HashMap<>();
        char c = '|';
        int totalChars = 50;

        for (Map.Entry<Alliance, Double> entry : distribution.entrySet()) {
            StringBuilder sb = new StringBuilder();
            int charCount = (int) Math.round((entry.getValue() / 100) * totalChars);
            sb.append(String.valueOf(c).repeat(Math.max(0, charCount)));
            result.put(entry.getKey(), sb.toString());
        }
        for (Alliance alliance : distribution.keySet()) {
            bar = bar.append(Component.text(result.get(alliance), alliance.getColor()));
        }
        bar = bar.append(Component.text("\n")); // No newline() support for some reason

        // Potential points
        Component pointsRed = Component.text(" +" + potentialPointsNextTick.get(War.ALLIANCE_RED), War.ALLIANCE_RED.getColor());
        Component pointsGreen = Component.text(" +" + potentialPointsNextTick.get(War.ALLIANCE_GREEN), War.ALLIANCE_GREEN.getColor());
        Component pointsBlue = Component.text(" +" + potentialPointsNextTick.get(War.ALLIANCE_BLUE), War.ALLIANCE_BLUE.getColor());

        footer = footer.append(Component.translatable("factions.war.score.potentialHeading"));
        footer = footer.append(Component.text("\n"));
        footer = footer.append(bar);
        footer = footer.append(pointsRed.append(Component.text(" | ", NamedTextColor.DARK_GRAY)).append(pointsGreen).append(Component.text(" | ", NamedTextColor.DARK_GRAY)).append(pointsBlue));
        footer = footer.append(Component.text("\n"));
        return footer;

    }

    private Map<Alliance, Double> getPotentialPointsDistribution() {
        int totalPotentialPoints = potentialPointsNextTick.values().stream().mapToInt(Integer::intValue).sum();
        Map<Alliance, Double> distribution = new HashMap<>();

        for (Map.Entry<Alliance, Integer> entry : potentialPointsNextTick.entrySet()) {
            double percentage = (entry.getValue() / (double) totalPotentialPoints) * 100;
            distribution.put(entry.getKey(), percentage);
        }

        return distribution;
    }

}
