package de.erethon.factions.building.attributes;

import org.bukkit.attribute.AttributeModifier;

import java.util.UUID;

public record FactionAttributeModifier(UUID uuid, FactionAttribute attribute, double modifier, AttributeModifier.Operation operation) {
    public FactionAttributeType getType() {
        return attribute.getType();
    }

    public double apply(double value) {
        double newValue = value;
        switch (operation) {
            case ADD_NUMBER -> newValue += modifier;
            case ADD_SCALAR -> newValue *= modifier;
        }
        return newValue;
    }
}

