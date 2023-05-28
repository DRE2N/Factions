package de.erethon.factions.building;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.Set;

public class BuildingRespawnableBlockEntry {

    private final Material material;
    private final int amount;
    private final int respawnTimeMin;
    private final int respawnTimeMax;
    private final Set<Material> blocksToReplace = new HashSet<>();

    public BuildingRespawnableBlockEntry(ConfigurationSection section) {
        material = Material.valueOf(section.getString("material"));
        amount = section.getInt("amount");
        respawnTimeMin = section.getInt("respawnTimeMin");
        respawnTimeMax = section.getInt("respawnTimeMax");
        for (String key : section.getStringList("blocksToReplace")) {
            blocksToReplace.add(Material.valueOf(key));
        }
    }
}
