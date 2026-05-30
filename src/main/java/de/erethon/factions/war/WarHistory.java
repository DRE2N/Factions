package de.erethon.factions.war;

import de.erethon.bedrock.config.EConfig;
import de.erethon.bedrock.misc.FileUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.util.FLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * @author Fyreum
 */
public class WarHistory {

    private final File folder;
    private final TreeSet<Entry> entries = new TreeSet<>();

    public WarHistory(@NotNull File folder) {
        this.folder = folder;
    }

    public @NotNull Entry storeEntry(long endDate, @NotNull Map<Integer, Double> allianceScores) {
        Entry entry = new Entry(endDate, allianceScores);
        entries.add(entry);
        return entry;
    }

    /* Serialization */

    public void load() {
        Factions.get().getDatabaseManager().loadWarHistoryInto(this);
        FLogger.INFO.log("Loaded " + entries.size() + " previous war entries.");
    }

    public void saveAll() {
        for (Entry entry : entries) {
            entry.saveData();
        }
    }

    /* Getters and setters */

    public @NotNull TreeSet<Entry> getEntries() {
        return entries;
    }

    public void addLoadedEntry(long endDate, @NotNull ConfigurationSection section) {
        entries.add(new Entry(endDate, section));
    }

    /* Classes */

    public class Entry extends EConfig implements Comparable<Entry> {

        public static int CONFIG_VERSION = 1;

        private long endDate;
        private Map<Integer, Double> allianceScores;

        public Entry(long endDate, @NotNull Map<Integer, Double> allianceScores) {
            super(new File(folder, endDate + ".yml"), CONFIG_VERSION);
            this.endDate = endDate;
            this.allianceScores = allianceScores;
        }

        public Entry(@NotNull File file) {
            super(file, CONFIG_VERSION);
            load();
        }

        public Entry(long endDate, @NotNull ConfigurationSection section) {
            super(new File(folder, endDate + ".yml"), CONFIG_VERSION);
            this.endDate = endDate;
            this.allianceScores = new HashMap<>();
            ConfigurationSection entriesSection = section.getConfigurationSection("allianceScores");
            if (entriesSection == null) {
                entriesSection = section.getConfigurationSection("entries");
            }
            if (entriesSection != null) {
                entriesSection.getValues(false).forEach((key, value) -> allianceScores.put(NumberConversions.toInt(key), NumberConversions.toDouble(value)));
            }
        }

        /* Serialization */

        @Override
        public void load() {
            this.endDate = config.getLong("endDate");
            this.allianceScores = new HashMap<>();
            ConfigurationSection entriesSection = config.getConfigurationSection("entries");
            if (entriesSection != null) {
                entriesSection.getValues(false).forEach((key, value) -> allianceScores.put(NumberConversions.toInt(key), NumberConversions.toDouble(value)));
            }
        }

        public void saveData() {
            config.set("endDate", endDate);
            config.set("allianceScores", allianceScores);
            Factions.get().getDatabaseManager().saveWarHistoryEntry(this, config.saveToString());
        }

        /* Getters */

        public long getEndDate() {
            return endDate;
        }

        public @NotNull Map<Integer, Double> getAllianceScores() {
            return allianceScores;
        }

        public int getWinner() {
            int winner = -1;
            double highestScore = -1;
            for (Map.Entry<Integer, Double> entry : allianceScores.entrySet()) {
                if (entry.getValue() > highestScore) {
                    winner = entry.getKey();
                    highestScore = entry.getValue();
                }
            }
            return winner;
        }

        @Override
        public int compareTo(@NotNull WarHistory.Entry o) {
            return Long.compare(endDate, o.endDate);
        }
    }
}
