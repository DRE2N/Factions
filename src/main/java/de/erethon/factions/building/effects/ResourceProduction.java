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

public class ResourceProduction extends BuildingEffect {

    private final Map<Resource, Integer> production = new HashMap<>();
    private final int interval; // In minutes

    private BukkitRunnable task;

    public ResourceProduction(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        for (String key : data.getConfigurationSection("production").getKeys(false)) {
            Resource resource = Resource.valueOf(key.toUpperCase());
            production.put(resource, data.getInt("production." + key));
        }
        interval = data.getInt("interval", 60);
    }

    @Override
    public void apply() {
        if (task != null) {
            task.cancel();
        }
        task = new BukkitRunnable() {
            @Override
            public void run() {
                produce();
            }
        };
        task.runTaskTimer(Factions.get(), 0, (long) interval * 20 * 60);
    }

    private void produce() {
        for (Map.Entry<Resource, Integer> entry : production.entrySet()) {
            faction.getStorage().addResource(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void remove() {
        task.cancel();
    }
}
