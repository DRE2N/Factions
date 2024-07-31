package de.erethon.factions.building.attributes;

import de.erethon.factions.Factions;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;

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
        return new AttributeModifier(new NamespacedKey(Factions.get(), "factions-attribute"), value, attributeModifier.getOperation(), EquipmentSlotGroup.ANY);
    }
}
