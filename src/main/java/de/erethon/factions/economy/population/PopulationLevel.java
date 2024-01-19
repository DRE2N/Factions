package de.erethon.factions.economy.population;

import de.erethon.factions.economy.FStorage;
import de.erethon.factions.economy.PopulationResourceConsumption;
import de.erethon.factions.economy.resource.Resource;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Malfrador
 */
public enum PopulationLevel {

    BEGGAR(0),
    PEASANT(0,
            new PopulationResourceConsumption(Resource.GRAIN, 0.5, 0, 3, 1.2),
            new PopulationResourceConsumption(Resource.FISH, 0.5, 0, 3, 0.5)),
    CITIZEN(4,
            new PopulationResourceConsumption(Resource.MEAT, 0.7, 200, 1, 0.5),
            new PopulationResourceConsumption(Resource.BREAD, 0.7, 200, 5, 1.2),
            new PopulationResourceConsumption(Resource.FRUIT, 0.3, 200, 1, 0.5),
            new PopulationResourceConsumption(Resource.VEGETABLES, 0.3, 200, 1, 0.5)),
    PATRICIAN(5,
            new PopulationResourceConsumption(Resource.WINE, 1.0, 50, 5, 1.2),
            new PopulationResourceConsumption(Resource.BOOKS, 0.5, 200, 1, 0.5),
            new PopulationResourceConsumption(Resource.PAPER, 0.5, 200, 1, 0.5)),
    NOBLEMEN(7,
            new PopulationResourceConsumption(Resource.JEWELRY, 0.5, 200, 1, 0.5),
            new PopulationResourceConsumption(Resource.FURNITURE, 0.1, 10, 1, 0.5));

    private final int minimumVariety;
    private final Set<PopulationResourceConsumption> requiredResources;

    PopulationLevel(int minimumVariety, PopulationResourceConsumption... requiredResources) {
        this.minimumVariety = minimumVariety;
        this.requiredResources = Set.of(requiredResources);
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
        return true;
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

}