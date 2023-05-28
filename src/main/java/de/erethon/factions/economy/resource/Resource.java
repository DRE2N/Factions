package de.erethon.factions.economy.resource;

public enum Resource {

    BREAD,
    POTATOES,
    MEAT,
    FISH,
    CHEESE,
    WINE,
    BEER,
    CANDLES;


    public static Resource getByID(String id) {
        return valueOf(id.toUpperCase());
    }

    public String getID() {
        return name().toLowerCase();
    }



}
