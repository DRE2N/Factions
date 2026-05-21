package de.erethon.factions.building;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record BuildingUpgradeConfig(
        @NotNull String targetBuildingId,
        int requiredSatisfiedPaydays,
        @NotNull List<BlockRequirement> requiredBlocks
) {
}
