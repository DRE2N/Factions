package de.erethon.factions.building.attributes;

import org.bukkit.attribute.AttributeModifier;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * @author Malfrador
 */
public record FactionAttributeModifier(@NotNull UUID uuid, double modifier, @NotNull AttributeModifier.Operation operation, boolean paydayPersistent) implements Comparable<FactionAttributeModifier> {

    public FactionAttributeModifier(double modifier, @NotNull AttributeModifier.Operation operation) {
        this(UUID.randomUUID(), modifier, operation, false);
    }

    public FactionAttributeModifier(double modifier, @NotNull AttributeModifier.Operation operation, boolean paydayPersistent) {
        this(UUID.randomUUID(), modifier, operation, paydayPersistent);
    }

    public double apply(double value) {
        double newValue = value;
        switch (operation) {
            case ADD_NUMBER -> newValue += modifier;
            case ADD_SCALAR -> newValue *= modifier;
            case MULTIPLY_SCALAR_1 -> newValue *= 1 + modifier;
        }
        return newValue;
    }

    public boolean isPaydayPersistent() {
        return paydayPersistent;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof FactionAttributeModifier other)) return false;
        return uuid.equals(other.uuid);
    }

    @Override
    public int compareTo(@NotNull FactionAttributeModifier o) {
        return Integer.compare(operation.ordinal(), o.operation.ordinal());
    }
}

