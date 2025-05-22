package de.erethon.factions.building;

import de.erethon.factions.util.FLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * @author Malfrador
 */
public class BuildingEffectData {

    private final String id;
    private final ConfigurationSection config;
    private Component displayName;
    private String className;
    private Class<? extends BuildingEffect> clazz;
    public boolean isTickable = false;

    public BuildingEffectData(ConfigurationSection section, String effectKey) {
        this.id = effectKey.replaceAll("_.*$", "");
        this.className = "de.erethon.factions.building.effects." + this.id;
        this.config = section;
        load();
        FLogger.BUILDING.log("Loaded building effect " + this.id + " (from key: " + effectKey + ") with keys: " + config.getKeys(false).size());
    }

    public BuildingEffect newEffect(BuildSite owner) {
        if (owner.getFaction() == null) {
            FLogger.BUILDING.log("Cannot create effect " + id + " for owner " + owner.getBuilding().getId() + " with no faction.");
            return null;
        }
        if (clazz == null) {
            FLogger.ERROR.log("Cannot create building effect " + id + " because its class (" + className + ") could not be loaded.");
            return null;
        }
        BuildingEffect effect = null;
        try {
            effect = clazz.getDeclaredConstructor(BuildingEffectData.class, BuildSite.class).newInstance(this, owner);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            FLogger.ERROR.log("Could not create building effect instance for " + id + " (class: " + className + ")");
            if (e instanceof InvocationTargetException && e.getCause() != null) {
                e.getCause().printStackTrace();
            } else {
                e.printStackTrace();
            }
            return null;
        }
        owner.getFaction().getBuildingEffects().add(effect);
        if (isTickable) {
            owner.getFaction().getTickingBuildingEffects().add(effect);
        }
        return effect;
    }

    public @NotNull String getId() {
        return id;
    }

    public ConfigurationSection getConfig() {
        return config;
    }

    public String getString(String path) {
        return config.getString(path);
    }

    public String getString(String path, String def) {
        return config.getString(path, def);
    }

    public int getInt(String path) {
        return config.getInt(path);
    }

    public int getInt(String path, int def) {
        return config.getInt(path, def);
    }

    public double getDouble(String path) {
        return config.getDouble(path);
    }

    public double getDouble(String path, double def) {
        return config.getDouble(path, def);
    }

    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }

    public boolean getBoolean(String path, boolean def) {
        return config.getBoolean(path, def);
    }

    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }

    public ConfigurationSection getConfigurationSection(String path) {
        return config.getConfigurationSection(path);
    }

    public boolean contains(String path) {
        return config.contains(path);
    }

    public @NotNull Component getDisplayName() {
        return displayName != null ? displayName : Component.text(id);
    }

    private void load() {
        displayName = MiniMessage.miniMessage().deserialize(config.getString("displayName", this.id)); // Default to ID if no displayName
        isTickable = config.getBoolean("isTickable", false);
        try {
            clazz = (Class<? extends BuildingEffect>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            FLogger.BUILDING.log("Could not find class for building effect: " + className + " (derived from ID: " + id + "). Check effect key and class naming conventions. Effect will not work.");
        } catch (ClassCastException e) {
            FLogger.BUILDING.log("Class " + className + " (derived from ID: " + id + ") does not extend BuildingEffect. Effect will not work.");
        }
    }

    @Override
    public String toString() {
        StringBuilder sectionKeys = new StringBuilder();
        for (String key : config.getKeys(false)) {
            sectionKeys.append(key).append(": ").append(config.get(key)).append(", ");
        }
        if (sectionKeys.length() > 1) {
            sectionKeys.setLength(sectionKeys.length() - 2);
        }
        return "BuildingEffectData{" +
                "id='" + id + '\'' +
                ", className='" + className + '\'' +
                ", clazz=" + (clazz != null ? clazz.getSimpleName() : "null (load failed?)") +
                ", isTickable=" + isTickable +
                ", configData={" + sectionKeys + "}" +
                '}';
    }
}