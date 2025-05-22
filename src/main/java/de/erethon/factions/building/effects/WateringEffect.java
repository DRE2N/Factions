package de.erethon.factions.building.effects;

import de.erethon.factions.Factions;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildSiteCoordinate;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.MoistureChangeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class WateringEffect extends BuildingEffect implements Listener {

    private final Factions plugin = Factions.get();
    private final int increasePerCycle;
    private final int cycleDuration;
    private final int maxDistance;
    private BukkitRunnable task;
    private final Set<Block> farmlandCache = new HashSet<>();

    public WateringEffect(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        cycleDuration = data.getInt("cycleDuration", 60);
        increasePerCycle = data.getInt("increasePerCycle", 1);
        maxDistance = data.getInt("maxDistance", 10);
    }

    @Override
    public void apply() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void remove() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    private void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.FARMLAND) {
            return;
        }
        if (site.getInteractive().getWorld() != event.getBlock().getWorld()) {
            return;
        }
        if (site.getInteractive().distance(event.getBlock().getLocation()) > maxDistance) {
            return;
        }
        Block block = event.getBlock();
        if (block.getBlockData() instanceof Farmland farmland) {
            farmland.setMoisture(7);
            block.setBlockData(farmland);
            block.getWorld().spawnParticle(Particle.SPLASH, block.getLocation().add(0.5, 1, 0.5), 3);
        }
    }

    @EventHandler
    private void onFarmlandChange(MoistureChangeEvent event) {
        if (event.getBlock().getType() != Material.FARMLAND) {
            return;
        }
        if (site.getInteractive().getWorld() != event.getBlock().getWorld()) {
            return;
        }
        if (site.getInteractive().distance(event.getBlock().getLocation()) > maxDistance) {
            return;
        }
        if (event.getNewState().getBlockData() instanceof Farmland newLand) {
            if (newLand.getMoisture() < 7) {
                event.setCancelled(true);
            }
        }
    }
}
