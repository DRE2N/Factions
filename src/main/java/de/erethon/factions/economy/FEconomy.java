package de.erethon.factions.economy;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.attributes.FactionAttribute;
import de.erethon.factions.building.attributes.FactionResourceAttribute;
import de.erethon.factions.economy.population.HappinessModifier;
import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.economy.population.entities.Revolutionary;
import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.region.LazyChunk;
import de.erethon.factions.util.FLogger;
import net.kyori.adventure.text.Component;
import org.bukkit.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * @author Malfrador
 */
public class FEconomy {

    private final static double UNREST_SPAWN_MULTIPLIER = 10.0;

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
        doBuildingStuff(); // Do building stuff before the payday, so buildings affect production rates at short notice
        for (Map.Entry<String, FactionAttribute> entry : faction.getAttributes().entrySet()) {
            if (entry.getValue() instanceof FactionResourceAttribute attribute) {
                Resource resource = attribute.getResource();

                double factor = faction.getAttributeValue("production_rate", 1.0);
                double amount = attribute.apply().getValue() * factor; // apply() just in case

                storage.addResource(resource, (int) amount);
                FLogger.ECONOMY.log("[" + faction.getName() + "] Received/Lost " + amount + " of " + resource.name());
            }
        }
        calculatePopulationConsumption(); // Calculate and consume resources for the population
        calculatePopulationHappiness(); // Based on the resources they got, calculate happiness
        calculatePopulationLevels(); // Level up or down based on happiness
        calculateFactionLevel(); // Level up the faction based on population and buildings, if possible
    }

    private void doBuildingStuff() {
        // Some buildings might have effects that need to be applied before the payday
        for (BuildSite site : faction.getFactionBuildings()) {
            site.onPrePayday();
        }
        // Payday
        for (BuildSite site : faction.getFactionBuildings()) {
            site.onPayday();
        }
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
        totalUnrest = totalUnrest * faction.getAttributeValue("unrest_multiplier", 1.0);
        faction.setUnrestLevel(totalUnrest);
        if (totalUnrest > 0) {
            spawnRevolt(faction, (int) (totalUnrest * UNREST_SPAWN_MULTIPLIER));
        }
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

    // Select random chunks in the faction's core region and spawn a revolt there
    public void spawnRevolt(Faction faction, int attempts) {
        MessageUtil.log("Spawning revolt for " + faction.getName());
        World world = faction.getCoreRegion().getWorld();
        Random random = new Random();
        int maxX = 0;
        int minX = 0;
        int maxZ = 0;
        int minZ = 0;
        for (LazyChunk chunk : faction.getCoreRegion().getChunks()) {
            maxX = Math.max(maxX, chunk.getX());
            minZ = Math.min(minZ, chunk.getZ());
            maxZ = Math.max(maxZ, chunk.getZ());
            minX = Math.min(minX, chunk.getX());
        }
        MessageUtil.log("minX: " + minX + ", maxX: " + maxX + ", minZ: " + minZ + ", maxZ: " + maxZ);
        for (int i = 0; i < attempts; i++) {
            int x = random.nextInt(minZ, maxX);
            int z = random.nextInt(minZ, maxZ);
            MessageUtil.log("Chunk: " + x + ", " + z);
            if (world.isChunkLoaded(x, z)) {
                // Spawn a small group of revolutionaries
                int amount = random.nextInt(2, 8);
                for (int j = 0; j < amount; j++) {
                    int xInChunk = random.nextInt(-8, 8);
                    int zInChunk = random.nextInt(-8, 8);
                    int xInWorld = x * 16 + xInChunk;
                    int zInWorld = z * 16 + zInChunk;
                    Revolutionary rev = new Revolutionary(faction, world.getHighestBlockAt(xInWorld, zInWorld).getLocation());
                    ((org.bukkit.craftbukkit.CraftWorld) world).getHandle().addFreshEntity(rev);
                    MessageUtil.log("Spawned revolutionary at " + x + ", " + z);
                }
            }
        }
    }
}
