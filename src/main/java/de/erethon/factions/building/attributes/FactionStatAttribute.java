package de.erethon.factions.building.attributes;

import java.util.HashSet;
import java.util.Set;

public class FactionStatAttribute implements FactionAttribute {
    private final Set<FactionAttributeModifier> modifiers = new HashSet<>();
    private double value;

    public FactionStatAttribute(double value) {
        this.value = value;
    }

    @Override
    public FactionAttributeType getType() {
        return FactionAttributeType.STAT;
    }

    @Override
    public Set<FactionAttributeModifier> getModifiers() {
        return modifiers;
    }

    @Override
    public double getValue() {
        return value;
    }

    @Override
    public FactionAttribute apply() {
        double newValue = value;
        for (FactionAttributeModifier modifier : modifiers) {
            newValue = modifier.apply(newValue);
        }
        value = newValue;
        return this;
    }
}
