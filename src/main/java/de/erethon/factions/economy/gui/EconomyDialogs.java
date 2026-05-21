package de.erethon.factions.economy.gui;

import de.erethon.factions.Factions;
import de.erethon.factions.building.Building;
import de.erethon.factions.dialog.FDialogFactory;
import de.erethon.factions.economy.report.BuildingUnlockReport;
import de.erethon.factions.economy.report.EconomyCycleReport;
import de.erethon.factions.economy.report.PopulationLevelReport;
import de.erethon.factions.economy.report.ProductionChainReport;
import de.erethon.factions.economy.report.ResourceFlowReport;
import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.faction.Faction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class EconomyDialogs {

    private EconomyDialogs() {
    }

    public static void showResource(@NotNull Player player, @NotNull Faction faction, @NotNull Resource resource) {
        EconomyCycleReport report = faction.getEconomy().getLastReport();
        ResourceFlowReport flow = report.resources().get(resource);
        List<Component> body = new ArrayList<>();
        int stored = faction.getStorage().getResource(resource);
        int limit = faction.getStorage().getResourceLimit(resource);
        double produced = flow == null ? faction.getEconomy().getLastProduction(resource) : flow.acceptedProduction();
        double lost = flow == null ? 0 : flow.lostProduction();
        double consumed = flow == null ? faction.getEconomy().getLastTotalConsumption(resource) : flow.consumed();
        body.add(Component.translatable("factions.economy.dialog.resource.storage", Component.text(stored), Component.text(limit)));
        body.add(Component.translatable("factions.economy.dialog.resource.production", Component.text(format(produced))));
        if (lost > 0) {
            body.add(Component.translatable("factions.economy.dialog.resource.lost", Component.text(format(lost))).color(NamedTextColor.RED));
        }
        body.add(Component.translatable("factions.economy.dialog.resource.consumption", Component.text(format(consumed))));
        body.add(Component.translatable("factions.economy.dialog.resource.net", Component.text(format(produced - consumed))));
        body.add(Component.empty());
        body.add(Component.translatable("factions.economy.dialog.resource.producers").color(NamedTextColor.GOLD));
        boolean anyProducer = false;
        for (ProductionChainReport chain : report.productionChains()) {
            int amount = chain.outputs().getOrDefault(resource, 0);
            if (amount <= 0) {
                continue;
            }
            anyProducer = true;
            body.add(Component.text("- ", chain.active() ? NamedTextColor.GREEN : NamedTextColor.RED)
                    .append(Component.translatable("factions.building.buildings." + chain.buildingId() + ".name"))
                    .append(Component.text(": +" + amount, NamedTextColor.GRAY)));
        }
        if (!anyProducer) {
            body.add(Component.translatable("factions.general.none"));
        }
        body.add(Component.empty());
        body.add(Component.translatable("factions.economy.dialog.resource.consumers").color(NamedTextColor.GOLD));
        boolean anyConsumer = false;
        for (ProductionChainReport chain : report.productionChains()) {
            int amount = chain.inputs().getOrDefault(resource, 0);
            if (amount <= 0) {
                continue;
            }
            anyConsumer = true;
            body.add(Component.text("- ", chain.active() ? NamedTextColor.GREEN : NamedTextColor.RED)
                    .append(Component.translatable("factions.building.buildings." + chain.buildingId() + ".name"))
                    .append(Component.text(": -" + amount, NamedTextColor.GRAY)));
        }
        for (Map.Entry<PopulationLevel, PopulationLevelReport> entry : report.population().entrySet()) {
            double required = entry.getValue().required().getOrDefault(resource, 0.0);
            if (required <= 0) {
                continue;
            }
            anyConsumer = true;
            body.add(Component.text("- ", NamedTextColor.GRAY)
                    .append(entry.getKey().displayName())
                    .append(Component.text(": " + format(entry.getValue().consumed().getOrDefault(resource, 0.0)) + "/" + format(required), NamedTextColor.GRAY)));
        }
        if (!anyConsumer) {
            body.add(Component.translatable("factions.general.none"));
        }
        FDialogFactory.showNotice(player,
                Component.translatable("factions.economy.dialog.resource.title", resource.displayName()),
                body);
    }

    public static void showPopulation(@NotNull Player player, @NotNull Faction faction, @NotNull PopulationLevel level) {
        EconomyCycleReport report = faction.getEconomy().getLastReport();
        PopulationLevelReport pop = report.population().get(level);
        List<Component> body = new ArrayList<>();
        int population = faction.getPopulation(level);
        int housing = (int) faction.getAttributeValue("housing_" + level.name().toLowerCase(), 0);
        body.add(Component.translatable("factions.gui.population.count", Component.text(population)));
        body.add(Component.translatable("factions.gui.population.housing", Component.text(housing)));
        body.add(Component.translatable("factions.gui.population.happiness", Component.text(format(faction.getHappiness(level)))));
        if (pop != null) {
            body.add(Component.translatable("factions.economy.dialog.population.target_happiness", Component.text(format(pop.targetHappiness()))));
            body.add(Component.translatable("factions.economy.dialog.population.tax", Component.text(format(pop.taxRevenue()))));
            body.add(Component.translatable("factions.economy.dialog.population.promoted", Component.text(pop.promoted())));
            body.add(Component.translatable("factions.economy.dialog.population.demoted", Component.text(pop.demoted())));
            body.add(Component.empty());
            body.add(Component.translatable("factions.economy.dialog.population.demands").color(NamedTextColor.GOLD));
            if (pop.required().isEmpty()) {
                body.add(Component.translatable("factions.general.none"));
            } else {
                for (Resource resource : level.getResources()) {
                    double required = pop.required().getOrDefault(resource, 0.0);
                    double consumed = pop.consumed().getOrDefault(resource, 0.0);
                    body.add(Component.text("- ", NamedTextColor.GRAY)
                            .append(resource.displayName())
                            .append(Component.text(": " + format(consumed) + "/" + format(required), consumed >= required ? NamedTextColor.GREEN : NamedTextColor.RED)));
                }
            }
            appendReasonList(body, "factions.economy.dialog.population.level_up_blockers", pop.levelUpBlockers());
            appendReasonList(body, "factions.economy.dialog.population.level_down_risks", pop.levelDownRisks());
        }
        appendUnlocks(body, faction, level);
        FDialogFactory.showNotice(player,
                Component.translatable("factions.economy.dialog.population.title", level.displayName()),
                body);
    }

    public static void showUnlocks(@NotNull Player player, @NotNull Faction faction) {
        List<Component> body = new ArrayList<>();
        EconomyCycleReport report = faction.getEconomy().getLastReport();
        for (PopulationLevel level : PopulationLevel.values()) {
            body.add(Component.empty());
            body.add(level.displayName().color(NamedTextColor.GOLD));
            List<Building> buildings = Factions.get().getBuildingManager().getBuildings().stream()
                    .filter(building -> building.getRequiredPopulation().containsKey(level))
                    .sorted(Comparator.comparing(Building::getId))
                    .toList();
            if (buildings.isEmpty()) {
                body.add(Component.translatable("factions.general.none"));
                continue;
            }
            for (Building building : buildings) {
                BuildingUnlockReport unlock = report.buildings().get(building.getId());
                Component state = unlock != null && unlock.built()
                        ? Component.translatable("factions.economy.dialog.unlock.built")
                        : unlock != null && unlock.available()
                        ? Component.translatable("factions.economy.dialog.unlock.available")
                        : Component.translatable("factions.economy.dialog.unlock.locked");
                body.add(Component.text("- ", NamedTextColor.GRAY)
                        .append(Component.translatable("factions.building.buildings." + building.getId() + ".name"))
                        .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                        .append(state));
            }
        }
        FDialogFactory.showNotice(player, Component.translatable("factions.economy.dialog.unlock.title"), body);
    }

    private static void appendUnlocks(@NotNull List<Component> body, @NotNull Faction faction, @NotNull PopulationLevel level) {
        List<Building> buildings = Factions.get().getBuildingManager().getBuildings().stream()
                .filter(building -> building.getRequiredPopulation().containsKey(level))
                .sorted(Comparator.comparing(Building::getId))
                .toList();
        body.add(Component.empty());
        body.add(Component.translatable("factions.economy.dialog.population.unlocks").color(NamedTextColor.GOLD));
        if (buildings.isEmpty()) {
            body.add(Component.translatable("factions.general.none"));
            return;
        }
        for (Building building : buildings) {
            body.add(Component.text("- ", NamedTextColor.GRAY)
                    .append(Component.translatable("factions.building.buildings." + building.getId() + ".name")));
        }
    }

    private static void appendReasonList(@NotNull List<Component> body, @NotNull String headingKey, @NotNull List<String> reasons) {
        body.add(Component.empty());
        body.add(Component.translatable(headingKey).color(NamedTextColor.GOLD));
        if (reasons.isEmpty()) {
            body.add(Component.translatable("factions.general.none"));
            return;
        }
        for (String reason : reasons) {
            body.add(Component.text("- ", NamedTextColor.RED)
                    .append(Component.translatable("factions.economy.reason." + reason.replace(':', '.'))));
        }
    }

    private static String format(double value) {
        return String.format("%.1f", value);
    }
}
