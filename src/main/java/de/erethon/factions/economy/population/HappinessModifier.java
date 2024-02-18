package de.erethon.factions.economy.population;

import net.kyori.adventure.text.Component;

import java.util.Map;

public record HappinessModifier(String name, Component display, Map<PopulationLevel, Double> modifiers) {
    public double getModifier(PopulationLevel level) {
        return modifiers.getOrDefault(level, 0.0);
    }
}
