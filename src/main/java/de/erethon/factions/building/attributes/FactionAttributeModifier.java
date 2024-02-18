package de.erethon.factions.building.attributes;

import org.bukkit.attribute.AttributeModifier;

import java.util.UUID;

/**
 * @author Malfrador
 */
public record FactionAttributeModifier(UUID uuid, double modifier, AttributeModifier.Operation operation) {

    public double apply(double value) {
        double newValue = value;
        switch (operation) {
            case ADD_NUMBER -> newValue += modifier;
            case ADD_SCALAR -> newValue *= modifier;
        }
        return newValue;
    }
}

