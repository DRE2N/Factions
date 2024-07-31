package de.erethon.factions.building.effects;

import de.erethon.factions.Factions;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildSiteCoordinate;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class WateringEffect extends BuildingEffect {

    private final Factions plugin = Factions.get();
    private final int increasePerCycle;
    private final int cycleDuration;
    private final int maxDistance;
    private BukkitRunnable task;

    public WateringEffect(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        cycleDuration = data.getInt("cycleDuration", 60);
        increasePerCycle = data.getInt("increasePerCycle", 1);
        maxDistance = data.getInt("maxDistance", 10);
    }

    @Override
    public void apply() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                water();
            }
        };
        task.runTaskTimer(plugin, 0, cycleDuration * 20L);
    }

    private void water() {
        for (BuildSiteCoordinate coord : site.getCoordinatesFor(Material.FARMLAND)) {
            World world = site.getInteractive().getWorld();
            if (coord.distance(site.getInteractive()) > maxDistance) {
                continue;
            }
            int chunkX = coord.x() >> 4;
            int chunkZ = coord.z() >> 4;
            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                continue;
            }
            Block block = world.getBlockAt(coord.x(), coord.y(), coord.z());
            if (block.getType() != Material.FARMLAND) {
                continue;
            }
            Farmland farmland = (Farmland) block.getBlockData();
            if (farmland.getMoisture() < 7) {
                farmland.setMoisture(farmland.getMoisture() + increasePerCycle);
                block.setBlockData(farmland);
            }
        }
    }

    @Override
    public void remove() {
        task.cancel();
    }
}
