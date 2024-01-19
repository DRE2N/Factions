package de.erethon.factions.building;

import de.erethon.factions.util.FLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * @author Malfrador
 */
public class BuildingEffectData extends YamlConfiguration {

    private final String id;
    private Component displayName;
    public boolean isTickable = false;

    public BuildingEffectData(File configFile) {
        try {
            super.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            FLogger.BUILDING.log("Could not load building effect " + configFile.getName() + ": " + e.getMessage());
        }
        id = configFile.getName().replace(".yml", "");
        load();
    }

    public BuildingEffect newEffect(BuildSite owner) {
        if (owner.getFaction() == null) {
            return null;
        }
        BuildingEffect effect = new BuildingEffect(this, owner);
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

    public void load() {
        displayName = MiniMessage.miniMessage().deserialize(getString("displayName", "no name"));
        isTickable = getBoolean("isTickable", false);
    }
}