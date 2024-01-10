package de.erethon.factions.building.attributes;

import java.util.Set;

/**
 * Represents a faction attribute, e.g. maximum population, resource production, etc.
 * @see FactionAttributeType
 *
 * Note that apply() should be called after all modifiers have been added to update the value.
 */
public interface FactionAttribute {
    FactionAttributeType getType();
    Set<FactionAttributeModifier> getModifiers();

    /**
     * @return the attribute with all modifiers applied
     */
    FactionAttribute apply();

    double getValue();
}
