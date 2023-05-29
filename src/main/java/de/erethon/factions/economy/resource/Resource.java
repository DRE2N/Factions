package de.erethon.factions.economy.resource;

import org.jetbrains.annotations.NotNull;

public enum Resource {

    BREAD,
    POTATOES,
    MEAT,
    FISH,
    CHEESE,
    WINE,
    BEER,
    CANDLES;

    public static Resource getById(@NotNull String id) {
        return valueOf(id.toUpperCase());
    }

    public @NotNull String getId() {
        return name().toLowerCase();
    }

}
