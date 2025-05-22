package de.erethon.factions.policy.handlers;

import de.erethon.factions.policy.FPolicyHandler;
import de.erethon.factions.building.attributes.FactionAttribute;
import de.erethon.factions.building.attributes.FactionAttributeModifier;
import de.erethon.factions.entity.FLegalEntity;
import org.bukkit.attribute.AttributeModifier;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public class ReduceTaxes implements FPolicyHandler {

    final FactionAttributeModifier modifier = new FactionAttributeModifier(-0.2, AttributeModifier.Operation.ADD_NUMBER, true);

    @Override
    public void apply(@NotNull FLegalEntity entity) {
        FactionAttribute attribute = entity.getOrCreateAttribute("tax_rate", 1.0);
        attribute.addModifier(modifier);
    }

    @Override
    public void remove(@NotNull FLegalEntity entity) {
        entity.removeModifier(modifier);
    }
}
