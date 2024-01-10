package de.erethon.factions.economy;

import de.erethon.factions.building.attributes.FactionAttribute;
import de.erethon.factions.building.attributes.FactionResourceAttribute;
import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.faction.Faction;

import java.util.Map;

public class FEconomy {

    private final Faction faction;
    private final FStorage storage;

    public FEconomy(Faction faction, FStorage storage) {
        this.faction = faction;
        this.storage = storage;
    }

    public void income() {
        for (Map.Entry<String, FactionAttribute> entry : faction.getAttributes().entrySet()) {
            if (entry.getValue() instanceof FactionResourceAttribute attribute) {
                Resource resource = attribute.getResource();
                attribute.apply(); // Just in case
                double amount = attribute.getValue();
                storage.addResource(resource, (int) amount);
            }
        }
        calculatePopulationConsumption();
    }

    public void calculatePopulationConsumption() {
        for (PopulationLevel level : PopulationLevel.values()) {
            int currentPop = faction.getPopulation(level);
            for (Resource resource : level.getResources()) {
                double consumptionPerPop = level.getResourceConsumption(resource).consumptionPerPop();
                double totalConsumption = (consumptionPerPop * currentPop);
                double difference = storage.getResource(resource) - totalConsumption;
                double popWhoCouldNotGetEnough = difference / consumptionPerPop;
                if (difference > 0) {
                    storage.removeResource(resource, (int) totalConsumption);
                    faction.addHappiness(level, popWhoCouldNotGetEnough * level.getResourceConsumption(resource).satisfactionFromResource());
                } else {
                    storage.removeResource(resource, storage.getResource(resource));
                    faction.addHappiness(level, -popWhoCouldNotGetEnough * level.getResourceConsumption(resource).satisfactionFromResource());
                }
            }
        }
    }

    public void calculatePopulationLevels() {
        for (PopulationLevel level : PopulationLevel.values()) {
            int currentPop = faction.getPopulation(level);
            PopulationLevel nextLevel = level.above();
            PopulationLevel previousLevel = level.below();
            if (nextLevel != level) {
                boolean canLevelUp = true;
                int popToLevelUp = 0;
                for (Resource resource : nextLevel.getResources()) {
                    double minimumInStorageToLevelUp = nextLevel.getResourceConsumption(resource).minimumInStorageToLevelUp();
                    if (!storage.canAfford(resource, (int) minimumInStorageToLevelUp)) {
                        canLevelUp = false;
                        break;
                    }
                    double consumptionPerPop = nextLevel.getResourceConsumption(resource).consumptionPerPop();
                    double popWeCanFeed = storage.getResource(resource) / consumptionPerPop;
                    popToLevelUp = (int) Math.min(popWeCanFeed, popToLevelUp);
                }
                if (canLevelUp) {
                    faction.getPopulation().put(level, currentPop - popToLevelUp);
                    faction.getPopulation().put(nextLevel, faction.getPopulation(nextLevel) + popToLevelUp);
                }
            }
            if (previousLevel != level) {
                boolean needsToLevelDown = false;
                int popToLevelDown = 0;
                double happiness = faction.getHappiness(level);
                if (happiness < 0) {
                    needsToLevelDown = true;
                    popToLevelDown = (int) Math.min(currentPop, Math.abs(happiness) * 10);
                }
                if (needsToLevelDown) {
                    faction.getPopulation().put(level, currentPop - popToLevelDown);
                    faction.getPopulation().put(previousLevel, faction.getPopulation(previousLevel) + popToLevelDown);
                }
            }
        }
    }
}
