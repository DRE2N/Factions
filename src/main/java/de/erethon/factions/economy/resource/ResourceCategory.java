package de.erethon.factions.economy.resource;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public enum ResourceCategory {

    FOOD("Food"),
    GOODS("Goods"),
    LUXURY_GOODS("Luxury Goods"),
    MILITARY("Military"),
    CULTURE_RELIGION_SCIENCE("Culture, Religion, Science");

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
