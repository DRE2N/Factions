package de.erethon.factions.economy;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.attributes.FactionAttribute;
import de.erethon.factions.building.attributes.FactionResourceAttribute;
import de.erethon.factions.economy.population.HappinessModifier;
import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.economy.population.entities.Revolutionary;
import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.economy.resource.ResourceCategory;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.region.LazyChunk;
import de.erethon.factions.util.FLogger;
import org.bukkit.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles the economy for a faction by processing resource production,
 * consumption by the population, calculation of overall satisfaction,
 * generation of tax revenue, and the leveling up/down of population and faction.
 * <p>
 * The economy cycle is executed in several steps:
 * <ol>
 *   <li>Buildings are processed to adjust resource production.</li>
 *   <li>Resources are produced and added to the faction's storage.</li>
 *   <li>Each population level consumes resources based on its defined needs, generating a satisfaction ratio (0.0–1.0) per resource.</li>
 *   <li>The overall satisfaction for each population level is computed as a weighted average (using the weights defined in the population level).</li>
 *   <li>Tax revenue is then generated per citizen, scaled by the satisfaction value.</li>
 *   <li>Population levels are adjusted (leveled up or down) based on satisfaction and housing constraints.</li>
 *   <li>The faction's level is updated if it meets the criteria.</li>
 *   <li>If unrest builds up (from unhappy citizens), a revolt is spawned in the faction's core region.</li>
 * </ol>
 * </p>
 *
 * @author Malfrador
 */
public class FEconomy {

    /** Base money produced by each citizen per cycle */
    private final static double MONEY_PER_CITIZEN = 10.0;
    /** Multiplier used to determine how many revolutionary units spawn based on unrest */
    private final static double UNREST_SPAWN_MULTIPLIER = 10.0;
    private static final double VARIETY_BONUS_FACTOR = 0.1;

    private final Faction faction;
    private final FStorage storage;
    private final Set<HappinessModifier> happinessModifiers = new HashSet<>();

    /**
     * Stores the per-resource satisfaction ratios (0.0 to 1.0) for each population level.
     * This is calculated during resource consumption.
     */
    private final Map<PopulationLevel, Map<Resource, Double>> resourceSatisfaction = new HashMap<>();

    /**
     * Constructs the economy system for a given faction.
     *
     * @param faction The faction for which the economy is managed.
     * @param storage The storage system that tracks available resources.
     */
    public FEconomy(Faction faction, FStorage storage) {
        this.faction = faction;
        this.storage = storage;
    }

    public void doEconomyCalculations() {
        doBuildingStuff();

        // Produce resources from buildings
        for (Map.Entry<String, FactionAttribute> entry : faction.getAttributes().entrySet()) {
            if (entry.getValue() instanceof FactionResourceAttribute attribute) {
                Resource resource = attribute.getResource();
                double factor = faction.getAttributeValue("production_rate", 1.0);
                double amount = attribute.apply().getValue() * factor;
                storage.addResource(resource, (int) amount);
                FLogger.ECONOMY.log("[" + faction.getName() + "] Received/Lost " + amount + " of " + resource.name());
            }
        }

        calculatePopulationConsumption();
        calculatePopulationHappiness();
        calculateTaxRevenue();
        calculatePopulationLevels();
        calculateFactionLevel();
    }

    /**
     * Processes building effects by invoking both pre-payday and payday methods on each building.
     * This allows buildings to affect production and other economy-related parameters.
     */
    private void doBuildingStuff() {
        // Pre-payday building effects
        for (BuildSite site : faction.getFactionBuildings()) {
            site.onPrePayday();
        }
        // Payday building effects
        for (BuildSite site : faction.getFactionBuildings()) {
            site.onPayday();
        }
    }

