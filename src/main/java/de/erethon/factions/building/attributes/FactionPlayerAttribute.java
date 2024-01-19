package de.erethon.factions.building.attributes;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

import java.util.UUID;

/**
 * @author Fyreum, Malfrador
 */
public class FactionPlayerAttribute extends AbstractFactionAttribute {

    private final Attribute bukkitAttribute;
    private final AttributeModifier attributeModifier;

    public FactionPlayerAttribute(Attribute attribute, AttributeModifier modifier, double value) {
        super(value);
        this.bukkitAttribute = attribute;
        this.attributeModifier = modifier;
    }

    @Override
    public FactionAttributeType getType() {
        return FactionAttributeType.PLAYER_ATTRIBUTES;
    }

    public AttributeModifier getBukkitModifier() {
        return new AttributeModifier(UUID.randomUUID(), attributeModifier.getName(), value, attributeModifier.getOperation());
    }
}
