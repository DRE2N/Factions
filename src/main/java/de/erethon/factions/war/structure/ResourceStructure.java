package de.erethon.factions.war.structure;

import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionStructure;
import de.erethon.factions.region.WarRegion;
import io.papermc.paper.math.Position;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author Fyreum
 */
@SuppressWarnings("UnstableApiUsage")
public class ResourceStructure extends RegionStructure {

    protected ResourceType resourceType;

    public ResourceStructure(@NotNull WarRegion region, @NotNull ConfigurationSection config) {
        super(region, config);
    }

    public ResourceStructure(@NotNull WarRegion region, @NotNull ConfigurationSection config, @NotNull Position a, @NotNull Position b) {
        super(region, config, a, b);
    }

    @Override
    protected void load(@NotNull ConfigurationSection config) {
        this.resourceType = ResourceType.getByName(config.getString("resourceType"));
        if (region.getAlliance() == null) {
            return;
        }
    }


    private void proceedNeighbourProcesses() {
        if (region.getAlliance() == null) {
            return;
        }
        for (Region neighbour : region.getAdjacentRegions()) {
            if (region.getAlliance() != neighbour.getAlliance()) {
                continue;
            }
            if (!(neighbour instanceof WarRegion warNeighbour)) {
                continue;
            }
            Map<String, WarCastleStructure> castles = warNeighbour.getStructures(WarCastleStructure.class);
            if (castles.isEmpty()) {
                continue;
            }
        }
    }

    /* Classes */

    public enum ResourceType {

        CAMP(1.0),
        WAREHOUSE(1.6),
        ;

        private final double speedModifier;

        ResourceType(double speedModifier) {
            this.speedModifier = speedModifier;
        }

        /* Getters */

        public double getSpeedModifier() {
            return speedModifier;
        }

        public static ResourceType getByName(@Nullable String name) {
            if (name == null) {
                return null;
            }
            for (ResourceType value : values()) {
                if (value.name().equalsIgnoreCase(name)) {
                    return value;
                }
            }
            return null;
        }
    }
}
