package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.building.attributes.FactionResourceAttribute;
import de.erethon.factions.economy.resource.Resource;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ResourceProduction extends BuildingEffect {

    protected final Map<Resource, Integer> production = new HashMap<>();

    public ResourceProduction(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        for (String key : data.getConfigurationSection("production").getKeys(false)) {
            Resource resource = Resource.valueOf(key.toUpperCase());
            production.put(resource, data.getInt("production." + key));
        }
    }

    @Override
    public void onPayday() {
        produce();
    }

    protected void produce() {
        for (Map.Entry<Resource, Integer> entry : production.entrySet()) {
            FactionResourceAttribute attribute = faction.getOrCreateAttribute(
                    entry.getKey().name(),
                    FactionResourceAttribute.class,
                    () -> new FactionResourceAttribute(entry.getKey(), 0.0)
            );
            attribute.setBaseValue(attribute.getBaseValue() + entry.getValue());
        }
    }
}
