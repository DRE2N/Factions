package de.erethon.factions.economy.report;

import de.erethon.factions.economy.resource.Resource;
import org.jetbrains.annotations.NotNull;

public record ResourceFlowReport(
        @NotNull Resource resource,
        int stored,
        int capacity,
        double produced,
        double acceptedProduction,
        double lostProduction,
        double consumed,
        double net,
        boolean storageFull
) {
}
