package de.erethon.factions.statistic;

import de.erethon.factions.util.FLogger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Fyreum
 */
public class FStatistics {

    protected static final Map<String, FStatInt> STATS = new HashMap<>();

    /* Statistics */

    public static final FStatInt TEST = new FStatInt("test");

    /* Serialization */

    public static void load(@NotNull File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        STATS.forEach((key, stat) -> stat.setValue(config.getInt(key)));
    }

    public static void save(@NotNull File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        STATS.forEach((key, stat) -> config.set(key, stat.getValue()));
        try {
            config.save(file);
        } catch (IOException e) {
            FLogger.ERROR.log("Couldn't save statistics: ");
            e.printStackTrace();
        }
    }

}
