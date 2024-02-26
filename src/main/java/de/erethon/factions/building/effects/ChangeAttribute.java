package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.building.attributes.FactionAttribute;
import de.erethon.factions.building.attributes.FactionAttributeModifier;
import org.bukkit.attribute.AttributeModifier;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * @author Malfrador
 */
public class ChangeAttribute extends BuildingEffect {

    private FactionAttributeModifier modifier;
    private final int value;
    FactionAttribute attribute;

    public ChangeAttribute(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        value = data.getInt("value", 1);
        attribute = faction.getOrCreateAttribute(data.getString("attribute"), value);
        AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(data.getString("operation", "ADD_NUMBER").toUpperCase());
        modifier = new FactionAttributeModifier(UUID.randomUUID(), value, operation);
    }

    @Override
    public void apply() {
        attribute.addModifier(modifier);
    }

    @Override
    public void remove() {
        attribute.removeModifier(modifier);
    }
}
