package de.erethon.factions.building.effects;

import de.erethon.factions.Factions;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.economy.resource.Resource;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ResourceConsumption extends BuildingEffect {

    private final Map<Resource, Integer> consumption = new HashMap<>();
    private final int interval; // In minutes
    private final boolean shouldDisableBuilding;

    private BukkitRunnable task;
    private boolean buildingIsDisabled = false;

    public ResourceConsumption(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        for (String key : data.getConfigurationSection("consumption").getKeys(false)) {
            Resource resource = Resource.valueOf(key.toUpperCase());
            consumption.put(resource, data.getInt("consumption." + key));
        }
        interval = data.getInt("interval", 60);
        shouldDisableBuilding = data.getBoolean("disableBuilding", true);
    }

    @Override
    public void apply() {
        if (task != null) {
            task.cancel();
        }
        task = new BukkitRunnable() {
            @Override
            public void run() {
                consume();
            }
        };
        task.runTaskTimer(Factions.get(), 0, (long) interval * 20 * 60);
    }

    private void consume() {
        boolean canAfford = true;
        for (HashMap.Entry<Resource, Integer> entry : consumption.entrySet()) {
            if (!faction.getStorage().canAfford(entry.getKey(), entry.getValue())) {
                if (shouldDisableBuilding && !buildingIsDisabled) {
                    disableOtherEffects();
                    buildingIsDisabled = true;
                    return;
                }
                canAfford = false;
            }
            faction.getStorage().removeResource(entry.getKey(), entry.getValue());
        }
        if (shouldDisableBuilding && buildingIsDisabled && canAfford) {
            enableOtherEffects();
            buildingIsDisabled = false;
        }
    }

    @Override
    public void remove() {
        task.cancel();
    }
}
