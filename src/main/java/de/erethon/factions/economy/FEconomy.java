package de.erethon.factions.economy;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.attributes.FactionAttribute;
import de.erethon.factions.building.attributes.FactionAttributeModifier;
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

import java.util.Collections;
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
    private static final double VARIETY_BONUS_FACTOR = 0.01; // Bonus per distinct resource
    private static final double MAX_PERCENTAGE_TO_LEVEL_DOWN = 0.1; // Percentage of citizens that can level down in one cycle
    private static final double DEMOTION_EVENT_BASE_PENALTY_PER_CITIZEN = 0.1; // Base penalty for leveling down
    private static final double HAPPINESS_THRESHOLD_FOR_UNREST_DECAY = 0.6; // How high happiness needs to be to decay unrest
    private static final double BASE_UNREST_DECAY_RATE = 0.05; // Base decay rate
    private static final double ADDITIONAL_DECAY_BONUS_AT_MAX_HAPPINESS = 0.08; // Bonus decay rate at max happiness
    private static final double MAX_UNREST_FOR_PENALTY_CALC = 50.0; // Unrest beyond this value doesn't increase the penalty
    private static final double MAX_HAPPINESS_PENALTY_FROM_UNREST = 0.25; // Max happiness reduction (e.g., -0.25 happiness)
    private static final double UNREST_PENALTY_EFFECTIVENESS_THRESHOLD = 1.0;
    public static final double REVOLT_THRESHOLD = 25.0; // Unrest level at which a revolt is triggered
    public final static double UNREST_SPAWN_MULTIPLIER = 1.2; // How many revolutionaries spawn per unrest point
    private static final int REVOLT_HOTSPOT_RADIUS = 1;
    private static final int MAX_REVOLT_EPICENTER_RETRIES = 10;
    public final static double UNREST_REDUCTION_FOR_REVOLUTIONARY_DEATH = 1.0;

    private final Faction faction;
    private final FStorage storage;
    private final Set<HappinessModifier> happinessModifiers = new HashSet<>();

    /**
     * Stores the per-resource satisfaction ratios (0.0 to 1.0) for each population level.
     * This is calculated during resource consumption.
     */
    private final Map<PopulationLevel, Map<Resource, Double>> resourceSatisfaction = new HashMap<>();
    /**
     * Stores the last consumption values for each population level. This is mostly for user feedback.
     */
    private final Map<PopulationLevel, Map<Resource, Double>> lastConsumption = new HashMap<>();
    /**
     * Stores the last production values for each resource. This is mostly for user feedback.
     */
    private final Map<Resource, Double> lastProduction = new HashMap<>();

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
        // Reset all modifiers for the resources. Buildings will add their own modifiers in the payday cycle.
        for (Map.Entry<String, FactionAttribute> entry : faction.getAttributes().entrySet()) {
            if (entry.getValue() instanceof FactionResourceAttribute attribute) {
                Set<FactionAttributeModifier> toRemove = new HashSet<>();
                for (FactionAttributeModifier modifier : attribute.getModifiers()) {
                    if (modifier.isPaydayPersistent()) {
                        continue;
                    }
                    toRemove.add(modifier);
                }
                toRemove.forEach(attribute::removeModifier);
            }
        }

        doBuildingStuff();

        // Produce resources from buildings. We do this via modifiers so that we can have production chains
        for (Map.Entry<String, FactionAttribute> entry : faction.getAttributes().entrySet()) {
            if (entry.getValue() instanceof FactionResourceAttribute attribute) {
                Resource resource = attribute.getResource();
                double factor = faction.getAttributeValue("production_rate", 1.0);
                double amount = attribute.apply().getValue() * factor;
                storage.addResource(resource, (int) amount);
                lastProduction.put(resource, amount);
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
                lastConsumption.computeIfAbsent(level, k -> new HashMap<>()).put(resource, Math.min(available, required));
                FLogger.ECONOMY.log("[" + faction.getName() + "] Consumed " + Math.min(available, required)
                        + " of " + resource.name() + " for " + currentPop + " " + level.name()
                        + " population. Satisfaction: " + satisfaction + " Available: " + available + " / Required: " + required);
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
            if (faction.getPopulation(level) == 0) {
                faction.setHappiness(level, 0.0); // Just so there is always a value here
                resourceSatisfaction.put(level, new HashMap<>());
                continue;
            }
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
            if (Double.isNaN(baseSatisfaction)) {
                FLogger.ECONOMY.log(String.format("[%s] WARNING: Base satisfaction for %s is NaN. Defaulting to 0.", faction.getName(), level.name()));
                baseSatisfaction = 0.0;
            }

            double additionalModifiers = 0;
            for (HappinessModifier modifier : happinessModifiers) {
                additionalModifiers += modifier.getModifier(level);
            }
            double totalVarietyBonus = calculateVarietyBonus(level, satisfactionMap);
            if (Double.isNaN(totalVarietyBonus)) {
                FLogger.ECONOMY.log(String.format("[%s] WARNING: Variety bonus for %s is NaN. Defaulting to 0.", faction.getName(), level.name()));
                totalVarietyBonus = 0.0;
            }
            double rawHappiness = baseSatisfaction + additionalModifiers + totalVarietyBonus;

            double currentFactionUnrest = faction.getUnrestLevel();
            if (currentFactionUnrest > UNREST_PENALTY_EFFECTIVENESS_THRESHOLD) {
                double effectiveUnrest = Math.min(currentFactionUnrest, MAX_UNREST_FOR_PENALTY_CALC) - UNREST_PENALTY_EFFECTIVENESS_THRESHOLD;
                double effectiveRange = MAX_UNREST_FOR_PENALTY_CALC - UNREST_PENALTY_EFFECTIVENESS_THRESHOLD;

                double unrestFactor = 0.0;
                if (effectiveRange > 0) {
                    unrestFactor = Math.max(0.0, effectiveUnrest / effectiveRange);
                } else if (effectiveUnrest > 0) { // If threshold == max, any unrest above threshold gives full penalty
                    unrestFactor = 1.0;
                }

                double unrestHappinessPenalty = unrestFactor * MAX_HAPPINESS_PENALTY_FROM_UNREST;

                if (!Double.isNaN(rawHappiness)) {
                    rawHappiness -= unrestHappinessPenalty;
                }

                if (unrestHappinessPenalty > 0) {
                    FLogger.ECONOMY.log(String.format("[%s] Applied unrest happiness penalty of %.3f to %s (Faction Unrest: %.2f)",
                            faction.getName(), unrestHappinessPenalty, level.name(), currentFactionUnrest));
                }
            }

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
            if (Double.isNaN(happinessForTax)) {
                FLogger.ECONOMY.log(String.format("[%s] Happiness for tax calculation for %s is NaN. Tax revenue for this level will be 0.", faction.getName(), level.name()));
                happinessForTax = 0.0;
            }

            double revenue = population * MONEY_PER_CITIZEN * happinessForTax;
            totalTaxRevenue += revenue;
            if (Double.isNaN(totalTaxRevenue)) {
                FLogger.ECONOMY.log(String.format("[%s] Total tax revenue became NaN. Setting to 0.", faction.getName()));
                totalTaxRevenue = 0.0;
            }


            FLogger.ECONOMY.log(String.format("[%s] Taxation: Population level %s: Pop=%d, Happiness=%.3f -> Tax Revenue=%.2f",
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
                double minVarietyForLevel = level.getMinimumVariety();
                double varietyRatio = 0.0;
                if (minVarietyForLevel > 0) { // Only calculate a ratio if a minimum is defined
                    varietyRatio = Math.min(1.0, (double) varietyCount / minVarietyForLevel);
                }
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
                FLogger.ECONOMY.log(String.format("[%s] High unrest (%.2f), attempting to spawn revolt with %d attempts.",
                        faction.getName(),
                        totalUnrest,
                        revoltAttempts));
                spawnRevolt(faction, Math.min(revoltAttempts, 100));
            }
        } else if (totalUnrest > 0) {
            FLogger.ECONOMY.log(String.format("[%s] Current unrest level is %.2f (below revolt threshold of %.2f).",
                    faction.getName(),
                    totalUnrest,
                    REVOLT_THRESHOLD));
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

    public  Map<PopulationLevel, Map<Resource, Double>> getResourceSatisfaction() {
        if (resourceSatisfaction.isEmpty()) {
            for (PopulationLevel level : PopulationLevel.values()) {
                resourceSatisfaction.put(level, new HashMap<>());
            }
        }
        return resourceSatisfaction;
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
        Factions.log("Spawning revolt for " + faction.getName() + " with " + attempts + " spawn groups.");
        if (faction.hasOngoingRevolt()) {
            FLogger.ECONOMY.log("[" + faction.getName() + "] Cannot spawn revolt, already has an ongoing revolt.");
            return;
        }
        World world = faction.getCoreRegion().getWorld();
        if (world == null) {
            FLogger.ECONOMY.log("[" + faction.getName() + "] Cannot spawn revolt, world is null for core region.");
            return;
        }
        Random random = new Random();

        Set<LazyChunk> coreChunks = faction.getCoreRegion().getChunks();
        if (coreChunks.isEmpty()) {
            FLogger.ECONOMY.log("[" + faction.getName() + "] Cannot spawn revolt, faction has no core chunks.");
            return;
        }

        LazyChunk epicenterChunk = null;
        Set<LazyChunk> potentialSpawnChunksInHotspot = new HashSet<>();
        int epicenterX = 0;
        int epicenterZ = 0;
        int retriesForEpicenter = 0;
        while (retriesForEpicenter < MAX_REVOLT_EPICENTER_RETRIES) {
            potentialSpawnChunksInHotspot.clear();
            epicenterChunk = coreChunks.stream().skip(random.nextInt(coreChunks.size())).findFirst().orElse(null);
            if (epicenterChunk == null) {
                FLogger.ECONOMY.log("[" + faction.getName() + "] Could not select an epicenter chunk candidate.");
                return;
            }

            if (!world.isChunkLoaded(epicenterChunk.getX(), epicenterChunk.getZ())) {
                FLogger.ECONOMY.log("[" + faction.getName() + "] Epicenter candidate chunk " + epicenterChunk.getX() + "," + epicenterChunk.getZ() + " is not loaded. Retrying...");
                retriesForEpicenter++;
                epicenterChunk = null;
                continue;
            }

            epicenterX = epicenterChunk.getX();
            epicenterZ = epicenterChunk.getZ();
            FLogger.ECONOMY.log("[" + faction.getName() + "] Revolt epicenter chosen at loaded chunk: " + epicenterX + ", " + epicenterZ);

            for (LazyChunk coreChunk : coreChunks) {
                int dx = Math.abs(coreChunk.getX() - epicenterX);
                int dz = Math.abs(coreChunk.getZ() - epicenterZ);
                if (dx <= REVOLT_HOTSPOT_RADIUS && dz <= REVOLT_HOTSPOT_RADIUS) {
                    potentialSpawnChunksInHotspot.add(coreChunk);
                }
            }

            boolean hotspotHasLoadedChunk = false;
            for (LazyChunk hotspotChunk : potentialSpawnChunksInHotspot) {
                if (world.isChunkLoaded(hotspotChunk.getX(), hotspotChunk.getZ())) {
                    hotspotHasLoadedChunk = true;
                    break;
                }
            }

            if (hotspotHasLoadedChunk) {
                break;
            } else {
                FLogger.ECONOMY.log("[" + faction.getName() + "] Epicenter " + epicenterX + "," + epicenterZ + " is loaded, but no chunks in its hotspot are loaded (or hotspot is empty). Retrying epicenter...");
                retriesForEpicenter++;
                epicenterChunk = null;
            }
        }

        if (epicenterChunk == null || potentialSpawnChunksInHotspot.isEmpty()) {
            FLogger.ECONOMY.log("[" + faction.getName() + "] Could not find a suitable loaded epicenter/hotspot after " + retriesForEpicenter + " retries. Aborting revolt spawn.");
            return;
        }

        FLogger.ECONOMY.log("[" + faction.getName() + "] Found " + potentialSpawnChunksInHotspot.size() + " potential chunks in the hotspot for spawning around epicenter " + epicenterX + "," + epicenterZ);

        int spawnedGroups = 0;
        for (int i = 0; i < attempts; i++) {
            if (potentialSpawnChunksInHotspot.isEmpty()) break;

            LazyChunk targetChunk = potentialSpawnChunksInHotspot.stream()
                    .skip(random.nextInt(potentialSpawnChunksInHotspot.size()))
                    .findFirst()
                    .orElse(null);

            if (targetChunk == null) continue;

            int chunkX = targetChunk.getX();
            int chunkZ = targetChunk.getZ();

            if (world.isChunkLoaded(chunkX, chunkZ)) {
                int amount = random.nextInt(2, 8);
                FLogger.ECONOMY.log("[" + faction.getName() + "] Attempting to spawn " + amount + " revolutionaries in loaded chunk: " + chunkX + ", " + chunkZ);
                for (int j = 0; j < amount; j++) {
                    int xInChunkBlocks = random.nextInt(16);
                    int zInChunkBlocks = random.nextInt(16);
                    int xInWorld = chunkX * 16 + xInChunkBlocks;
                    int zInWorld = chunkZ * 16 + zInChunkBlocks;
                    org.bukkit.Location spawnLocation = world.getHighestBlockAt(xInWorld, zInWorld).getLocation().add(0.5, 0, 0.5);

                    Revolutionary rev = new Revolutionary(faction, spawnLocation);
                    ((org.bukkit.craftbukkit.CraftWorld) world).getHandle().addFreshEntity(rev);
                    FLogger.ECONOMY.log("[" + faction.getName() + "] Spawned revolutionary at world coords: " + spawnLocation.getX() + ", " + spawnLocation.getY() + ", " + spawnLocation.getZ() + " (Chunk: " + chunkX + "," + chunkZ + ")");
                }
                spawnedGroups++;
            } else {
                FLogger.ECONOMY.log("[" + faction.getName() + "] Chunk " + chunkX + ", " + chunkZ + " in hotspot is not loaded. Skipping spawn for this chunk.");
            }
        }
        if (spawnedGroups == 0 && attempts > 0) {
            FLogger.ECONOMY.log("[" + faction.getName() + "] Revolt attempted with " + attempts + " groups, but no revolutionaries were spawned (likely no loaded chunks in hotspot).");
        } else {
            faction.setOngoingRevolt(true);
        }
    }

    public double getLastProduction(Resource resource) {
        return lastProduction.getOrDefault(resource, 0.0);
    }

    public double getLastConsumption(PopulationLevel level, Resource resource) {
        return lastConsumption.getOrDefault(level, new HashMap<>()).getOrDefault(resource, 0.0);
    }

    public Set<Resource> getAllConsumedResourcesForLevel(PopulationLevel level) {
        Map<Resource, Double> consumed = lastConsumption.get(level);
        if (consumed == null) {
            return Collections.emptySet();
        }
        Set<Resource> consumedResources = new HashSet<>();
        for (Resource resource : consumed.keySet()) {
            if (consumed.get(resource) > 0) {
                consumedResources.add(resource);
            }
        }
        return consumedResources;
    }

    public double getLastTotalConsumption(Resource resource) {
        double totalConsumption = 0.0;
        for (Map<Resource, Double> consumption : lastConsumption.values()) {
            totalConsumption += consumption.getOrDefault(resource, 0.0);
        }
        return totalConsumption;
    }

    public Component getFittingCitizenGossip(PopulationLevel level) {
        double happiness = faction.getHappiness(level);
        Set<Resource> unhappyResources = new HashSet<>();
        if (resourceSatisfaction.get(level) == null) {
            return Component.empty();
        }
        for (Map.Entry<Resource, Double> entry : resourceSatisfaction.get(level).entrySet()) {
            if (entry.getValue() < 0.33) {
                unhappyResources.add(entry.getKey());
            }
        }
        if (happiness < 0.1) {
            return Component.translatable("factions.economy.gossip.veryUnhappy",
                    Component.text(unhappyResources.stream().map(Resource::name).collect(Collectors.joining(", ")).toLowerCase()));
        } else if (happiness < 0.3) {
            return Component.translatable("factions.economy.gossip.unhappy",
                    Component.text(unhappyResources.stream().map(Resource::name).collect(Collectors.joining(", ")).toLowerCase()));
        } else if (happiness < 0.5) {
            return Component.translatable("factions.economy.gossip.neutral",
                    Component.text(unhappyResources.stream().map(Resource::name).collect(Collectors.joining(", ")).toLowerCase()));
        } else if (happiness < 0.7) {
            return Component.translatable("factions.economy.gossip.happy",
                    Component.text(unhappyResources.stream().map(Resource::name).collect(Collectors.joining(", ")).toLowerCase()));
        } else if (happiness < 0.9) {
            return Component.translatable("factions.economy.gossip.veryHappy",
                    Component.text(unhappyResources.stream().map(Resource::name).collect(Collectors.joining(", ")).toLowerCase()));
        } else {
            return Component.translatable("factions.economy.gossip.extremelyHappy",
                    Component.text(unhappyResources.stream().map(Resource::name).collect(Collectors.joining(", ")).toLowerCase()));
        }
    }
}
