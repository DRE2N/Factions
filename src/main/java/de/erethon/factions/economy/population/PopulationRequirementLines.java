package de.erethon.factions.economy.population;

import de.erethon.factions.economy.PopulationResourceConsumption;
import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.faction.Faction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PopulationRequirementLines {

    private PopulationRequirementLines() {
    }

    public static @NotNull List<Component> missingTargetLevelRequirements(@NotNull Faction faction,
                                                                          @NotNull PopulationLevel targetLevel) {
        List<Component> blockers = new ArrayList<>();
        for (String requiredBuilding : targetLevel.getRequiredBuildingIds()) {
            String requiredBuildingId = requiredBuilding.toLowerCase(Locale.ROOT);
            if (!faction.hasBuilding(requiredBuildingId)) {
                blockers.add(Component.text("")
                        .append(Component.translatable("factions.building.dialog.site.upgrade_blocker_building",
                                "factions.building.dialog.site.upgrade_blocker_building"))
                        .append(Component.translatable("factions.building.buildings." + requiredBuildingId + ".name",
                                requiredBuildingId).color(NamedTextColor.GOLD)));
            }
        }
        for (Resource resource : targetLevel.getResources()) {
            PopulationResourceConsumption consumption = targetLevel.getResourceConsumption(resource);
            if (consumption == null || consumption.minimumInStorageToLevelUp() <= 0) {
                continue;
            }
            int required = consumption.minimumInStorageToLevelUp();
            int available = faction.getStorage().getResource(resource);
            if (available < required) {
                blockers.add(Component.text("")
                        .append(Component.translatable("factions.building.dialog.site.upgrade_blocker_resource",
                                "factions.building.dialog.site.upgrade_blocker_resource"))
                        .append(Component.translatable("factions.economy.resource." + resource.getId(), resource.getId()).color(NamedTextColor.GOLD))
                        .append(Component.text(" " + available + "/" + required, NamedTextColor.GRAY)));
            }
        }
        return blockers;
    }
}
