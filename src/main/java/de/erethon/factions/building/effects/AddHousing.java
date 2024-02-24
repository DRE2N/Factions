package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.building.attributes.FactionAttribute;
import de.erethon.factions.building.attributes.FactionAttributeModifier;
import org.bukkit.attribute.AttributeModifier;
import org.jetbrains.annotations.NotNull;

public class AddHousing extends BuildingEffect {

    private final FactionAttributeModifier modifier;
    private final FactionAttribute attribute;

    public AddHousing(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        attribute = faction.getOrCreateAttribute("housing_" + data.getString("level"), 0.0);
        modifier = new FactionAttributeModifier(data.getInt("amount", 0), AttributeModifier.Operation.ADD_NUMBER);
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
