package de.erethon.factions.economy;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.faction.Faction;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Malfrador
 */
public class FStorage {

    private Faction faction;
    private final Map<Resource, Integer> resources = new HashMap<>();
    private final Map<Resource, Integer> resourceLimits;

    public FStorage(@NotNull Faction faction) {
        this.faction = faction;
        resourceLimits = Factions.get().getFConfig().getDefaultResourceLimits();
    }

    public FStorage(@NotNull Faction faction, @NotNull ConfigurationSection section) {
        this.faction = faction;
        resourceLimits = Factions.get().getFConfig().getDefaultResourceLimits();
        for (String key : section.getKeys(false)) {
            Resource resource = Resource.getById(key);
            if (resource == null) {
                continue;
            }
            resources.put(resource, section.getInt(key));
        }
    }

    public boolean addResource(@NotNull Resource resource, int amount) {
        int current = resources.getOrDefault(resource, 0);
        int limit = resourceLimits.getOrDefault(resource, 0);
        int spaceToLimit = limit - current;
        if (spaceToLimit <= 0) {
            return false;
        }
        if (amount > spaceToLimit) {
            amount = spaceToLimit;
        }
        resources.put(resource, Math.min(current + amount, limit));
        MessageUtil.log("Storage: " + Math.min(current + amount, limit) + " of " + resource.getId() + " for " + faction.getName() + " (added " + amount + ")");
        return true;
    }

    public boolean removeResource(@NotNull Resource resource, int amount) {
        int current = resources.getOrDefault(resource, 0);
        if (current < amount) {
            return false;
        }
        resources.put(resource, current - amount);
        MessageUtil.log("Storage: " + (current - amount) + " of " + resource.getId() + " for " + faction.getName() + " (removed " + amount + ")");
        return true;
    }

    public boolean canAfford(@NotNull Resource resource, int amount) {
        return resources.getOrDefault(resource, 0) >= amount;
    }

    public int getResource(@NotNull Resource resource) {
        return resources.getOrDefault(resource, 0);
    }

    public int getResourceLimit(@NotNull Resource resource) {
        return resourceLimits.getOrDefault(resource, 0);
    }

    public void setResourceLimit(@NotNull Resource resource, int amount) {
        resourceLimits.put(resource, amount);
    }

    public void increaseResourceLimit(@NotNull Resource resource, int amount) {
        int current = resourceLimits.getOrDefault(resource, 0);
        resourceLimits.put(resource, current + amount);
    }

    public void decreaseResourceLimit(@NotNull Resource resource, int amount) {
        int current = resourceLimits.getOrDefault(resource, 0);
        resourceLimits.put(resource, current - amount);
    }

    public boolean isFull(@NotNull Resource resource) {
        return resources.getOrDefault(resource, 0) >= resourceLimits.getOrDefault(resource, 0);
    }

    public @NotNull ConfigurationSection save() {
        ConfigurationSection section = new YamlConfiguration();
        for (Resource resource : resources.keySet()) {
            section.set(resource.getId(), resources.get(resource));
        }
        return section;
    }

    public Faction getFaction() {
        return faction;
    }

    public boolean hasResource(Resource resource) {
        return resources.getOrDefault(resource, 0) > 0;
    }
}
