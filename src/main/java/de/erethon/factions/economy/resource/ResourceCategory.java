package de.erethon.factions.economy.resource;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author Malfrador
 */
public enum ResourceCategory {

    BASIC_FOOD("Basic food", Resource.GRAIN, Resource.FISH, Resource.FRUIT),
    VEGETABLES("Vegetables", Resource.VEGETABLES),
    COAL("Coal", Resource.COAL),
    IRON("Iron", Resource.IRON),
    COPPER("Copper", Resource.COPPER),
    WAX("Wax", Resource.WAX),
    CANDLES("Candles", Resource.CANDLES),
    BREAD("Bread", Resource.BREAD),
    MEAT("Meat", Resource.MEAT),
    BEER("Beer", Resource.BEER),
    WINE("Wine", Resource.WINE),
    WOOD("Wood", Resource.WOOD),
    BARRELS("Barrels", Resource.BARRELS);

    private final String name;
    private final Set<Resource> resources;

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
