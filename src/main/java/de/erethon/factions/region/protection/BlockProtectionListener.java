package de.erethon.factions.region.protection;

import de.erethon.factions.Factions;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.entity.Relation;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionType;
import de.erethon.factions.region.RegionStructure;
import net.kyori.adventure.util.TriState;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

import java.util.List;

/**
 * @author Fyreum
 */
public class BlockProtectionListener implements Listener {

    final Factions plugin = Factions.get();

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        forbidIfInProtectedTerritory(event, event.getPlayer(), event.getBlock(), FMessage.PROTECTION_CANNOT_DESTROY_FACTION);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockPlaceEvent event) {
        forbidIfInProtectedTerritory(event, event.getPlayer(), event.getBlock(), FMessage.PROTECTION_CANNOT_BUILD_FACTION);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        forbidIfInProtectedTerritory(event, event.getPlayer(), event.getBlock(), FMessage.PROTECTION_CANNOT_BUILD_FACTION);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(PlayerBucketFillEvent event) {
        forbidIfInProtectedTerritory(event, event.getPlayer(), event.getBlock(), FMessage.PROTECTION_CANNOT_DESTROY_FACTION);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(PlayerBucketEmptyEvent event) {
        forbidIfInProtectedTerritory(event, event.getPlayer(), event.getBlock(), FMessage.PROTECTION_CANNOT_BUILD_FACTION);
    }

    private void forbidIfInProtectedTerritory(Cancellable event, Player player, Block block, FMessage message) {
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(player);
        if (fPlayer.isBypassRaw()) {
            doBuildingChecks(player, block, event);
            return;
        }
        Region region = plugin.getRegionManager().getRegionByChunk(block.getChunk());
        if (region == null) {
            return;
        }
        callEventForAllBuildingsInRegion(region, fPlayer, event);
        List<RegionStructure> structures = region.getStructuresAt(block.getLocation());
        TriState state = TriState.NOT_SET;
        // Loop until a structure dictates the following behaviour.
        for (RegionStructure structure : structures) {
            if (state != TriState.NOT_SET) {
                break;
            }
            state = structure.canBuild(fPlayer, block);
        }
        if (state == TriState.TRUE) {
            doBuildingChecks(player, block, event);
            return;
        }
        if (state == TriState.FALSE || !region.getType().isAllowsBuilding() ||
                (region.getType() == RegionType.WAR_ZONE && plugin.getWarPhaseManager().getCurrentWarPhase().isAllowPvP())) { // Can only build during peace.
            cancel(event, fPlayer, region, message);
            return;
        }
        Relation relation = fPlayer.getRelation(region);
        if (!relation.canBuild()) {
            cancel(event, fPlayer, region, message);
        } else {
            doBuildingChecks(player, block, event);
        }
    }

    private void cancel(Cancellable event, FPlayer fPlayer, Region region, FMessage message) {
        event.setCancelled(true);
        fPlayer.sendActionBarMessage(message.message(region.getDisplayOwner()));
    }

    private void doBuildingChecks(Player player, Block block, Cancellable event) {
        Chunk chunk = block.getChunk();
        plugin.getBuildSiteCache().get(chunk.getChunkKey()).forEach(building -> {
            if (building.isInBuildSite(block.getLocation())) {
                if (event instanceof BlockPlaceEvent) {
                    building.blockPlaced(player, event);
                } else if (event instanceof BlockBreakEvent) {
                    building.blockBroken(player, event);
                }
            }
        });
    }

    private void callEventForAllBuildingsInRegion(Region region, FPlayer player, Cancellable cancellable) {
        // Idk about the performance of this, guess we're going to have to see. Not really another way if an effect wants to add/remove drops or something though.
        for (BuildSite site : region.getBuildSites()) {
            if (site.isInBuildSite(player.getPlayer().getLocation())) {
                if (cancellable instanceof BlockPlaceEvent place) {
                    site.onBlockPlaceInRegion(player, place.getBlock(), cancellable);
                } else if (cancellable instanceof BlockBreakEvent breakEvent) {
                    site.onBlockBreakInRegion(player, breakEvent.getBlock(), cancellable);
                }
            }
        }
    }
}