    /**
     * Calculates resource consumption for each population level.
     * <p>
     * For every population level:
     * <ul>
     *   <li>Compute the required amount of each resource (consumption per citizen * number of citizens).</li>
     *   <li>Determine the satisfaction ratio as (available / required), clamped to 1.0.</li>
     *   <li>Consume the resources (up to the required amount) and log the consumption.</li>
     *   <li>Store the satisfaction ratios for later use in happiness and tax calculations.</li>
     * </ul>
     * </p>
     */
    private void calculatePopulationConsumption() {
        for (PopulationLevel level : PopulationLevel.values()) {
            int currentPop = faction.getPopulation(level);
            Map<Resource, Double> satisfactionMap = new HashMap<>();
            for (Resource resource : level.getResources()) {
                double consumptionPerPop = level.getResourceConsumption(resource).consumptionPerPop();
                double required = consumptionPerPop * currentPop;
                double available = storage.getResource(resource);
                double satisfaction = (required > 0) ? Math.min(1.0, available / required) : 1.0;
                satisfactionMap.put(resource, satisfaction);
                // Consume resource up to the required amount
                storage.removeResource(resource, (int) Math.min(available, required));
                FLogger.ECONOMY.log("[" + faction.getName() + "] Consumed " + Math.min(available, required)
                        + " of " + resource.name() + " for " + currentPop + " " + level.name()
                        + " population. Satisfaction: " + satisfaction);
            }
            resourceSatisfaction.put(level, satisfactionMap);
        }
    }

    /**
     * Calculates and updates the overall happiness of each population level within the faction.
     * <p>
     * The happiness value is determined using the following factors:
     * <ul>
     *     <li><b>Base Satisfaction:</b> A weighted average of resource satisfaction ratios,
     *         where each required resource contributes based on its importance.</li>
     *     <li><b>Additional Modifiers:</b> Effects from external happiness modifiers
     *         (e.g., buildings, events, faction-wide effects).</li>
     *     <li><b>Variety Bonus:</b> A satisfaction boost based on how many distinct resources
     *         from each {@link ResourceCategory} are provided. The bonus is calculated as:
     *         <pre>
     *             variety_bonus = (distinct_resources / min_variety) * VARIETY_BONUS_FACTOR
     *         </pre>
     *         where min_variety is the minimum number of distinct resources required for full variety.</li>
     * </ul>
     */
    private void calculatePopulationHappiness() {
        for (PopulationLevel level : PopulationLevel.values()) {
            Map<Resource, Double> satisfactionMap = resourceSatisfaction.getOrDefault(level, new HashMap<>());
            double weightedSum = 0;
            double totalWeight = 0;
            for (Resource resource : level.getResources()) {
                double weight = level.getSatisfactionWeight(resource);
                double resSat = satisfactionMap.getOrDefault(resource, 0.0);
                weightedSum += weight * resSat;
                totalWeight += weight;
            }
            double baseSatisfaction = (totalWeight > 0) ? weightedSum / totalWeight : 1.0;

            // Include any additional happiness modifiers (e.g., from building effects)
            double additionalModifiers = 0;
            for (HappinessModifier modifier : happinessModifiers) {
                additionalModifiers += modifier.getModifier(level);
            }

            double totalVarietyBonus = calculateVarietyBonus(level, satisfactionMap);

            double finalHappiness = baseSatisfaction + additionalModifiers + totalVarietyBonus;
            faction.setHappiness(level, finalHappiness);
            FLogger.ECONOMY.log("[" + faction.getName() + "] Overall satisfaction for " + level.name()
                    + " is " + baseSatisfaction + " with modifiers " + additionalModifiers
                    + " and total variety bonus " + totalVarietyBonus
                    + " -> final happiness: " + finalHappiness);
        }
    }

