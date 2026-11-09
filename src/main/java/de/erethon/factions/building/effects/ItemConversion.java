package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.building.BuildingContainerType;
import de.erethon.factions.util.FLogger;
import de.erethon.hephaestus.Hephaestus;
import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemLibrary;
import de.erethon.hephaestus.items.HItemStack;
import net.kyori.adventure.text.Component;
import net.minecraft.resources.Identifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ItemConversion extends BuildingEffect {

    private final HItemLibrary itemLibrary = Hephaestus.INSTANCE.getLibrary();
    private final int buildingTicksPerItem;
    private int ticks = 0;
    private boolean missingInput = false;
    private boolean storageFull = false;
    private final Map<HItem, HItem> conversion = new java.util.LinkedHashMap<>();

    public ItemConversion(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        site.setRequiresOutputChest(true);
        site.setRequiresInputChest(true);
        buildingTicksPerItem = data.getInt("buildingTicksPerItem", 20);
        ConfigurationSection section = data.getConfigurationSection("conversion");
        if (section == null) {
            FLogger.ERROR.log("ItemConversion effect for " + site.getBuilding().getId() + " is missing conversion section.");
            return;
        }
        for (String key : section.getKeys(false)) {
            Identifier inputId = Identifier.tryParse(key);
            String outputName = section.getString(key);
            Identifier outputId = outputName == null ? null : Identifier.tryParse(outputName);
            HItem input = inputId == null ? null : itemLibrary.get(inputId);
            HItem output = outputId == null ? null : itemLibrary.get(outputId);
            if (input == null || output == null) {
                throw new IllegalArgumentException("Unknown Hephaestus items in ItemConversion: " + key + " -> " + outputName);
            }
            conversion.put(input, output);
        }
    }

    @Override
    public boolean requiresTicking() {
        return true;
    }

    @Override
    public void tick() {
        if (++ticks < Math.max(1, buildingTicksPerItem)) {
            return;
        }
        ticks = 0;
        for (Map.Entry<HItem, HItem> entry : conversion.entrySet()) {
            ItemStack matchingInput = site.findFirstInputMatching(stack -> {
                HItemStack hStack = itemLibrary.get(stack);
                return hStack != null && hStack.getItem() != null
                        && hStack.getItem().getKey().equals(entry.getKey().getKey());
            });
            if (matchingInput == null) {
                continue;
            }
            if (!site.addOutputItem(entry.getValue().createStack().getBukkitStack())) {
                storageFull = true;
                site.updateHolo();
                return;
            }
            site.takeFirstInputMatching(stack -> stack == matchingInput, 1);
            missingInput = false;
            storageFull = false;
            return;
        }
        missingInput = true;
        site.updateHolo();
    }

    @Override
    public @NotNull Set<BuildingContainerType> getRequiredContainers() {
        return Set.of(BuildingContainerType.INPUT, BuildingContainerType.OUTPUT);
    }

    @Override
    public @NotNull List<Component> getStatusLines() {
        if (storageFull || site.isOutputBufferFull()) {
            return List.of(Component.translatable("factions.building.storage.output_full"));
        }
        if (missingInput) {
            return List.of(Component.translatable("factions.building.storage.input_empty"));
        }
        return List.of();
    }

    @Override
    public @NotNull List<Component> getDetailLines() {
        List<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable("factions.building.effect.item_conversion"));
        lines.add(Component.translatable("factions.building.effect.input_buffer",
                Component.text(site.getInputBufferItemCount()), Component.text(site.getInputBufferCapacityItems())));
        lines.add(Component.translatable("factions.building.effect.output_buffer",
                Component.text(site.getOutputBufferItemCount()), Component.text(site.getOutputBufferCapacityItems())));
        lines.addAll(getStatusLines());
        return lines;
    }
}
