package de.erethon.factions.economy.report;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record BuildingUnlockReport(
        @NotNull String buildingId,
        boolean built,
        boolean available,
        @NotNull List<String> blockers
) {
}
