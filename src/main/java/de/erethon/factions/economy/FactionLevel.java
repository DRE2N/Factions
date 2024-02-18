package de.erethon.factions.economy;

import de.erethon.factions.Factions;
import de.erethon.factions.building.Building;
import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.faction.Faction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * @author Malfrador
 */
public enum FactionLevel {
    HAMLET,
    VILLAGE,
    TOWN,
    CITY,
    METROPOLIS;

    private final Map<PopulationLevel, Integer> requiredPopulation;
    private final Set<Building> requiredBuildings;

    FactionLevel() {
        this.requiredPopulation = Factions.get().getFConfig().getRequiredPopulation(this);
        this.requiredBuildings = Factions.get().getFConfig().getRequiredBuildings(this);
    }


    public @NotNull Map<PopulationLevel, Integer> getRequiredPopulation() {
        return requiredPopulation;
    }

    public @NotNull Set<Building> getRequiredBuildings() {
        return requiredBuildings;
    }

    public boolean hasRequiredPopulationToLevelUpToThis(@NotNull Faction faction) {
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

    public boolean hasRequiredBuildingsToLevelUpToThis(@NotNull Faction faction) {
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

    public @Nullable FactionLevel next() {
        return switch (this) {
            case HAMLET -> TOWN;
            case TOWN -> VILLAGE;
            case VILLAGE -> CITY;
            case CITY -> METROPOLIS;
            default -> null;
        };
    }

    public Component displayName() {
        return Component.translatable("factions.economy.faction.level." + name().toLowerCase());
    }

    /* Statics */

    @Contract("null, null -> null; _, !null -> !null")
    public static @Nullable FactionLevel getByName(@Nullable String name, @Nullable FactionLevel def) {
        for (FactionLevel level : values()) {
            if (level.name().equalsIgnoreCase(name)) {
                return level;
            }
        }
        return def;
    }
}
