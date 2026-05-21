package de.erethon.factions.building;

public enum BuildingContainerType {
    INPUT("input_chest"),
    OUTPUT("output_chest");

    private final String positionKey;

    BuildingContainerType(String positionKey) {
        this.positionKey = positionKey;
    }

    public String positionKey() {
        return positionKey;
    }
}
