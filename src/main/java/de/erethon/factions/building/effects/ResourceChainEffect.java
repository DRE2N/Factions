package de.erethon.factions.building.effects;

import de.erethon.factions.economy.resource.Resource;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface ResourceChainEffect {

    default @NotNull Map<Resource, Integer> getProducedResources() {
        return Map.of();
    }

    default @NotNull Map<Resource, Integer> getConsumedResources() {
        return Map.of();
    }
}
