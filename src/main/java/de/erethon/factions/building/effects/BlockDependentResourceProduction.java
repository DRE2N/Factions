package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.building.attributes.FactionResourceAttribute;
import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.util.FLogger;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
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
        if (!site.hasResourceInputsAvailable()) {
            return;
        }
        Map<Resource, Double> toBeAdded = new HashMap<>();
        FLogger.ECONOMY.log("Producing resources for " + site.getBuilding().getId() + " with " + productionPerBlock.size() + " block modifiers");
        for (Map.Entry<Material, Double> entry : productionPerBlock.entrySet()) {
            int blockCount = Math.min(site.getBlockCount(entry.getKey()), maximumCountedBlocks);
            for (Map.Entry<Resource, Integer> resource : production.entrySet()) {
                double amount = blockCount * entry.getValue() * resource.getValue();
                toBeAdded.merge(resource.getKey(), amount, Double::sum);
            }
        }
        for (Map.Entry<Resource, Double> entry : toBeAdded.entrySet()) {
            FactionResourceAttribute attribute = faction.getOrCreateAttribute(
                    entry.getKey().name(),
                    FactionResourceAttribute.class,
                    () -> new FactionResourceAttribute(entry.getKey(), 0)
            );
            FLogger.ECONOMY.log("Adding " + entry.getKey().name() + " to " + attribute + " with amount " + entry.getValue() + " from " + site.getBuilding().getId());
            attribute.setBaseValue(attribute.getBaseValue() + entry.getValue());
        }
    }

    @Override
    public @NotNull List<Component> getStatusLines() {
        return List.of(Component.translatable("factions.building.hologram.efficiency",
                Component.text(getEfficiencyPercent())));
    }

    @Override
    public @NotNull List<Component> getDetailLines() {
        return List.of(
                Component.translatable("factions.building.effect.block_efficiency", Component.text(getEfficiencyPercent())),
                Component.translatable("factions.building.effect.resource_production", Component.text(formatResources(production)))
        );
    }

    private int getEfficiencyPercent() {
        if (maximumCountedBlocks <= 0) {
            return 100;
        }
        return Math.min(100, (int) Math.round((getCountedBlocks() * 100.0) / maximumCountedBlocks));
    }

    private int getCountedBlocks() {
        int blockCount = 0;
        for (Material material : productionPerBlock.keySet()) {
            blockCount += site.getBlockCount(material);
            if (blockCount >= maximumCountedBlocks) {
                return maximumCountedBlocks;
            }
        }
        return blockCount;
    }

}
