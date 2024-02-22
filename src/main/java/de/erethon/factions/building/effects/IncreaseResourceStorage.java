package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.economy.resource.Resource;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class IncreaseResourceStorage extends BuildingEffect {

    private final HashMap<Resource, Integer> storageIncrease = new HashMap<>();

    public IncreaseResourceStorage(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        for (String key : data.getConfigurationSection("storageIncrease").getKeys(false)) {
            Resource resource = Resource.valueOf(key.toUpperCase());
            storageIncrease.put(resource, data.getInt("storageIncrease." + key));
        }
    }

    @Override
    public void apply() {
        for (HashMap.Entry<Resource, Integer> entry : storageIncrease.entrySet()) {
            faction.getStorage().increaseResourceLimit(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void remove() {
        for (HashMap.Entry<Resource, Integer> entry : storageIncrease.entrySet()) {
            faction.getStorage().decreaseResourceLimit(entry.getKey(), entry.getValue());
        }
    }
}
