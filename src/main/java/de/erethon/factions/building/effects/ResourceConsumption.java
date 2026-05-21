package de.erethon.factions.building.effects;

import de.erethon.factions.Factions;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.economy.resource.Resource;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ResourceConsumption extends BuildingEffect implements ResourceChainEffect {

    private final Map<Resource, Integer> consumption = new HashMap<>();
    private final boolean shouldDisableBuilding;
    private boolean buildingIsDisabled = false;

    public ResourceConsumption(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        for (String key : data.getConfigurationSection("consumption").getKeys(false)) {
            Resource resource = Resource.valueOf(key.toUpperCase());
            consumption.put(resource, data.getInt("consumption." + key));
        }
        shouldDisableBuilding = data.getBoolean("disableBuilding", true);
    }

    @Override
    public void onPrePayday() {
        consume();
    }

    private void consume() {
        boolean canAfford = true;
        for (HashMap.Entry<Resource, Integer> entry : consumption.entrySet()) {
            if (!faction.getStorage().canAfford(entry.getKey(), entry.getValue())) {
                canAfford = false;
                continue;
            }
        }
        if (!canAfford) {
            if (shouldDisableBuilding) {
                site.setResourceInputsAvailable(false);
                buildingIsDisabled = true;
            }
            site.updateHolo();
            return;
        }
        for (HashMap.Entry<Resource, Integer> entry : consumption.entrySet()) {
            faction.getStorage().removeResource(entry.getKey(), entry.getValue());
        }
        site.setResourceInputsAvailable(true);
        buildingIsDisabled = false;
    }

    @Override
    public @NotNull Map<Resource, Integer> getConsumedResources() {
        return Map.copyOf(consumption);
    }

    @Override
    public @NotNull List<Component> getStatusLines() {
        if (!buildingIsDisabled) {
            return List.of();
        }
        return List.of(Component.translatable("factions.building.storage.resource_input_missing"));
    }

    @Override
    public @NotNull List<Component> getDetailLines() {
        List<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable("factions.building.effect.resource_consumption",
                Component.text(formatResources(consumption))));
        if (buildingIsDisabled) {
            lines.addAll(getStatusLines());
        }
        return lines;
    }

    private String formatResources(@NotNull Map<Resource, Integer> resources) {
        return resources.entrySet().stream()
                .map(entry -> entry.getKey().getId() + " -" + entry.getValue())
                .collect(Collectors.joining(", "));
    }
}
