package de.erethon.factions.war.objective;

import de.erethon.bedrock.config.EConfig;
import de.erethon.factions.util.FLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Fyreum
 */
public class WarObjectiveManager extends EConfig {

    public static final int CONFIG_VERSION = 1;

    private final Map<String, WarObjective> objectives = new HashMap<>();

    public WarObjectiveManager(@NotNull File file) {
        super(file, CONFIG_VERSION);
    }

    @Override
    public void load() {
        ConfigurationSection section = config.getConfigurationSection("objectives");
        if (section == null) {
            FLogger.WAR.log("No war objectives found.");
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection objectiveSection = section.getConfigurationSection(key);
            if (objectiveSection == null) {
                FLogger.ERROR.log("Section for war objective '" + key + "' not found.");
                continue;
            }
            try {
                WarObjective objective = WarObjective.deserialize(objectiveSection);
                objectives.put(objective.getName(), objective);
            } catch (IllegalArgumentException e) {
                FLogger.ERROR.log("Couldn't load war objective: ");
                e.printStackTrace();
            }
        }
        FLogger.INFO.log("Loaded " + objectives.size() + " war objectives");
    }

    public <T extends WarObjective> @NotNull T instantiateObjective(@NotNull String name, @NotNull Function<@NotNull ConfigurationSection, @NotNull T> builder) {
        ConfigurationSection section = config.getConfigurationSection("objectives");
        if (section == null) {
            section = config.createSection("objectives");
        }
        ConfigurationSection objectiveSection = section.getConfigurationSection(name);
        if (objectiveSection == null) {
            objectiveSection = section.createSection(name);
        }
        T objective = builder.apply(objectiveSection);
        assert name.equals(objective.getName()) : "Names do not match";
        objectives.put(name, objective);
        return objective;
    }

    public void activateAll() {
        objectives.forEach((s, obj) -> obj.activate());
    }

    public void deactivateAll() {
        objectives.forEach((s, obj) -> obj.deactivate());
    }

    public void saveAll() {
        Map<String, Object> serialized = new HashMap<>(objectives.size());
        for (WarObjective objective : objectives.values()) {
            serialized.put(objective.getName(), objective.serialize());
        }
        config.set("objectives", serialized);
        save();
    }

    /* Getters and setters */

    public @Nullable WarObjective getByEntity(@NotNull Entity entity) {
        for (WarObjective objective : objectives.values()) {
            if (objective instanceof CrystalWarObjective crystal && crystal.getCrystal() == entity) {
                return crystal;
            }
        }
        return null;
    }

    public @NotNull Map<String, WarObjective> getObjectives() {
        return objectives;
    }

    public <T extends WarObjective> @NotNull Map<String, T> getObjectives(@NotNull Class<T> type) {
        Map<String, T> filtered = new HashMap<>();
        objectives.forEach((name, objective) -> {
            if (type.isInstance(objective)) {
                filtered.put(name, (T) objective);
            }
        });
        return filtered;
    }

    public @Nullable WarObjective getObjective(@NotNull String name) {
        return objectives.get(name);
    }

    public <T extends WarObjective> @Nullable T getObjective(@NotNull String name, @NotNull Class<T> type) {
        WarObjective objective = objectives.get(name);
        return type.isInstance(objective) ? (T) objective : null;
    }

    public void addObjective(@NotNull WarObjective objective) {
        objectives.put(objective.getName(), objective);
    }

    public void removeObjective(@NotNull WarObjective objective) {
        removeObjective(objective.getName());
    }

    public void removeObjective(@NotNull String name) {
        objectives.remove(name);
    }
}
