package de.erethon.factions.building.attributes;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.TreeSet;

/**
 * @author Fyreum
 */
public abstract class AbstractFactionAttribute implements FactionAttribute {

    protected final Set<FactionAttributeModifier> modifiers = new TreeSet<>();
    protected double baseValue;
    protected double value;

    public AbstractFactionAttribute(double value) {
        this.baseValue = value;
        this.value = value;
    }

    @Override
    public Set<FactionAttributeModifier> getModifiers() {
        return modifiers;
    }

    @Override
    public @NotNull FactionAttribute apply() {
        double newValue = baseValue;
        for (FactionAttributeModifier modifier : modifiers) {
            newValue = modifier.apply(newValue);
        }
        value = newValue;
        return this;
    }

    @Override
    public double getBaseValue() {
        return baseValue;
    }

    @Override
    public void setBaseValue(double value) {
        baseValue = value;
    }

    @Override
    public double getValue() {
        return value;
    }
}
