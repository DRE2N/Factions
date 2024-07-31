package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.building.attributes.FactionResourceAttribute;
import de.erethon.factions.economy.resource.Resource;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class BlockDependentResourceProduction extends ResourceProduction {

    private final Map<Material, Double> productionPerBlock = new HashMap<>();
    private final int maximumCountedBlocks;

    public BlockDependentResourceProduction(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        for (String key : data.getConfigurationSection("blockModifiers").getKeys(false)) {
            Material material = Material.valueOf(key.toUpperCase());
            productionPerBlock.put(material, data.getDouble("production." + key));
        }
        maximumCountedBlocks = data.getInt("maximumCountedBlocks", 200);
    }

    @Override
    public void onPayday() {
        produce();
    }

    protected void produce() {
        Map<Resource, Double> toBeAdded = new HashMap<>();
        int blockCount = 0;
        for (Map.Entry<Material, Double> entry : productionPerBlock.entrySet()) {
            int count = site.getBlockCount(entry.getKey());
            blockCount += count;
            if (blockCount > maximumCountedBlocks) {
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
