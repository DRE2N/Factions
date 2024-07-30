package de.erethon.factions.economy;

import de.erethon.factions.building.attributes.FactionAttribute;
import de.erethon.factions.building.attributes.FactionResourceAttribute;
import de.erethon.factions.economy.population.HappinessModifier;
import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.util.FLogger;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Malfrador
 */
public class FEconomy {

    private final Faction faction;
    private final FStorage storage;
    private final Set<HappinessModifier> happinessModifiers = new HashSet<>();

    public FEconomy(Faction faction, FStorage storage) {
        this.faction = faction;
        this.storage = storage;
    }


    /**
     * Economy works as follows:
     * - Buildings add a resource attribute to the faction
     * - Every payday, the economy calculates the resources gained/lost from these attributes
     * - Population consumes resources and is happy or unhappy based on the resources they get
     * - Population levels up or down based on happiness
     * - Faction levels up based on population and buildings
     */
    public void doEconomyCalculations() {
        for (Map.Entry<String, FactionAttribute> entry : faction.getAttributes().entrySet()) {
            if (entry.getValue() instanceof FactionResourceAttribute attribute) {
                Resource resource = attribute.getResource();

                double factor = faction.getAttributeValue("production_rate", 1.0);
                double amount = attribute.apply().getValue() * factor; // apply() just in case

                storage.addResource(resource, (int) amount);
                FLogger.ECONOMY.log("[" + faction.getName() + "] Received/Lost " + amount + " of " + resource.name());
            }
        }
        calculatePopulationConsumption();
        calculatePopulationHappiness();
        calculatePopulationLevels();
        calculateFactionLevel();
    }

    private void calculatePopulationConsumption() {
        for (PopulationLevel level : PopulationLevel.values()) {
            int currentPop = faction.getPopulation(level);
            for (Resource resource : level.getResources()) {
                double consumptionPerPop = level.getResourceConsumption(resource).consumptionPerPop();
                double totalConsumption = (consumptionPerPop * currentPop);
                double difference = storage.getResource(resource) - totalConsumption;
                double popWhoCouldNotGetEnough = difference / consumptionPerPop;
                if (difference > 0) {
                    storage.removeResource(resource, (int) totalConsumption);
                    HappinessModifier modifier = new HappinessModifier("resource_" + resource.name().toLowerCase(), Component.translatable("factions.economy.population.resources.satisfied", resource.displayName()), new HashMap<>() {{
                        put(level, 1.0);
                    }});
                    addOrReplaceHappinessModifier(modifier);
                    FLogger.ECONOMY.log("[" + faction.getName() + "] Enough " + resource.name() + " for " + currentPop + " " + level.name() + " population.");
                } else {
                    storage.removeResource(resource, storage.getResource(resource));
                    HappinessModifier modifier = new HappinessModifier("resource_" + resource.name().toLowerCase(), Component.translatable("factions.economy.population.resources.dissatisfied", resource.displayName()), new HashMap<>() {{
                        put(level, -1.0 * popWhoCouldNotGetEnough / currentPop);
                    }});
                    addOrReplaceHappinessModifier(modifier);
                    FLogger.ECONOMY.log("[" + faction.getName() + "] Not enough " + resource.name() + " for " + currentPop + " " + level.name() + " population");
                }
            }
        }
    }

    private void calculatePopulationHappiness() {
        for (PopulationLevel level : PopulationLevel.values()) {
            double happiness = 0;
            for (HappinessModifier modifier : happinessModifiers) {
                happiness += modifier.getModifier(level);
            }
            faction.setHappiness(level, happiness);
            FLogger.ECONOMY.log("[" + faction.getName() + "] Happiness for " + level.name() + " is now " + happiness);
        }
    }

    private void calculatePopulationLevels() {
        double totalUnrest = faction.getUnrestLevel();
        for (PopulationLevel level : PopulationLevel.values()) {
            int currentPop = faction.getPopulation(level);
            PopulationLevel nextLevel = level.above();
            PopulationLevel previousLevel = level.below();
            if (nextLevel != level) {
                boolean canLevelUp = true;
                int popToLevelUp = 0;
                int housingForNextLevel = (int) faction.getAttributes().get("housing_" + nextLevel.name().toLowerCase()).getValue();
                int housingCapacity = housingForNextLevel - faction.getPopulation(nextLevel);
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
                if (canLevelUp && level.canLevelUp(storage) && popToLevelUp > 0) {
                    int levellingUp = Math.min(popToLevelUp, housingCapacity);
                    faction.getPopulation().put(level, currentPop - levellingUp);
                    faction.getPopulation().put(nextLevel, faction.getPopulation(nextLevel) + levellingUp);
                    totalUnrest -= levellingUp * nextLevel.getUnrestMultiplier();
                    FLogger.ECONOMY.log("[" + faction.getName() + "] Levelled up " + levellingUp + " from " + level.name() + " to " + nextLevel.name());
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
                    totalUnrest += popToLevelDown * level.getUnrestMultiplier();
                    FLogger.ECONOMY.log("[" + faction.getName() + "] Levelled down " + popToLevelDown + " from " + level.name() + " to " + previousLevel.name());
                }
            }
        }
        faction.setUnrestLevel(totalUnrest);
    }

    private void calculateFactionLevel() {
        for (FactionLevel level : FactionLevel.values()) {
            if (level.hasRequiredPopulationToLevelUpToThis(faction) && level.hasRequiredBuildingsToLevelUpToThis(faction)) {
                faction.setLevel(level);
                FLogger.ECONOMY.log("[" + faction.getName() + "] Levelled up to " + level.name());
            }
        }
    }

    public void addOrReplaceHappinessModifier(HappinessModifier modifier) {
        happinessModifiers.removeIf(m -> m.name().equals(modifier.name()));
        happinessModifiers.add(modifier);
    }

    public void removeHappinessModifier(String name) {
        happinessModifiers.removeIf(m -> m.name().equals(name));
    }

    public void removeHappinessModifier(HappinessModifier modifier) {
        happinessModifiers.remove(modifier);
    }
}
