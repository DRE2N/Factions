package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.building.attributes.FactionResourceAttribute;
import de.erethon.factions.economy.resource.Resource;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class EntityDependentResourceProduction extends ResourceProduction {

    private final Map<EntityType, Double> productionPerEntity = new HashMap<>();
    private final int maximumCountedEntities;
    private final int range;

    public EntityDependentResourceProduction(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        for (String key : data.getConfigurationSection("entityModifiers").getKeys(false)) {
            EntityType entityType = EntityType.valueOf(key.toUpperCase());
            productionPerEntity.put(entityType, data.getDouble("production." + key));
        }
        maximumCountedEntities = data.getInt("maximumCountedEntities", 50);
        range = data.getInt("range", 10);
    }

    @Override
    public void onPayday() {
        produce();
    }

    protected void produce() {
        Map<Resource, Double> toBeAdded = new HashMap<>();
        int entityCount = 0;
        for (Map.Entry<EntityType, Double> entry : productionPerEntity.entrySet()) {
            int count = site.getInteractive().getNearbyEntitiesByType(entry.getKey().getEntityClass(), range).size();
            entityCount += count;
            if (entityCount > maximumCountedEntities) {
                break;
            }
            double amount = count * entry.getValue();
            for (Map.Entry<Resource, Integer> material : production.entrySet()) {
                toBeAdded.put(material.getKey(), amount);
            }
        }
        for (Map.Entry<Resource, Double> entry : toBeAdded.entrySet()) {
            FactionResourceAttribute attribute = faction.getOrCreateAttribute(
                    entry.getKey().name(),
                    FactionResourceAttribute.class,
                    () -> new FactionResourceAttribute(entry.getKey(), 0.0)
            );
            attribute.setBaseValue(attribute.getBaseValue() + entry.getValue());
        }
    }
}
