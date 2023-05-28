package de.erethon.factions.economy.resource;

import java.util.HashSet;
import java.util.Set;

public enum ResourceCategory {

    FOOD("Food"),
    GOODS("Goods"),
    LUXURY_GOODS("Luxury Goods"),
    MILITARY("Military"),
    CULTURE_RELIGION_SCIENCE("Culture, Religion, Science");

    private final String name;
    private Set<Resource> resources = new HashSet<>();

    ResourceCategory(String name, Resource... resources) {
        this.name = name;
        this.resources = Set.of(resources);
    }

    public String getName() {
        return name;
    }

    public Set<Resource> getResources() {
        return resources;
    }
}
