package de.erethon.factions.building.attributes;

import de.erethon.factions.economy.resource.Resource;

/**
 * @author Fyreum, Malfrador
 */
public class FactionResourceAttribute extends AbstractFactionAttribute {

    private final Resource resource;

    public FactionResourceAttribute(Resource resource, double value) {
        super(value);
        this.resource = resource;
    }

    @Override
    public FactionAttributeType getType() {
        return FactionAttributeType.RESOURCE;
    }

    public Resource getResource() {
        return resource;
    }

}
