package de.erethon.factions.economy;

import de.erethon.factions.economy.resource.Resource;

public record PopulationResourceConsumption(Resource resource, double consumptionPerPop, int minimumInStorageToLevelUp, int dayWithoutConsumptionToLevelDown, double satisfactionFromResource) {
}