    /**
     * Calculates and collects tax revenue based on population happiness.
     * <p>
     * The revenue for each population level is determined using the following formula:
     * <pre>
     *     revenue = population * MONEY_PER_CITIZEN * tax_satisfaction
     * </pre>
     * where:
     * <ul>
     *     <li><b>Base Satisfaction:</b> A weighted average of resource satisfaction ratios,
     *         determining how well the population’s needs are met.</li>
     *     <li><b>Variety Bonus:</b> A boost applied if multiple distinct resources from a
     *         {@link ResourceCategory} are provided, improving satisfaction.</li>
     *     <li><b>Tax Satisfaction:</b> The final satisfaction score used in tax calculations,
     *         capped between 0 and 1.</li>
     * </ul>
     */
    private void calculateTaxRevenue() {
        double totalTaxRevenue = 0.0;
        for (PopulationLevel level : PopulationLevel.values()) {
            int population = faction.getPopulation(level);
            Map<Resource, Double> satisfactionMap = resourceSatisfaction.getOrDefault(level, new HashMap<>());
            double weightedSum = 0;
            double totalWeight = 0;
            for (Resource resource : level.getResources()) {
                double weight = level.getSatisfactionWeight(resource);
                double resSat = satisfactionMap.getOrDefault(resource, 0.0);
                weightedSum += weight * resSat;
                totalWeight += weight;
            }
            double baseSatisfaction = (totalWeight > 0) ? weightedSum / totalWeight : 1.0;
            double totalVarietyBonus = calculateVarietyBonus(level, satisfactionMap);

            double taxSatisfaction = Math.max(0, Math.min(1, baseSatisfaction + totalVarietyBonus));
            double revenue = population * MONEY_PER_CITIZEN * taxSatisfaction;
            totalTaxRevenue += revenue;
            FLogger.ECONOMY.log("[" + faction.getName() + "] Population level " + level.name()
                    + ": base satisfaction " + baseSatisfaction
                    + " with total variety bonus " + totalVarietyBonus
                    + " -> tax revenue: " + revenue);
        }
        faction.getFAccount().deposit((int) totalTaxRevenue);
        FLogger.ECONOMY.log("[" + faction.getName() + "] Collected " + totalTaxRevenue + " money in taxes.");
    }

    private double calculateVarietyBonus(PopulationLevel level, Map<Resource, Double> satisfactionMap) {
        double totalVarietyBonus = 0.0;
        for (ResourceCategory category : ResourceCategory.values()) {
            Set<Resource> categoryResources = category.getResources();
            Set<Resource> levelCategoryResources = level.getResources().stream()
                    .filter(categoryResources::contains)
                    .collect(Collectors.toSet());
            if (!levelCategoryResources.isEmpty()) {
                long varietyCount = levelCategoryResources.stream()
                        .filter(res -> satisfactionMap.getOrDefault(res, 0.0) > 0)
                        .count();
                double varietyRatio = Math.min(1.0, (double) varietyCount / level.getMinimumVariety());
                double categoryBonus = varietyRatio * VARIETY_BONUS_FACTOR;
                totalVarietyBonus += categoryBonus;
            }
        }
        return totalVarietyBonus;
    }

