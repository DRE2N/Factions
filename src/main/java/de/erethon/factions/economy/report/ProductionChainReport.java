package de.erethon.factions.economy.report;

import de.erethon.factions.economy.resource.Resource;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record ProductionChainReport(
        @NotNull String buildingId,
        boolean active,
        @NotNull Map<Resource, Integer> inputs,
        @NotNull Map<Resource, Integer> outputs
) {
}
