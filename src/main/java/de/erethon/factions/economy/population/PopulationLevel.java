package de.erethon.factions.economy.population;

import de.erethon.factions.economy.FStorage;
import de.erethon.factions.economy.PopulationResourceConsumption;
import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.faction.Faction;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Malfrador
 */
public enum PopulationLevel {

    BEGGAR(new HashSet<>(),0, 1.5),
    PEASANT(new HashSet<>(Arrays.asList("Chapel")), 0, 1.3,
            new PopulationResourceConsumption(Resource.GRAIN, 1, 0, 3, 1.2, 0.7),
            new PopulationResourceConsumption(Resource.FISH, 1, 0, 3, 1.2, 0.7)),
    CITIZEN(new HashSet<>(Arrays.asList("Chapel", "Courthouse")),4, 1.2,
            new PopulationResourceConsumption(Resource.GRAIN, 2, 0, 3, 1.2, 0.2),
            new PopulationResourceConsumption(Resource.VEGETABLES, 1, 200, 1, 0.5, 0.25),
            new PopulationResourceConsumption(Resource.COAL, 1, 200, 1, 0.5, 0.3),
            new PopulationResourceConsumption(Resource.MEAT, 1, 200, 5, 1.2, 0.3)),
    PATRICIAN(new HashSet<>(Arrays.asList("Church", "Park", "Courthouse")),5, 1.1,
            new PopulationResourceConsumption(Resource.VEGETABLES, 2, 200, 1, 0.5, 0.1),
            new PopulationResourceConsumption(Resource.COAL, 2, 200, 1, 0.5, 0.1),
            new PopulationResourceConsumption(Resource.BREAD, 1, 50, 5, 1.2, 0.1),
            new PopulationResourceConsumption(Resource.MEAT, 2, 200, 1, 0.5, 0.15),
            new PopulationResourceConsumption(Resource.BEER, 2, 200, 1, 0.5, 0.15)),
    NOBLEMEN(new HashSet<>(Arrays.asList("Church", "Park", "Government", "Courthouse")), 7, 1.0,
            new PopulationResourceConsumption(Resource.VEGETABLES, 4, 200, 1, 0.5, 0.05),
            new PopulationResourceConsumption(Resource.COAL, 4, 200, 1, 0.5, 0.05),
            new PopulationResourceConsumption(Resource.BREAD, 3, 50, 5, 1.2, 0.075),
            new PopulationResourceConsumption(Resource.MEAT, 3, 200, 1, 0., 0.1),
            new PopulationResourceConsumption(Resource.WINE, 1, 200, 1, 0.5, 0.15),
            new PopulationResourceConsumption(Resource.CANDLES, 1, 200, 1, 0.5, 0.15));

    private final Set<String> buildings;
    private final int minimumVariety;
    private final Set<PopulationResourceConsumption> requiredResources;
    private final double unrestMultiplier;

    PopulationLevel(Set<String> buildings, int minimumVariety, double unrestMultiplier, PopulationResourceConsumption... requiredResources) {
        this.buildings = buildings;
        this.minimumVariety = minimumVariety;
        this.requiredResources = Set.of(requiredResources);
        this.unrestMultiplier = unrestMultiplier;
    }

    public @NotNull PopulationLevel above() {
        return switch (this) {
            case BEGGAR -> PEASANT;
            case PEASANT -> CITIZEN;
            case CITIZEN -> PATRICIAN;
            case PATRICIAN, NOBLEMEN -> NOBLEMEN;
        };
    }

    public @NotNull PopulationLevel below() {
        return switch (this) {
            case BEGGAR, PEASANT -> BEGGAR;
            case CITIZEN -> PEASANT;
            case PATRICIAN -> CITIZEN;
            case NOBLEMEN -> PATRICIAN;
        };
    }

    public boolean canLevelUp(FStorage storage) { // Use a minimum so pop doesn't go up instantly if you have 1 of each resource
        for (PopulationResourceConsumption consumption : requiredResources) {
            if (!storage.canAfford(consumption.resource(), consumption.minimumInStorageToLevelUp())) {
                return false;
            }
        }
        return hasRequiredBuildings(storage.getFaction());
    }

    public PopulationResourceConsumption getResourceConsumption(Resource resource) {
        for (PopulationResourceConsumption consumption : requiredResources) {
            if (consumption.resource() == resource) {
                return consumption;
            }
        }
        return null;
    }

    public Set<Resource> getResources() {
        return requiredResources.stream().map(PopulationResourceConsumption::resource).collect(Collectors.toSet());
    }

    public int getMinimumVariety() {
        return minimumVariety;
    }

    public double getUnrestMultiplier() {
        return unrestMultiplier;
    }

    public boolean hasRequiredBuildings(Faction faction) {
        for (String id : buildings) {
            if (!faction.hasBuilding(id)) {
                return false;
            }
        }
        return true;
    }

    public Component displayName() {
        return Component.translatable("factions.economy.population.level." + name().toLowerCase());
    }

}