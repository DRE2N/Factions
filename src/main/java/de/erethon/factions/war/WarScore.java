package de.erethon.factions.war;

import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
    private final Component HEADING = MiniMessage.miniMessage().deserialize("<gradient:red:dark_red><st>       </st></gradient><dark_gray>]<gray><st> </st> <#ad1c11>⚔</#ad1c11> <st> </st><dark_gray>[<gradient:dark_red:red><st>       </st></gradient>");

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
        // Always initialize all three alliances first
        potentialPointsNextTick.put(War.ALLIANCE_RED, 0);
        potentialPointsNextTick.put(War.ALLIANCE_GREEN, 0);
        potentialPointsNextTick.put(War.ALLIANCE_BLUE, 0);
        totalScore.put(War.ALLIANCE_RED, 0);
        totalScore.put(War.ALLIANCE_GREEN, 0);
        totalScore.put(War.ALLIANCE_BLUE, 0);
        objectiveScore.put(War.ALLIANCE_RED, 0);
        objectiveScore.put(War.ALLIANCE_GREEN, 0);
        objectiveScore.put(War.ALLIANCE_BLUE, 0);
        playerKillScore.put(War.ALLIANCE_RED, 0);
        playerKillScore.put(War.ALLIANCE_GREEN, 0);
        playerKillScore.put(War.ALLIANCE_BLUE, 0);
        currentOwnership.put(War.ALLIANCE_RED, new HashSet<>());
        currentOwnership.put(War.ALLIANCE_GREEN, new HashSet<>());
        currentOwnership.put(War.ALLIANCE_BLUE, new HashSet<>());

        // If we have saved scores, load them on top
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
        }
        displayTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickDisplays();
            }
        };
        displayTask.runTaskTimer(plugin, 0, 60);
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
        Component footer = HEADING;
        footer = footer.append(Component.text("\n")); // No newline() support in the tab footer for some reason
        // Current victory points
        Component vpRed = Component.text(totalScore.get(War.ALLIANCE_RED), War.ALLIANCE_RED.getColor());
        Component vpGreen = Component.text(" " + totalScore.get(War.ALLIANCE_GREEN), War.ALLIANCE_GREEN.getColor());
        Component vpBlue = Component.text(" " + totalScore.get(War.ALLIANCE_BLUE), War.ALLIANCE_BLUE.getColor());
        Component vpIcon = Component.text("👑", NamedTextColor.GOLD);
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
        bar = bar.append(Component.text("\n"));

        // Potential points
        Component pointsRed = Component.text(" +" + potentialPointsNextTick.get(War.ALLIANCE_RED), War.ALLIANCE_RED.getColor());
        Component pointsGreen = Component.text(" +" + potentialPointsNextTick.get(War.ALLIANCE_GREEN), War.ALLIANCE_GREEN.getColor());
        Component pointsBlue = Component.text(" +" + potentialPointsNextTick.get(War.ALLIANCE_BLUE), War.ALLIANCE_BLUE.getColor());

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
