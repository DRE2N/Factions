package de.erethon.factions.building;

import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.region.RegionType;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class BuildingEffect {

    private String displayName = "";

    public BuildingEffect(Building building, ConfigurationSection section) {
        load(section);
    }

    public abstract void apply(@NotNull Faction faction);

    public abstract void remove(@NotNull Faction faction);

    public void load(ConfigurationSection section) {
        displayName = (String) section.get("displayName");
    }
}