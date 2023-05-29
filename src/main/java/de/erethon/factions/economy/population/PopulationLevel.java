package de.erethon.factions.economy.population;

import org.jetbrains.annotations.NotNull;

public enum PopulationLevel {

    BEGGAR(),
    PEASANT(),
    CITIZEN(),
    PATRICIAN(),
    NOBLEMEN();

    PopulationLevel() {
    }

    public @NotNull PopulationLevel above() {
        return switch (this) {
            case BEGGAR -> PEASANT;
            case PEASANT -> CITIZEN;
            case CITIZEN -> PATRICIAN;
            case PATRICIAN -> NOBLEMEN;
            case NOBLEMEN -> NOBLEMEN;
        };
    }

    public @NotNull PopulationLevel below() {
        return switch (this) {
            case BEGGAR -> BEGGAR;
            case PEASANT -> BEGGAR;
            case CITIZEN -> PEASANT;
            case PATRICIAN -> CITIZEN;
            case NOBLEMEN -> PATRICIAN;
        };
    }
}