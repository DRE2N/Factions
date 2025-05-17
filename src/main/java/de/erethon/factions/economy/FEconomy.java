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
import net.kyori.adventure.text.Component;
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

    private final static double MONEY_PER_CITIZEN = 10.0;
    private final static double UNREST_SPAWN_MULTIPLIER = 1.5; // How many revolutionaries spawn per unrest point
    private static final double VARIETY_BONUS_FACTOR = 0.01; // Bonus per distinct resource
    private static final double REVOLT_THRESHOLD = 10.0; // Unrest level at which a revolt is triggered
    private static final double MAX_PERCENTAGE_TO_LEVEL_DOWN = 0.1; // Percentage of citizens that can level down in one cycle
    private static final double DEMOTION_EVENT_BASE_PENALTY_PER_CITIZEN = 0.1; // Base penalty for leveling down
    private static final double HAPPINESS_THRESHOLD_FOR_UNREST_DECAY = 0.6; // How high happiness needs to be to decay unrest
    private static final double BASE_UNREST_DECAY_RATE = 0.05; // Base decay rate
    private static final double ADDITIONAL_DECAY_BONUS_AT_MAX_HAPPINESS = 0.08; // Bonus decay rate at max happiness

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
            // Will be between 0.0 and 1.0
            double baseSatisfaction = (totalWeight > 0) ? weightedSum / totalWeight : 1.0;

            double additionalModifiers = 0;
            for (HappinessModifier modifier : happinessModifiers) {
                additionalModifiers += modifier.getModifier(level);
            }
            double totalVarietyBonus = calculateVarietyBonus(level, satisfactionMap);
            double rawHappiness = baseSatisfaction + additionalModifiers + totalVarietyBonus;
            double finalHappiness = Math.max(0.0, Math.min(1.0, rawHappiness));

            double currentHappiness = faction.getHappiness(level);
            double newHappiness = currentHappiness + (finalHappiness - currentHappiness) * 0.25; // Slow change so it doesn't spike too much
            faction.setHappiness(level, newHappiness);
            FLogger.ECONOMY.log(String.format("[%s] Happiness for %s: Base=%.2f, Modifiers=%.2f, VarietyBonus=%.2f -> Raw=%.2f -> Final=%.2f",
                    faction.getName(),
                    level.name(),
                    baseSatisfaction,
                    additionalModifiers,
                    totalVarietyBonus,
                    rawHappiness,
                    finalHappiness));
        }
    }

    /**
     * Calculates and collects tax revenue based on population happiness.
     * <p>
     * Revenue is based on the final, clamped happiness value for each population level,
     * which incorporates base resource satisfaction, variety bonuses, and all other
     * multiplicative happiness modifiers.
     * </p>
     * <pre>
     *     revenue = population * MONEY_PER_CITIZEN * final_happiness
     * </pre>
     */
    private void calculateTaxRevenue() {
        double totalTaxRevenue = 0.0;
        for (PopulationLevel level : PopulationLevel.values()) {
            int population = faction.getPopulation(level);
            double happinessForTax = faction.getHappiness(level);

            double revenue = population * MONEY_PER_CITIZEN * happinessForTax;
            totalTaxRevenue += revenue;

            FLogger.ECONOMY.log(String.format("[%s] Population level %s: Pop=%d, Happiness=%.3f -> Tax Revenue=%.2f",
                    faction.getName(),
                    level.name(),
                    population,
                    happinessForTax,
                    revenue));
        }
        faction.getFAccount().deposit((int) totalTaxRevenue);
        FLogger.ECONOMY.log(String.format("[%s] Collected %.2f money in taxes.",
                faction.getName(),
                totalTaxRevenue));
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
     *   <li>If overall happiness is very low, citizens may level down to the previous level.</li>
     *   <li>The unrest value is adjusted accordingly (reducing unrest when leveling up, increasing when leveling down).</li>
     * </ul>
     * After processing, if total unrest is positive, a revolt may be triggered.
     * </p>
     */
    private void calculatePopulationLevels() {
        double totalUnrest = faction.getUnrestLevel();
        for (PopulationLevel level : PopulationLevel.values()) {
            int initialPopAtThisLevel = faction.getPopulation(level);
            if (initialPopAtThisLevel == 0) {
                continue;
            }

            PopulationLevel nextLevel = level.above();
            PopulationLevel previousLevel = level.below();

            // --- Attempt to Level Up Population ---
            if (nextLevel != level) {
                int currentPopTryingToLevelUp = faction.getPopulation(level);

                if (currentPopTryingToLevelUp > 0) {
                    boolean canPotentiallyLevelUp = true;

                    // 1. Check Housing Capacity in the Next Level
                    FactionAttribute housingAttr = faction.getAttributes().get("housing_" + nextLevel.name().toLowerCase());
                    int housingStockNextLevel = (housingAttr != null) ? (int) housingAttr.getValue() : 0;
                    int popInNextLevel = faction.getPopulation(nextLevel);
                    int housingCapacityInNextLevel = Math.max(0, housingStockNextLevel - popInNextLevel);

                    if (housingCapacityInNextLevel == 0) {
                        canPotentiallyLevelUp = false;
                        FLogger.ECONOMY.log("[" + faction.getName() + "] No housing capacity in " + nextLevel.name() + " for " + level.name() + " to level up. Needed for pop > " + popInNextLevel + ", available stock: " + housingStockNextLevel);
                    }

                    // 2. Check General Faction/Level Prerequisites
                    if (canPotentiallyLevelUp && !level.canLevelUp(storage)) {
                        canPotentiallyLevelUp = false;
                        FLogger.ECONOMY.log("[" + faction.getName() + "] Level " + level.name() + " does not meet general criteria to level up its population (checked by " + level.name() + ".canLevelUp()).");
                    }

                    // 3. Determine Resource-Based Support Capacity for the Next Level
                    int popSupportedByNextLevelResources = Integer.MAX_VALUE; // Assume infinite if no resource needs
                    if (canPotentiallyLevelUp) {
                        if (nextLevel.getResources().isEmpty()) {
                            // If next level has no specific resource requirements for population, it's not a limiter.
                            FLogger.ECONOMY.log("[" + faction.getName() + "] Next level " + nextLevel.name() + " has no resource requirements to sustain population.");
                        } else {
                            for (Resource resource : nextLevel.getResources()) {
                                var consumptionInfo = nextLevel.getResourceConsumption(resource);
                                if (consumptionInfo == null) {
                                    FLogger.ECONOMY.log("[" + faction.getName() + "] Missing resource consumption info for " + resource.name() + " at level " + nextLevel.name());
                                    canPotentiallyLevelUp = false;
                                    break;
                                }
                                double minimumInStorageToLevelUp = consumptionInfo.minimumInStorageToLevelUp();

                                // Check 1: Absolute minimum amount of this resource required in storage
                                if (!storage.canAfford(resource, (int) minimumInStorageToLevelUp)) {
                                    canPotentiallyLevelUp = false;
                                    FLogger.ECONOMY.log("[" + faction.getName() + "] Cannot level up to " + nextLevel.name() + ": Insufficient " + resource.name() + " (need " + minimumInStorageToLevelUp + ", have " + storage.getResource(resource) + ").");
                                    break; // Stop checking other resources if one critical minimum is not met
                                }

                                // Check 2: How many new pops can be sustained by this resource's consumption rate
                                double consumptionPerPop = consumptionInfo.consumptionPerPop();
                                if (consumptionPerPop > 0) {
                                    double trulyAvailableForConsumption = Math.max(0, storage.getResource(resource) - minimumInStorageToLevelUp);
                                    double popThisResourceCanSupport = Math.floor(trulyAvailableForConsumption / consumptionPerPop);
                                    popSupportedByNextLevelResources = (int) Math.min(popSupportedByNextLevelResources, popThisResourceCanSupport);
                                }
                            }
                        }
                    }

                    if (canPotentiallyLevelUp && popSupportedByNextLevelResources == 0 && !nextLevel.getResources().isEmpty()) {
                        // This means minimums were met, but current stock vs consumption supports 0 new pops.
                        FLogger.ECONOMY.log("[" + faction.getName() + "] Resources for " + nextLevel.name() + " meet minimums, but cannot sustain any new population based on current stock and consumption rates.");
                        canPotentiallyLevelUp = false; // Cannot sustain anyone new
                    }


                    // 4. Calculate Final Number to Level Up
                    if (canPotentiallyLevelUp) {
                        int numToLevelUp = currentPopTryingToLevelUp; // Start with max available from current level
                        numToLevelUp = Math.min(numToLevelUp, housingCapacityInNextLevel); // Limited by housing
                        if (!nextLevel.getResources().isEmpty() || popSupportedByNextLevelResources != Integer.MAX_VALUE) {
                            // Calculate how many *additional* people the resources can support at the next level
                            int additionalResourceSupportedCapacity = Math.max(0, popSupportedByNextLevelResources - popInNextLevel);
                            numToLevelUp = Math.min(numToLevelUp, additionalResourceSupportedCapacity);
                        }
                        numToLevelUp = Math.max(0, numToLevelUp); // Ensure it's not negative

                        if (numToLevelUp > 0) {
                            faction.getPopulation().put(level, currentPopTryingToLevelUp - numToLevelUp);
                            faction.getPopulation().put(nextLevel, popInNextLevel + numToLevelUp);

                            double unrestImpactFromTierChange = numToLevelUp * (nextLevel.getUnrestMultiplier() - level.getUnrestMultiplier());
                            totalUnrest += unrestImpactFromTierChange;

                            FLogger.ECONOMY.log(String.format("[%s] Leveled up %d from %s (remaining: %d) to %s (total: %d). Unrest change: %.2f",
                                    faction.getName(), numToLevelUp,
                                    level.name(), faction.getPopulation(level),
                                    nextLevel.name(), faction.getPopulation(nextLevel),
                                    unrestImpactFromTierChange));
                        } else {
                            FLogger.ECONOMY.log("[" + faction.getName() + "] Conditions evaluated for " + level.name() + " to level up to " + nextLevel.name() + ", but calculated numToLevelUp is 0. (PopToTry: " + currentPopTryingToLevelUp + ", HousingCap: " + housingCapacityInNextLevel + ", ResSupported: " + (popSupportedByNextLevelResources == Integer.MAX_VALUE ? "N/A" : popSupportedByNextLevelResources) + ")");
                        }
                    }
                }
            }

            // --- Attempt to Level Down Population  ---
            int currentPopForLevelDown = faction.getPopulation(level); // Re-fetch current pop at this level, as it might have decreased due to level-ups
            if (previousLevel != level && currentPopForLevelDown > 0) {
                double happiness = faction.getHappiness(level);
                boolean needsToLevelDown = false;
                int popToLevelDown = 0;

                double levelDownThreshold = 0.15;
                if (happiness < levelDownThreshold) {
                    needsToLevelDown = true;
                    double severity = (levelDownThreshold - happiness) / levelDownThreshold; // Ranges from 0 (basically fine) to 1 (at 0 happiness)

                    popToLevelDown = (int) Math.ceil(currentPopForLevelDown * severity * MAX_PERCENTAGE_TO_LEVEL_DOWN);

                    popToLevelDown = Math.max(1, popToLevelDown); // Ensure at least one levels down if trigger met and pop > 0
                    popToLevelDown = Math.min(popToLevelDown, currentPopForLevelDown);
                }

                if (needsToLevelDown && popToLevelDown > 0) {
                    faction.getPopulation().put(level, currentPopForLevelDown - popToLevelDown);
                    faction.getPopulation().put(previousLevel, faction.getPopulation(previousLevel) + popToLevelDown);

                    double unrestImpactFromTierChange = popToLevelDown * (previousLevel.getUnrestMultiplier() - level.getUnrestMultiplier());
                    double demotionEventPenalty = popToLevelDown * DEMOTION_EVENT_BASE_PENALTY_PER_CITIZEN;

                    double totalUnrestChangeFromDemotion = unrestImpactFromTierChange + demotionEventPenalty;
                    totalUnrest += totalUnrestChangeFromDemotion;

                    FLogger.ECONOMY.log(String.format("[%s] Leveled down %d from %s (remaining: %d) to %s (total: %d) due to low happiness (%.2f). Unrest change: +%.2f (TierShift: %.2f, EventPenalty: %.2f)",
                            faction.getName(), popToLevelDown,
                            level.name(), faction.getPopulation(level),
                            previousLevel.name(), faction.getPopulation(previousLevel),
                            happiness, totalUnrestChangeFromDemotion, unrestImpactFromTierChange, demotionEventPenalty));
                }
            }
        }

        totalUnrest = Math.max(0, totalUnrest); // Unrest should not be negative
        totalUnrest = totalUnrest * faction.getAttributeValue("unrest_multiplier", 1.0); // Apply faction-wide unrest modifier
        faction.setUnrestLevel(totalUnrest);

        // Decay after calculating the new unrest level
        applyUnrestDecay();
        totalUnrest = faction.getUnrestLevel();

        // Spawn Revolt if unrest is significant
        if (totalUnrest > REVOLT_THRESHOLD) {
            int revoltAttempts = (int) Math.ceil(totalUnrest * UNREST_SPAWN_MULTIPLIER);
            if (revoltAttempts > 0) {
                FLogger.ECONOMY.log("[" + faction.getName() + "] High unrest (%.2f), attempting to spawn revolt with %d attempts." + faction.getName() + totalUnrest + revoltAttempts);
                spawnRevolt(faction, revoltAttempts);
            }
        } else if (totalUnrest > 0) {
            FLogger.ECONOMY.log("[" + faction.getName() + "] Current unrest level is %.2f (below revolt threshold of %.2f)." + faction.getName() + totalUnrest + REVOLT_THRESHOLD);
        }
    }

    /**
     * Applies decay to the faction's unrest level if average happiness is high enough.
     * Unrest decays by a percentage of its current value, influenced by overall happiness.
     */
    private void applyUnrestDecay() {
        double currentUnrest = faction.getUnrestLevel();
        if (currentUnrest <= 0) {
            return;
        }

        double totalHappinessSum = 0;
        int populatedLevelsCount = 0;
        for (PopulationLevel pLevel : PopulationLevel.values()) {
            if (faction.getPopulation(pLevel) > 0) {
                totalHappinessSum += faction.getHappiness(pLevel);
                populatedLevelsCount++;
            }
        }

        if (populatedLevelsCount == 0) {
            return;
        }
        double averageHappiness = totalHappinessSum / populatedLevelsCount;

        if (averageHappiness > HAPPINESS_THRESHOLD_FOR_UNREST_DECAY) {
            // If threshold is 0.6, averageHappiness 0.8 -> happinessEffectiveness is (0.8-0.6)/(1.0-0.6) = 0.2/0.4 = 0.5
            double happinessEffectiveness = (averageHappiness - HAPPINESS_THRESHOLD_FOR_UNREST_DECAY) / (1.0 - HAPPINESS_THRESHOLD_FOR_UNREST_DECAY);
            happinessEffectiveness = Math.max(0, Math.min(1.0, happinessEffectiveness)); // Clamp between 0 and 1

            double decayRate = BASE_UNREST_DECAY_RATE + (ADDITIONAL_DECAY_BONUS_AT_MAX_HAPPINESS * happinessEffectiveness);

            decayRate *= faction.getAttributeValue("unrest_decay_modifier", 1.0);

            double amountToDecay = currentUnrest * decayRate;
            double newUnrest = Math.max(0, currentUnrest - amountToDecay); // Ensure unrest doesn't go negative

            if (newUnrest < currentUnrest) {
                faction.setUnrestLevel(newUnrest);
                FLogger.ECONOMY.log(String.format("[%s] Unrest decayed by %.2f due to happiness (Avg: %.2f). New unrest: %.2f",
                        faction.getName(),
                        (currentUnrest - newUnrest),
                        averageHappiness,
                        newUnrest));
            }
        } else {
            FLogger.ECONOMY.log(String.format("[%s] Unrest did not decay. Average happiness (%.2f) is below threshold (%.2f).",
                    faction.getName(), averageHappiness, HAPPINESS_THRESHOLD_FOR_UNREST_DECAY));
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
                    int xInChunk = random.nextInt(16);
                    int zInChunk = random.nextInt(16);
                    int xInWorld = x * 16 + xInChunk;
                    int zInWorld = z * 16 + zInChunk;
                    Revolutionary rev = new Revolutionary(faction, world.getHighestBlockAt(xInWorld, zInWorld).getLocation());
                    ((org.bukkit.craftbukkit.CraftWorld) world).getHandle().addFreshEntity(rev);
                    MessageUtil.log("Spawned revolutionary at " + x + ", " + z);
                }
            }
        }
    }

    public Component getFittingCitizenGossip(PopulationLevel level) {
        double happiness = faction.getHappiness(level);
        Set<Resource> unhappyResources = new HashSet<>();
        for (Map.Entry<Resource, Double> entry : resourceSatisfaction.get(level).entrySet()) {
            if (entry.getValue() < 0.33) {
                unhappyResources.add(entry.getKey());
            }
        }
        if (happiness < 0.1) {
            return Component.translatable("factions.economy.gossip.veryUnhappy",
                    unhappyResources.stream().map(Resource::name).collect(Collectors.joining(", ")));
        } else if (happiness < 0.3) {
            return Component.translatable("factions.economy.gossip.unhappy",
                    unhappyResources.stream().map(Resource::name).collect(Collectors.joining(", ")));
        } else if (happiness < 0.5) {
            return Component.translatable("factions.economy.gossip.neutral",
                    unhappyResources.stream().map(Resource::name).collect(Collectors.joining(", ")));
        } else if (happiness < 0.7) {
            return Component.translatable("factions.economy.gossip.happy",
                    unhappyResources.stream().map(Resource::name).collect(Collectors.joining(", ")));
        } else if (happiness < 0.9) {
            return Component.translatable("factions.economy.gossip.veryHappy",
                    unhappyResources.stream().map(Resource::name).collect(Collectors.joining(", ")));
        } else {
            return Component.translatable("factions.economy.gossip.extremelyHappy",
                    unhappyResources.stream().map(Resource::name).collect(Collectors.joining(", ")));
        }
    }
}
