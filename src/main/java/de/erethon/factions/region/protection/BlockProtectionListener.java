package de.erethon.factions.region.protection;

import de.erethon.factions.Factions;
import de.erethon.factions.entity.Relation;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

/**
 * @author Fyreum
 */
public class BlockProtectionListener implements Listener {

    final Factions plugin = Factions.get();

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        forbidIfInProtectedTerritory(event, event.getPlayer(), event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockPlaceEvent event) {
        forbidIfInProtectedTerritory(event, event.getPlayer(), event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        forbidIfInProtectedTerritory(event, event.getPlayer(), event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(PlayerBucketFillEvent event) {
        forbidIfInProtectedTerritory(event, event.getPlayer(), event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(PlayerBucketEmptyEvent event) {
        forbidIfInProtectedTerritory(event, event.getPlayer(), event.getBlock());
    }

    private void forbidIfInProtectedTerritory(Cancellable event, Player player, Block block) {
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(player);
        if (fPlayer.isBypassRaw()) {
            return;
        }
        Region region = plugin.getRegionManager().getRegionByChunk(block.getChunk());
        if (region == null) {
            return;
        }
        if (region.getType() == RegionType.WAR_ZONE && !plugin.getWarPhaseManager().getCurrentWarPhase().isOpenWarZones()) {
            event.setCancelled(true);
            return;
        }
        Relation relation = fPlayer.getRelation(region);
        if (!relation.canBuild()) {
            event.setCancelled(true);
        }
    }
}
