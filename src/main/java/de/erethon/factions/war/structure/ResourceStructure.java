package de.erethon.factions.war.structure;

import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionStructure;
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
    protected BukkitTask restoreTask;

    public ResourceStructure(@NotNull Region region, @NotNull ConfigurationSection config) {
        super(region, config);
    }

    public ResourceStructure(@NotNull Region region, @NotNull ConfigurationSection config, @NotNull Position a, @NotNull Position b) {
        super(region, config, a, b);
    }

    @Override
    protected void load(@NotNull ConfigurationSection config) {
        this.resourceType = ResourceType.getByName(config.getString("resourceType"));
        if (region.getAlliance() == null) {
            return;
        }
        startRestoring();
    }

    public void startRestoring() {
        if (restoreTask != null) {
            return;
        }
        long interval = (long) (plugin.getFConfig().getWarCastleRestoreInterval() / resourceType.getSpeedModifier());
        restoreTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::proceedNeighbourProcesses, 0, interval);
    }

    public void stopRestoring() {
        if (restoreTask == null) {
            return;
        }
        restoreTask.cancel();
        restoreTask = null;
    }

    private void proceedNeighbourProcesses() {
        if (region.getAlliance() == null) {
            return;
        }
        for (Region neighbour : region.getAdjacentRegions()) {
            if (region.getAlliance() != neighbour.getAlliance()) {
                continue;
            }
            Map<String, WarCastleStructure> castles = neighbour.getStructures(WarCastleStructure.class);
            if (castles.isEmpty()) {
                continue;
            }
            for (WarCastleStructure castle : castles.values()) {
                castle.getRestoreProcess().proceed();
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
