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
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ItemProduction extends BuildingEffect {

    private final HItemLibrary itemLibrary = Hephaestus.INSTANCE.getLibrary();
    private final Map<HItem, Integer> production = new java.util.LinkedHashMap<>();
    private final int interval; // In seconds
    private int nextProductionTick;
    private boolean storageFull = false;

    public ItemProduction(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        site.setRequiresOutputChest(true);
        ConfigurationSection configured = data.getConfigurationSection("production");
        if (configured == null) {
            throw new IllegalArgumentException("ItemProduction requires a production section");
        }
        for (String entry : configured.getKeys(false)) {
            ConfigurationSection section = configured.getConfigurationSection(entry);
            String id = section == null ? entry : section.getString("id", entry);
            int amount = section == null ? configured.getInt(entry) : section.getInt("amount", 1);
            if (amount <= 0) {
                throw new IllegalArgumentException("ItemProduction amount must be positive for " + entry);
            }
            Identifier key = Identifier.tryParse(id);
            HItem item = key == null ? null : itemLibrary.get(key);
            if (item == null) {
                throw new IllegalArgumentException("Unknown Hephaestus item in ItemProduction: " + id);
            }
            production.put(item, amount);
        }
        interval = Math.clamp(data.getInt("interval", 60), 1, Integer.MAX_VALUE / 20);
        nextProductionTick = Bukkit.getCurrentTick() + interval * 20;
    }

    @Override
    public boolean requiresTicking() {
        return true;
    }

    @Override
    public void tick() {
        int currentTick = Bukkit.getCurrentTick();
        if (currentTick - nextProductionTick >= 0) {
            nextProductionTick = currentTick + interval * 20;
            produce();
        }
    }

    private void produce() {
        for (Map.Entry<HItem, Integer> entry : production.entrySet()) {
            if (!site.addOutputItem(entry.getKey().createStack(entry.getValue()).getBukkitStack())) {
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
