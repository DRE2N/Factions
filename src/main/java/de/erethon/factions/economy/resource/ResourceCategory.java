package de.erethon.factions.economy.resource;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

public enum ResourceCategory {

    FOOD("Food", Resource.FISH, Resource.GRAIN, Resource.MEAT, Resource.SPICES, Resource.WINE, Resource.BREAD, Resource.CHEESE, Resource.BEER, Resource.FRUIT, Resource.VEGETABLES),
    GOODS("Goods", Resource.TOOLS, Resource.CLOTH),
    LUXURY_GOODS("Luxury Goods", Resource.JEWELRY, Resource.FURNITURE),
    MILITARY("Military", Resource.HORSES, Resource.SIEGE_EQUIPMENT, Resource.WEAPONS, Resource.ARMOR),
    CULTURE_RELIGION_SCIENCE("Culture, Religion, Science", Resource.BOOKS, Resource.PAPER, Resource.INK, Resource.CANDLES, Resource.MITHRIL);

    private final String name;
    private Set<Resource> resources;

    ResourceCategory(String name, Resource... resources) {
        this.name = name;
        this.resources = Set.of(resources);
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull Set<Resource> getResources() {
        return resources;
    }
}
