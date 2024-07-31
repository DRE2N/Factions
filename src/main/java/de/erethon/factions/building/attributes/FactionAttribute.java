package de.erethon.factions.building.attributes;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Represents a faction attribute, e.g. maximum population, resource production, etc.
 * @see FactionAttributeType
 *
 * Note that apply() should be called after all modifiers have been added to update the value.
 *
 * @author Malfrador
 */
public interface FactionAttribute {
    FactionAttributeType getType();
    Set<FactionAttributeModifier> getModifiers();

    /**
     * @return the attribute with all modifiers applied
     */
    @NotNull FactionAttribute apply();

    double getBaseValue();

    void setBaseValue(double value);

    double getValue();

    default void addModifier(FactionAttributeModifier modifier) {
        getModifiers().add(modifier);
        apply();
    }

    default void removeModifier(FactionAttributeModifier modifier) {
        getModifiers().remove(modifier);
        apply();
    }
}
