package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.building.BuildingContainerType;
import de.erethon.factions.util.FLogger;
import de.erethon.hephaestus.Hephaestus;
import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemLibrary;
import net.kyori.adventure.text.Component;
import net.minecraft.resources.Identifier;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ItemProduction extends BuildingEffect {

    private final HItemLibrary itemLibrary = Hephaestus.INSTANCE.getLibrary();
    private final Map<HItem, Integer> production = new HashMap<>();
    private final int interval; // In seconds
    private int ticks = 0;
    private boolean storageFull = false;

    public ItemProduction(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        site.setRequiresOutputChest(true);
        for (String entry : data.getConfigurationSection("production").getKeys(false)) {
            ConfigurationSection section = data.getConfigurationSection("production." + entry);
            if (section == null) {
                FLogger.ERROR.log("Invalid item production effect for building " + site.getBuilding().getId() + ": " + entry);
                continue;
            }
            Identifier id = Identifier.tryParse(section.getString("id", "air"));
            if (id == null) {
                FLogger.ERROR.log("Invalid item id in production effect for building " + site.getBuilding().getId() + ": " + section.getString("id"));
                continue;
            }
            HItem item = itemLibrary.get(id);
            if (item == null) {
                FLogger.ERROR.log("Item not found in production effect for building " + site.getBuilding().getId() + ": " + section.getString("id"));
                continue;
            }
            production.put(item, section.getInt("amount"));
        }
        interval = data.getInt("interval", 60);
    }

    @Override
    public void tick() {
        ticks++;
        if (ticks >= interval * 20) {
            ticks = 0;
            produce();
        }
    }

    private void produce() {
        for (Map.Entry<HItem, Integer> entry : production.entrySet()) {
            if (!site.addOutputItem(entry.getKey().rollRandomStack(entry.getValue()).getBukkitStack())) {
                storageFull = true;
                site.updateHolo();
                return;
            }
        }
        storageFull = false;
    }

    @Override
    public @NotNull Set<BuildingContainerType> getRequiredContainers() {
        return Set.of(BuildingContainerType.OUTPUT);
    }

    @Override
    public @NotNull List<Component> getStatusLines() {
        if (!storageFull && !site.isOutputBufferFull()) {
            return List.of();
        }
        return List.of(Component.translatable("factions.building.storage.output_full"));
    }

    @Override
    public @NotNull List<Component> getDetailLines() {
        List<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable("factions.building.effect.item_production_interval", Component.text(interval)));
        lines.add(Component.translatable("factions.building.effect.output_buffer",
                Component.text(site.getOutputBufferItemCount()), Component.text(site.getOutputBufferCapacityItems())));
        lines.addAll(getStatusLines());
        return lines;
    }
}
