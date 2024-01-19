package de.erethon.factions.building.attributes;

import de.erethon.factions.economy.resource.Resource;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Malfrador
 */
public class FactionResourceAttribute implements FactionAttribute {

    private final Set<FactionAttributeModifier> modifiers = new HashSet<>();
    private final Resource resource;
    private double value;

    public FactionResourceAttribute(Resource resource, double value) {
        this.resource = resource;
        this.value = value;
    }

    @Override
    public FactionAttributeType getType() {
        return FactionAttributeType.RESOURCE;
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

    public Resource getResource() {
        return resource;
    }

}
