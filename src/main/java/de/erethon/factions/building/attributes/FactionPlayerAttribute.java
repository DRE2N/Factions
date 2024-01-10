package de.erethon.factions.building.attributes;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FactionPlayerAttribute implements FactionAttribute {

    private  final Set<FactionAttributeModifier> modifiers = new HashSet<>();
    private final Attribute bukkitAttribute;
    private final AttributeModifier attributeModifier;
    private double value;

    public FactionPlayerAttribute(Attribute attribute, AttributeModifier modifier, double value) {
        this.bukkitAttribute = attribute;
        this.attributeModifier = modifier;
        this.value = value;
    }

    @Override
    public FactionAttributeType getType() {
        return FactionAttributeType.PLAYER_ATTRIBUTES;
    }

    @Override
    public Set<FactionAttributeModifier> getModifiers() {
        return null;
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

    public AttributeModifier getBukkitModifier() {
        return new AttributeModifier(UUID.randomUUID(), attributeModifier.getName(), value, attributeModifier.getOperation());
    }
}
