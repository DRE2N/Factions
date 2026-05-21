package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.building.attributes.FactionResourceAttribute;
import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.util.FLogger;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ResourceProduction extends BuildingEffect implements ResourceChainEffect {

    protected final Map<Resource, Integer> production = new HashMap<>();

    public ResourceProduction(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        if (!data.contains("production")) {
            String errorMessage = "ResourceProduction effect is missing 'production' section in " + site.getBuilding().getId() +
                    " for effect ID " + data.getId() + ". Config Data: " + data + " Path: " + data.getConfig().getCurrentPath();
            FLogger.ECONOMY.log(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        ConfigurationSection productionConfig = data.getConfigurationSection("production");
        if (productionConfig == null) {
            String errorMessage = "ResourceProduction effect's 'production' section resolved to null in " + site.getBuilding().getId() +
                    " for effect ID " + data.getId() + ". Config Data: " + data;
            FLogger.ECONOMY.log(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        for (String key : productionConfig.getKeys(false)) {
            try {
                Resource resource = Resource.valueOf(key.toUpperCase());
                production.put(resource, productionConfig.getInt(key));
            } catch (IllegalArgumentException e) {
                FLogger.ECONOMY.log("Invalid resource key '" + key + "' in production section for " + site.getBuilding().getId() + ", effect " + data.getId());
            }
        }
    }

    @Override
    public void onPayday() {
        produce();
    }

    protected void produce() {
        if (!site.hasResourceInputsAvailable()) {
            return;
        }
        for (Map.Entry<Resource, Integer> entry : production.entrySet()) {
            FactionResourceAttribute attribute = faction.getOrCreateAttribute(
                    entry.getKey().name(),
                    FactionResourceAttribute.class,
                    () -> new FactionResourceAttribute(entry.getKey(), 0.0)
            );
            attribute.setBaseValue(attribute.getBaseValue() + entry.getValue());
        }
    }

    @Override
    public @NotNull Map<Resource, Integer> getProducedResources() {
        return Map.copyOf(production);
    }

    @Override
    public @NotNull List<Component> getDetailLines() {
        return List.of(Component.translatable("factions.building.effect.resource_production",
                Component.text(formatResources(production))));
    }

    protected String formatResources(@NotNull Map<Resource, ? extends Number> resources) {
        return resources.entrySet().stream()
                .map(entry -> entry.getKey().getId() + " +" + entry.getValue())
                .collect(Collectors.joining(", "));
    }
}
