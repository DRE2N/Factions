package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.building.attributes.FactionAttribute;
import de.erethon.factions.building.attributes.FactionAttributeModifier;
import org.bukkit.attribute.AttributeModifier;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class AddHousingEffect extends BuildingEffect {

    private final FactionAttributeModifier modifier;
    private final FactionAttribute attribute;

    public AddHousingEffect(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        attribute = faction.getAttribute("housing_" + data.getString("level"));
        modifier = new FactionAttributeModifier(UUID.randomUUID(), data.getInt("amount", 0), AttributeModifier.Operation.ADD_NUMBER);
    }

    @Override
    public void apply() {
        faction.addModifier(attribute, modifier);
    }

    @Override
    public void remove() {
        faction.removeModifier(attribute, modifier);
    }
}
