package de.erethon.factions.economy.population;

import de.erethon.factions.economy.FStorage;
import de.erethon.factions.economy.PopulationResourceConsumption;
import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.faction.Faction;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a population level and defines its resource consumption requirements,
 * building prerequisites, and unrest characteristics.
 * <p>
 * Each level has a set of required resources, where each resource is associated with
 * consumption values and a satisfaction weight. These values are used to calculate how well
 * the needs of that population level are met, which in turn affects tax revenue, happiness,
 * and potential leveling up or down.
 * </p>
 *
 * <p><b>Key methods:</b>
 * <ul>
 *   <li>{@link #above()} returns the next higher population level.</li>
 *   <li>{@link #below()} returns the next lower population level.</li>
 *   <li>{@link #canLevelUp(FStorage)} checks if resource and building conditions are met for leveling up.</li>
 *   <li>{@link #getSatisfactionWeight(Resource)} retrieves the weight used in satisfaction calculations for a given resource.</li>
 * </ul></p>
 *
 * @author Malfrador
 */
public enum PopulationLevel {

    BEGGAR(new HashSet<>(), 0, 1.5),
    PEASANT(new HashSet<>(List.of("Chapel")), 0, 1.3,
            new PopulationResourceConsumption(Resource.GRAIN, 1, 0, 3, 1.2, 0.7),
            new PopulationResourceConsumption(Resource.FISH, 1, 0, 3, 1.2, 0.7)),
    CITIZEN(new HashSet<>(Arrays.asList("Chapel", "Courthouse")), 4, 1.2,
            new PopulationResourceConsumption(Resource.GRAIN, 2, 0, 3, 1.2, 0.2),
            new PopulationResourceConsumption(Resource.VEGETABLES, 1, 200, 1, 0.5, 0.25),
            new PopulationResourceConsumption(Resource.COAL, 1, 200, 1, 0.5, 0.3),
            new PopulationResourceConsumption(Resource.MEAT, 1, 200, 5, 1.2, 0.3)),
    PATRICIAN(new HashSet<>(Arrays.asList("Church", "Park", "Courthouse")), 5, 1.1,
            new PopulationResourceConsumption(Resource.VEGETABLES, 2, 200, 1, 0.5, 0.1),
            new PopulationResourceConsumption(Resource.COAL, 2, 200, 1, 0.5, 0.1),
            new PopulationResourceConsumption(Resource.BREAD, 1, 50, 5, 1.2, 0.1),
            new PopulationResourceConsumption(Resource.MEAT, 2, 200, 1, 0.5, 0.15),
            new PopulationResourceConsumption(Resource.BEER, 2, 200, 1, 0.5, 0.15)),
    NOBLEMEN(new HashSet<>(Arrays.asList("Church", "Park", "Government", "Courthouse")), 7, 1.0,
            new PopulationResourceConsumption(Resource.VEGETABLES, 4, 200, 1, 0.5, 0.05),
            new PopulationResourceConsumption(Resource.COAL, 4, 200, 1, 0.5, 0.05),
            new PopulationResourceConsumption(Resource.BREAD, 3, 50, 5, 1.2, 0.075),
            new PopulationResourceConsumption(Resource.MEAT, 3, 200, 1, 0.5, 0.1),
            new PopulationResourceConsumption(Resource.WINE, 1, 200, 1, 0.5, 0.15),
            new PopulationResourceConsumption(Resource.CANDLES, 1, 200, 1, 0.5, 0.15));

    private final Set<String> buildings;
    private final int minimumVariety;
    private final Set<PopulationResourceConsumption> requiredResources;
    private final double unrestMultiplier;

    /**
     * Constructs a population level with the specified requirements.
     *
     * @param buildings         A set of required building identifiers.
     * @param minimumVariety    The minimum diversity (or variety) required.
     * @param unrestMultiplier  A multiplier affecting how much unrest is generated.
     * @param requiredResources One or more resource consumption definitions.
     */
    PopulationLevel(Set<String> buildings, int minimumVariety, double unrestMultiplier, PopulationResourceConsumption... requiredResources) {
        this.buildings = buildings;
        this.minimumVariety = minimumVariety;
        this.requiredResources = Set.of(requiredResources);
        this.unrestMultiplier = unrestMultiplier;
    }

    /**
     * Returns the next higher population level.
     *
     * @return the population level above the current one.
     */
    public @NotNull PopulationLevel above() {
        return switch (this) {
            case BEGGAR -> PEASANT;
            case PEASANT -> CITIZEN;
            case CITIZEN -> PATRICIAN;
            case PATRICIAN, NOBLEMEN -> NOBLEMEN;
        };
    }

    /**
     * Returns the next lower population level.
     *
     * @return the population level below the current one.
     */
    public @NotNull PopulationLevel below() {
        return switch (this) {
            case BEGGAR, PEASANT -> BEGGAR;
            case CITIZEN -> PEASANT;
            case PATRICIAN -> CITIZEN;
            case NOBLEMEN -> PATRICIAN;
        };
    }

    /**
     * Checks whether the population level can level up based on the available resources and required buildings.
     *
     * @param storage The storage object containing available resources.
     * @return true if all resource requirements and building conditions are met; false otherwise.
     */
    public boolean canLevelUp(FStorage storage) {
        for (PopulationResourceConsumption consumption : requiredResources) {
            if (!storage.canAfford(consumption.resource(), consumption.minimumInStorageToLevelUp())) {
                return false;
            }
        }
        return hasRequiredBuildings(storage.getFaction());
    }

    /**
     * Returns the resource consumption definition for a given resource.
     *
     * @param resource The resource to look up.
     * @return The corresponding PopulationResourceConsumption, or null if not defined.
     */
    public PopulationResourceConsumption getResourceConsumption(Resource resource) {
        for (PopulationResourceConsumption consumption : requiredResources) {
            if (consumption.resource() == resource) {
                return consumption;
            }
        }
        return null;
    }

    /**
     * Returns a set of all resources required by this population level.
     *
     * @return A set of resources.
     */
    public Set<Resource> getResources() {
        return requiredResources.stream()
                .map(PopulationResourceConsumption::resource)
                .collect(Collectors.toSet());
    }

    /**
     * Returns the minimum variety requirement for this population level.
     *
     * @return the minimum variety.
     */
    public int getMinimumVariety() {
        return minimumVariety;
    }

    /**
     * Returns the unrest multiplier for this population level.
     *
     * @return the unrest multiplier.
     */
    public double getUnrestMultiplier() {
        return unrestMultiplier;
    }

    /**
     * Checks whether the faction has all required buildings for this population level.
     *
     * @param faction The faction to check.
     * @return true if all required buildings are present; false otherwise.
     */
    public boolean hasRequiredBuildings(Faction faction) {
        for (String id : buildings) {
            if (!faction.hasBuilding(id)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a display name for the population level.
     *
     * @return a translatable Component for the population level.
     */
    public Component displayName() {
        return Component.translatable("factions.economy.population.level." + name().toLowerCase());
    }

    /**
     * Returns the satisfaction weight for a given resource as defined in the consumption settings.
     * This weight is used in calculating overall satisfaction for tax revenue and happiness.
     *
     * @param resource The resource to retrieve the weight for.
     * @return The satisfaction weight for the resource, or 1.0 if not defined.
     */
    public double getSatisfactionWeight(Resource resource) {
        PopulationResourceConsumption prc = getResourceConsumption(resource);
        return prc != null ? prc.satisfactionFromResource() : 1.0;
    }
}
