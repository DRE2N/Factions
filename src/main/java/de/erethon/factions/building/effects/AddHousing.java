package de.erethon.factions.building.effects;

import de.erethon.factions.Factions;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.building.attributes.FactionAttribute;
import de.erethon.factions.building.attributes.FactionAttributeModifier;
import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.economy.population.entities.Citizen;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class AddHousing extends BuildingEffect {

    private final FactionAttributeModifier modifier;
    private final FactionAttribute attribute;
    private final double citizenSpawnChance;
    private final PopulationLevel level;

    public AddHousing(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        this.level = PopulationLevel.valueOf(data.getString("level", "PEASANT").toUpperCase());
        attribute = faction.getOrCreateAttribute("housing_" + data.getString("level"), 1.0);
        modifier = new FactionAttributeModifier(data.getInt("amount", 0), AttributeModifier.Operation.ADD_NUMBER);
        citizenSpawnChance = data.getDouble("citizenSpawnChance", 0.1);
    }

    @Override
    public void apply() {
        attribute.addModifier(modifier);
    }

    @Override
    public void remove() {
        attribute.removeModifier(modifier);
    }

    @Override
    public void onChunkLoad() {
        Random rand = new Random();
        if (rand.nextDouble() < citizenSpawnChance) {
            // Spawn at a random location in the site
            int x = rand.nextInt((int) site.getCorner().distance(site.getOtherCorner()) + 1) - (int) site.getCorner().distance(site.getOtherCorner()) / 2;
            int z = rand.nextInt((int) site.getCorner().distance(site.getOtherCorner()) + 1) - (int) site.getCorner().distance(site.getOtherCorner()) / 2;
            Location baseLocation = site.getCenter().add(x, 0, z);
            int nearby = baseLocation.getNearbyEntitiesByType(Villager.class, 32).size();
            if (nearby > 4) {
                return;
            }
            Location highestBlock = baseLocation.toHighestLocation(HeightMap.MOTION_BLOCKING_NO_LEAVES);
            highestBlock.setY(highestBlock.getY() + 1);
            Factions.log("Spawning citizen for faction " + faction.getName() + " at " + highestBlock + "(x + " + x + ", z + " + z + ")");
            new Citizen(faction, highestBlock, level);
        }
    }
}
