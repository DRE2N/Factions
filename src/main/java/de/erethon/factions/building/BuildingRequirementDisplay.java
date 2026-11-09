package de.erethon.factions.building;

import de.erethon.factions.Factions;
import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.region.ClaimableRegion;
import de.erethon.factions.region.RegionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class BuildingRequirementDisplay {

    private BuildingRequirementDisplay() {
    }

    static @NotNull List<Component> createLines(@NotNull Faction faction, @Nullable ClaimableRegion region,
                                                @NotNull Location location, @NotNull Building building,
                                                @NotNull Set<RequirementFail> fails) {
        List<Component> lines = new ArrayList<>();
        lines.add(statusLine(!fails.contains(RequirementFail.NO_PERMISSION),
                Component.translatable("factions.building.requirement.detail.permission")));
        boolean validRegion = region != null && !fails.contains(RequirementFail.NOT_IN_REGION);
        lines.add(statusLine(validRegion,
                Component.translatable("factions.building.requirement.detail.region")));
        if (validRegion) {
            lines.add(statusLine(!fails.contains(RequirementFail.TOO_CLOSE_TO_BORDER),
                    Component.translatable("factions.building.requirement.detail.border")));
            lines.add(overlapLine(region, location, building, fails));
        }

        if (building.isUnique()) {
            long existing = faction.getFactionBuildings().stream()
                    .filter(site -> site.getBuilding() == building)
                    .count();
            lines.add(statusLine(!fails.contains(RequirementFail.UNIQUE_BUILDING),
                    Component.translatable("factions.building.requirement.detail.unique",
                            buildingName(building.getId()), Component.text(existing))));
        }

        if (validRegion) {
            appendRegionTypes(lines, region, building, fails);
        }
        appendRequiredBuildings(lines, faction, building);
        appendPopulation(lines, faction, building);
        appendCost(lines, faction, building);
        return lines;
    }

    private static void appendRegionTypes(@NotNull List<Component> lines, @NotNull ClaimableRegion region,
                                          @NotNull Building building, @NotNull Set<RequirementFail> fails) {
        if (building.getRequiredRegionTypes().isEmpty()) {
            return;
        }
        List<Component> allowedTypes = building.getRequiredRegionTypes().stream()
                .sorted(Comparator.comparing(RegionType::name))
                .map(BuildingRequirementDisplay::regionTypeName)
                .toList();
        lines.add(Component.translatable("factions.building.requirement.detail.region_types").color(NamedTextColor.GOLD));
        lines.add(statusLine(!fails.contains(RequirementFail.WRONG_REGION_TYPE),
                Component.translatable("factions.building.requirement.detail.region_type",
                        regionTypeName(region.getType()),
                        Component.join(JoinConfiguration.separator(Component.text(", ")), allowedTypes))));
    }

    private static void appendRequiredBuildings(@NotNull List<Component> lines, @NotNull Faction faction,
                                                @NotNull Building building) {
        if (building.getRequiredBuildings().isEmpty()) {
            return;
        }
        Map<String, Integer> completed = new HashMap<>();
        for (BuildSite site : faction.getFactionBuildings()) {
            if (site.isFinished()) {
                completed.merge(site.getBuilding().getId(), 1, Integer::sum);
            }
        }
        lines.add(Component.translatable("factions.building.requirement.detail.buildings").color(NamedTextColor.GOLD));
        building.getRequiredBuildings().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    int current = completed.getOrDefault(entry.getKey(), 0);
                    lines.add(statusLine(current >= entry.getValue(),
                            Component.translatable("factions.building.requirement.detail.progress",
                                    buildingName(entry.getKey()), Component.text(current), Component.text(entry.getValue()))));
                });
    }

    private static void appendPopulation(@NotNull List<Component> lines, @NotNull Faction faction,
                                         @NotNull Building building) {
        if (building.getRequiredPopulation().isEmpty()) {
            return;
        }
        lines.add(Component.translatable("factions.building.requirement.detail.population").color(NamedTextColor.GOLD));
        building.getRequiredPopulation().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    int current = faction.getPopulation(entry.getKey());
                    lines.add(statusLine(current >= entry.getValue(),
                            Component.translatable("factions.building.requirement.detail.progress",
                                    populationName(entry.getKey()), Component.text(current), Component.text(entry.getValue()))));
                });
    }

    private static void appendCost(@NotNull List<Component> lines, @NotNull Faction faction,
                                   @NotNull Building building) {
        if (building.getUnlockCost().isEmpty()) {
            return;
        }
        lines.add(Component.translatable("factions.building.requirement.detail.cost").color(NamedTextColor.GOLD));
        building.getUnlockCost().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    int current = faction.getStorage().getResource(entry.getKey());
                    lines.add(statusLine(current >= entry.getValue(),
                            Component.translatable("factions.building.requirement.detail.progress",
                                    entry.getKey().displayName(), Component.text(current), Component.text(entry.getValue()))));
                });
    }

    private static @NotNull Component overlapLine(@NotNull ClaimableRegion region, @NotNull Location location,
                                                  @NotNull Building building, @NotNull Set<RequirementFail> fails) {
        if (!fails.contains(RequirementFail.OVERLAPPING_BUILDING)) {
            return statusLine(true, Component.translatable("factions.building.requirement.detail.overlap_clear"));
        }
        List<Component> overlappingBuildings = region.getBuildSites().stream()
                .filter(site -> Factions.get().getBuildingManager()
                        .hasOverlap(building.getCorner1(location), building.getCorner2(location), site))
                .filter(site -> !(building.isAllowOverlap() || site.getBuilding().isAllowOverlap()))
                .map(site -> site.getBuilding().getId())
                .distinct()
                .sorted()
                .map(BuildingRequirementDisplay::buildingName)
                .toList();
        Component names = Component.join(JoinConfiguration.separator(Component.text(", ")), overlappingBuildings);
        return statusLine(false, Component.translatable("factions.building.requirement.detail.overlap", names));
    }

    private static @NotNull Component statusLine(boolean fulfilled, @NotNull Component description) {
        NamedTextColor color = fulfilled ? NamedTextColor.GREEN : NamedTextColor.RED;
        return Component.text(fulfilled ? "✓ " : "✗ ", color)
                .append(description.color(NamedTextColor.GRAY));
    }

    private static @NotNull Component buildingName(@NotNull String id) {
        return Component.translatable("factions.building.buildings." + id + ".name");
    }

    private static @NotNull Component populationName(@NotNull PopulationLevel level) {
        return level.displayName();
    }

    private static @NotNull Component regionTypeName(@NotNull RegionType type) {
        return Component.text(type.getName());
    }
}