    /**
     * Adjusts the population levels based on satisfaction and resource availability.
     * <p>
     * For each population level:
     * <ul>
     *   <li>If resource availability and housing allow, citizens can level up to the next population level.</li>
     *   <li>If overall happiness is negative, citizens may level down to the previous level.</li>
     *   <li>The unrest value is adjusted accordingly (reducing unrest when leveling up, increasing when leveling down).</li>
     * </ul>
     * After processing, if total unrest is positive, a revolt may be triggered.
     * </p>
     */
    private void calculatePopulationLevels() {
        double totalUnrest = faction.getUnrestLevel();
        for (PopulationLevel level : PopulationLevel.values()) {
            int currentPop = faction.getPopulation(level);
            PopulationLevel nextLevel = level.above();
            PopulationLevel previousLevel = level.below();

            // Attempt to level up
            if (nextLevel != level) {
                boolean canLevelUp = true;
                int popToLevelUp = Integer.MAX_VALUE;
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
                    popToLevelUp = (int) Math.min(popToLevelUp, popWeCanFeed);
                }
                if (canLevelUp && level.canLevelUp(storage) && popToLevelUp > 0) {
                    int levellingUp = Math.min(popToLevelUp, housingCapacity);
                    faction.getPopulation().put(level, currentPop - levellingUp);
                    faction.getPopulation().put(nextLevel, faction.getPopulation(nextLevel) + levellingUp);
                    totalUnrest -= levellingUp * nextLevel.getUnrestMultiplier();
                    FLogger.ECONOMY.log("[" + faction.getName() + "] Leveled up " + levellingUp
                            + " from " + level.name() + " to " + nextLevel.name());
                }
            }

            // Attempt to level down if happiness is negative
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
                    FLogger.ECONOMY.log("[" + faction.getName() + "] Leveled down " + popToLevelDown
                            + " from " + level.name() + " to " + previousLevel.name());
                }
            }
        }
        totalUnrest = totalUnrest * faction.getAttributeValue("unrest_multiplier", 1.0);
        faction.setUnrestLevel(totalUnrest);
        if (totalUnrest > 0) {
            spawnRevolt(faction, (int) (totalUnrest * UNREST_SPAWN_MULTIPLIER));
        }
    }

    /**
     * Adjusts the faction's level based on whether it meets the population and building requirements
     * for higher levels.
     */
    private void calculateFactionLevel() {
        for (FactionLevel level : FactionLevel.values()) {
            if (level.hasRequiredPopulationToLevelUpToThis(faction) && level.hasRequiredBuildingsToLevelUpToThis(faction)) {
                faction.setLevel(level);
                FLogger.ECONOMY.log("[" + faction.getName() + "] Leveled up to " + level.name());
            }
        }
    }

    /**
     * Adds or replaces a happiness modifier that influences overall satisfaction.
     *
     * @param modifier The happiness modifier to add or replace.
     */
    public void addOrReplaceHappinessModifier(HappinessModifier modifier) {
        happinessModifiers.removeIf(m -> m.name().equals(modifier.name()));
        happinessModifiers.add(modifier);
    }

    /**
     * Removes a happiness modifier by its name.
     *
     * @param name The name of the modifier to remove.
     */
    public void removeHappinessModifier(String name) {
        happinessModifiers.removeIf(m -> m.name().equals(name));
    }

    /**
     * Removes a given happiness modifier.
     *
     * @param modifier The modifier to remove.
     */
    public void removeHappinessModifier(HappinessModifier modifier) {
        happinessModifiers.remove(modifier);
    }

    /**
     * Spawns a revolt by randomly selecting chunks within the faction's core region and
     * spawning revolutionary entities.
     * <p>
     * The number of revolutions spawned depends on the current unrest level.
     * </p>
     *
     * @param faction  The faction where the revolt is to be spawned.
     * @param attempts The number of attempts (chunks) to try spawning revolutionaries.
     */
    public void spawnRevolt(Faction faction, int attempts) {
        MessageUtil.log("Spawning revolt for " + faction.getName());
        World world = faction.getCoreRegion().getWorld();
        Random random = new Random();
        int maxX = Integer.MIN_VALUE;
        int minX = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        for (LazyChunk chunk : faction.getCoreRegion().getChunks()) {
            maxX = Math.max(maxX, chunk.getX());
            minX = Math.min(minX, chunk.getX());
            maxZ = Math.max(maxZ, chunk.getZ());
            minZ = Math.min(minZ, chunk.getZ());
        }
        MessageUtil.log("minX: " + minX + ", maxX: " + maxX + ", minZ: " + minZ + ", maxZ: " + maxZ);
        for (int i = 0; i < attempts; i++) {
            int x = random.nextInt(minX, maxX + 1);
            int z = random.nextInt(minZ, maxZ + 1);
            MessageUtil.log("Chunk: " + x + ", " + z);
            if (world.isChunkLoaded(x, z)) {
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
