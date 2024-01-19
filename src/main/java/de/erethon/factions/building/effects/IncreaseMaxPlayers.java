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
public class IncreaseMaxPlayers extends BuildingEffect {

    private FactionAttributeModifier modifier;
    private final int amount = site.getInt("bonus", 5);
    FactionAttribute maxPlayers = faction.getAttribute("max_players");

    public IncreaseMaxPlayers(@NotNull BuildingEffectData effect, BuildSite site) {
        super(effect, site);
    }

    @Override
    public void apply() {
        modifier = new FactionAttributeModifier(UUID.randomUUID(), maxPlayers, amount, AttributeModifier.Operation.ADD_NUMBER);
        maxPlayers.getModifiers().add(modifier);
        maxPlayers.apply();
    }

    @Override
    public void remove() {
        maxPlayers.getModifiers().remove(modifier);
        maxPlayers.apply();
    }
}
