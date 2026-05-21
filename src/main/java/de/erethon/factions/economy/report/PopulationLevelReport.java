package de.erethon.factions.economy.report;

import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.economy.resource.Resource;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public record PopulationLevelReport(
        @NotNull PopulationLevel level,
        int population,
        int housing,
        double happiness,
        double targetHappiness,
        double baseSatisfaction,
        double varietyBonus,
        double modifierBonus,
        double taxRevenue,
        int promoted,
        int demoted,
        @NotNull Map<Resource, Double> required,
        @NotNull Map<Resource, Double> consumed,
        @NotNull Map<Resource, Double> satisfaction,
        @NotNull List<String> levelUpBlockers,
        @NotNull List<String> levelDownRisks
) {
}
