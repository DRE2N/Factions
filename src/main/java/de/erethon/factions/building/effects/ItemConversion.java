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
    private final HashMap<HItem, HItem> conversion = new HashMap<>();

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
            Identifier outputId = Identifier.tryParse(section.getString(key, "air"));
            if (inputId == null || outputId == null) {
                FLogger.ERROR.log("Invalid ItemConversion mapping in " + site.getBuilding().getId() + ": " + key + " -> " + section.getString(key));
                continue;
            }
            HItem input = itemLibrary.get(inputId);
            HItem output = itemLibrary.get(outputId);
            if (input == null || output == null) {
                FLogger.ERROR.log("Unknown ItemConversion item in " + site.getBuilding().getId() + ": " + key + " -> " + section.getString(key));
                continue;
            }
            conversion.put(input, output);
        }
    }

    @Override
    public void tick() {
        if (ticks++ < buildingTicksPerItem) {
            return;
        }
        ticks = 0;
        for (Map.Entry<HItem, HItem> entry : conversion.entrySet()) {
            ItemStack consumed = site.takeFirstInputMatching(stack -> {
                HItemStack hStack = itemLibrary.get(stack);
                return hStack != null && hStack.getItem().equals(entry.getKey());
            }, 1);
            if (consumed == null) {
                continue;
            }
            if (!site.addOutputItem(entry.getValue().rollRandomStack().getBukkitStack())) {
                site.addInputItem(consumed);
                storageFull = true;
                site.updateHolo();
                return;
            }
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
