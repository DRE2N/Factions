package de.erethon.factions.building.attributes;


/**
 * @author Fyreum, Malfrador
 */
public class FactionStatAttribute extends AbstractFactionAttribute {

    public FactionStatAttribute(double value) {
        super(value);
    }

    @Override
    public FactionAttributeType getType() {
        return FactionAttributeType.STAT;
    }
}
