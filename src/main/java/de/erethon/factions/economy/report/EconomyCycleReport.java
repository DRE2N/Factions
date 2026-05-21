package de.erethon.factions.economy.report;

import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.economy.resource.Resource;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EconomyCycleReport {

    private final Map<Resource, ResourceFlowReport> resources = new EnumMap<>(Resource.class);
    private final Map<PopulationLevel, PopulationLevelReport> population = new EnumMap<>(PopulationLevel.class);
    private final Map<String, BuildingUnlockReport> buildings = new HashMap<>();
    private final List<ProductionChainReport> productionChains = new ArrayList<>();
    private double totalTaxRevenue;
    private double unrestBefore;
    private double unrestAfter;

    public @NotNull Map<Resource, ResourceFlowReport> resources() {
        return resources;
    }

    public @NotNull Map<PopulationLevel, PopulationLevelReport> population() {
        return population;
    }

    public @NotNull Map<String, BuildingUnlockReport> buildings() {
        return buildings;
    }

    public @NotNull List<ProductionChainReport> productionChains() {
        return productionChains;
    }

    public double totalTaxRevenue() {
        return totalTaxRevenue;
    }

    public void totalTaxRevenue(double totalTaxRevenue) {
        this.totalTaxRevenue = totalTaxRevenue;
    }

    public double unrestBefore() {
        return unrestBefore;
    }

    public void unrestBefore(double unrestBefore) {
        this.unrestBefore = unrestBefore;
    }

    public double unrestAfter() {
        return unrestAfter;
    }

    public void unrestAfter(double unrestAfter) {
        this.unrestAfter = unrestAfter;
    }
}
