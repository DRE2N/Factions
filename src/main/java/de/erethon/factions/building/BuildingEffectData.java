package de.erethon.factions.building;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.util.FLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Malfrador
 */
public class BuildingEffectData extends MemorySection {

    private final String id;
    private Component displayName;
    private String className;
    private Class<? extends BuildingEffect> clazz;
    public boolean isTickable = false;

    public BuildingEffectData(ConfigurationSection section, String path) {
        super(section, path);
        id = path.replace("effects.", "").replaceAll("_.*$", ""); // Quick fix to allow for multiple effects with the same id
        className = "de.erethon.factions.building.effects." + id;
        load();
        FLogger.BUILDING.log("Loaded building effect " + id);
    }

    public BuildingEffect newEffect(BuildSite owner) {
        if (owner.getFaction() == null) {
            return null;
        }
        BuildingEffect effect = null;
        try {
            effect = clazz.getDeclaredConstructor(BuildingEffectData.class, BuildSite.class).newInstance(this, owner);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            FLogger.ERROR.log("Could not create building effect " + id);
            e.printStackTrace();
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

    public @NotNull Component getDisplayName() {
        return displayName;
    }

    private void load() {
        displayName = MiniMessage.miniMessage().deserialize(getString("displayName", "no name"));
        isTickable = getBoolean("isTickable", false);
        try {
            clazz = (Class<? extends BuildingEffect>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            FLogger.BUILDING.log("Could not find class " + className);
        }
    }

    @Override
    public String toString() {
        return "BuildingEffectData{id='" + id + '\'' +
                ", className='" + className + '\'' +
                ", clazz=" + clazz +
                ", isTickable=" + isTickable +
                '}';
    }
}