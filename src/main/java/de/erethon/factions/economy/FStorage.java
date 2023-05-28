package de.erethon.factions.economy;

import de.erethon.factions.Factions;
import de.erethon.factions.data.FConfig;
import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.faction.Faction;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;

public class FStorage {

    private Faction faction;
    private final HashMap<Resource, Integer> resources = new HashMap<>();
    private final HashMap<Resource, Integer> resourceLimits;

    public FStorage(Faction faction) {
        this.faction = faction;
        resourceLimits = Factions.get().getFConfig().getDefaultResourceLimits();
    }

    public FStorage(Faction faction, ConfigurationSection section) {
        this.faction = faction;
        resourceLimits = Factions.get().getFConfig().getDefaultResourceLimits();
        for (String key : section.getKeys(false)) {
            Resource resource = Resource.getByID(key);
            if (resource == null) {
                continue;
            }
            resources.put(resource, section.getInt(key));
        }
    }

    public boolean addResource(Resource resource, int amount) {
        int current = resources.getOrDefault(resource, 0);
        int limit = resourceLimits.getOrDefault(resource, 0);
        if (current + amount > limit) {
            return false;
        }
        resources.put(resource, current + amount);
        return true;
    }

    public boolean removeResource(Resource resource, int amount) {
        int current = resources.getOrDefault(resource, 0);
        if (current - amount < 0) {
            return false;
        }
        resources.put(resource, current - amount);
        return true;
    }

    public boolean canAfford(Resource resource, int amount) {
        return resources.getOrDefault(resource, 0) >= amount;
    }

    public int getResource(Resource resource) {
        return resources.getOrDefault(resource, 0);
    }

    public void setResourceLimit(Resource resource, int amount) {
        resourceLimits.put(resource, amount);
    }

    public void increaseResourceLimit(Resource resource, int amount) {
        int current = resourceLimits.getOrDefault(resource, 0);
        resourceLimits.put(resource, current + amount);
    }

    public boolean isFull(Resource resource) {
        return resources.getOrDefault(resource, 0) >= resourceLimits.getOrDefault(resource, 0);
    }

    public ConfigurationSection save() {
        ConfigurationSection section = new YamlConfiguration();
        for (Resource resource : resources.keySet()) {
            section.set(resource.getID(), resources.get(resource));
        }
        return section;
    }
}
