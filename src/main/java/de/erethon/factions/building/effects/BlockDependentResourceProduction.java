package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.building.attributes.FactionAttributeModifier;
import de.erethon.factions.building.attributes.FactionResourceAttribute;
import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.util.FLogger;
import org.bukkit.Material;
import org.bukkit.attribute.AttributeModifier;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class BlockDependentResourceProduction extends ResourceProduction {

    private final Map<Material, Double> productionPerBlock = new HashMap<>();
    private final int maximumCountedBlocks;

    public BlockDependentResourceProduction(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        for (String key : data.getConfig().getConfigurationSection("blockModifiers").getKeys(false)) {
            Material material = Material.valueOf(key.toUpperCase());
            productionPerBlock.put(material, data.getDouble("blockModifiers." + key, 1.0));
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
        FLogger.ECONOMY.log("Producing resources for " + site.getBuilding().getId() + " with " + productionPerBlock.size() + " block modifiers");
        for (Map.Entry<Material, Double> entry : productionPerBlock.entrySet()) {
            int count = site.getBlockCount(entry.getKey());
            blockCount += count;
            if (blockCount > maximumCountedBlocks) {
                blockCount = maximumCountedBlocks;
            }
            double amount = blockCount * entry.getValue();
            for (Map.Entry<Resource, Integer> material : production.entrySet()) {
                toBeAdded.put(material.getKey(), amount);
            }
        }
        for (Map.Entry<Resource, Double> entry : toBeAdded.entrySet()) {
            FactionResourceAttribute attribute = faction.getOrCreateAttribute(
                    entry.getKey().name(),
                    FactionResourceAttribute.class,
                    () -> new FactionResourceAttribute(entry.getKey(), 0)
            );
            FLogger.ECONOMY.log("Adding " + entry.getKey().name() + " to " + attribute + " with amount " + entry.getValue() + " from " + site.getBuilding().getId());
            attribute.addModifier(new FactionAttributeModifier(entry.getValue(), AttributeModifier.Operation.ADD_NUMBER));
        }
    }


}
