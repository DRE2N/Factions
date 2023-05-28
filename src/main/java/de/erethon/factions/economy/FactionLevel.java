package de.erethon.factions.economy;

import de.erethon.factions.Factions;
import de.erethon.factions.building.Building;
import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.faction.Faction;

import java.util.HashMap;
import java.util.Set;

public enum FactionLevel {
    HAMLET("Weiler"),
    VILLAGE("Dorf"),
    TOWN("Stadt"),
    CITY("Großstadt"),
    METROPOLIS("Metropole");

    private final String name;
    private final HashMap<PopulationLevel, Integer> requiredPopulation;
    private final Set<Building> requiredBuildings;

    FactionLevel(String name) {
        this.name = name;
        this.requiredPopulation = Factions.get().getFConfig().getRequiredPopulation(this);
        this.requiredBuildings = Factions.get().getFConfig().getRequiredBuildings(this);
    }

    public String getName() {
        return name;
    }

    public HashMap<PopulationLevel, Integer> getRequiredPopulation() {
        return requiredPopulation;
    }

    public Set<Building> getRequiredBuildings() {
        return requiredBuildings;
    }

    public boolean hasRequiredPopulationToLevelUpToThis(Faction faction) {
        if (this == METROPOLIS) {
            return false; // Max level
        }
        for (PopulationLevel level : requiredPopulation.keySet()) {
            if (faction.getPopulation(level) < requiredPopulation.get(level)) {
                return false;
            }
        }
        return true;
    }

    public boolean hasRequiredBuildingsToLevelUpToThis(Faction faction) {
        if (this == METROPOLIS) {
            return false; // Max level
        }
        for (Building building : requiredBuildings) {
            if (!faction.hasBuilding(building)) {
                return false;
            }
        }
        return true;
    }

    public FactionLevel next() {
        return switch (this) {
            case HAMLET -> TOWN;
            case TOWN -> VILLAGE;
            case VILLAGE -> CITY;
            case CITY -> METROPOLIS;
            default -> null;
        };
    }
}
