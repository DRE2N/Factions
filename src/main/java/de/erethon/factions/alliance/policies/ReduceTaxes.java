package de.erethon.factions.alliance.policies;

import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.alliance.AlliancePolicyHandler;
import de.erethon.factions.building.attributes.FactionAttribute;
import de.erethon.factions.building.attributes.FactionAttributeModifier;
import org.bukkit.attribute.AttributeModifier;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public class ReduceTaxes implements AlliancePolicyHandler {

    final FactionAttributeModifier modifier = new FactionAttributeModifier(0.8, AttributeModifier.Operation.ADD_SCALAR);

    @Override
    public void apply(@NotNull Alliance alliance) {
        FactionAttribute attribute = alliance.getOrCreateAttribute("tax_rate", 1.0);
        attribute.addModifier(modifier);
    }

    @Override
    public void remove(@NotNull Alliance alliance) {
        alliance.removeModifier(modifier);
    }
}
