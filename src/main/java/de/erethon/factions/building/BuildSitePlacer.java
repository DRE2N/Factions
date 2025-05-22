package de.erethon.factions.building;

import de.erethon.factions.Factions;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;

public class BuildSitePlacer implements Listener {

    private final Building building;
    private final FPlayer player;
    private final Region region;
    private final Faction faction;

    public BuildSitePlacer(Building building, FPlayer player, Region region, Faction faction) {
        this.building = building;
        this.player = player;
        this.region = region;
        this.faction = faction;
        if (building == null || region == null || faction == null || player.getPlayer() == null) {
            player.sendMessage("<red>Internal error. Invalid building, region, faction or player.");
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, Factions.get());
        player.getPlayer().showTitle(Title.title(Component.empty(), Component.translatable("factions.building.place.title")));
    }

    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        if (!event.getPlayer().equals(player.getPlayer())) {
            return;
        }
        Block targetBlock = event.getPlayer().getTargetBlockExact(16);
        if (targetBlock == null) return;
        boolean isAllowed = building.checkRequirements(player.getPlayer(), faction, targetBlock.getLocation()).isEmpty();
        building.displayFrame(player.getPlayer(), targetBlock.getLocation(), isAllowed);
    }

    @EventHandler
    private void onInteract(PlayerInteractEvent event) {
        if (!event.getPlayer().equals(player.getPlayer())) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        event.setCancelled(true);
        if (event.getAction().isLeftClick()) { // Confirmed placement
            if (!building.checkRequirements(player.getPlayer(), faction, event.getClickedBlock().getLocation()).isEmpty()) {
                player.sendMessage(Component.translatable("factions.building.place.requirements"));
                for (RequirementFail fail : building.checkRequirements(player.getPlayer(), faction, event.getClickedBlock().getLocation())) {
                    player.sendMessage(fail.getTranslationKey());
                }
                return;
            }
            Region rg = player.getCurrentRegion(); // Just in case the player moved
            Faction faction = player.getFaction(); // Maybe someone leaves while trying to place a building
            if (rg == null || faction == null || faction != this.faction) {
                player.sendMessage(Component.translatable("factions.building.place.cancelled"));
                return;
            }
            building.build(event.getPlayer(), faction, rg, event.getClickedBlock().getLocation());
            HandlerList.unregisterAll(this);
        }
        if (event.getAction().isRightClick()) { // Cancelled placement
            player.sendMessage(Component.translatable("factions.building.place.cancelled"));
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    private void onDeath(PlayerDeathEvent event) {
        if (event.getEntity().equals(player.getPlayer())) {
            player.sendMessage(Component.translatable("factions.building.place.cancelled"));
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    private void onTeleport(PlayerTeleportEvent event) {
        if (event.getPlayer().equals(player.getPlayer())) {
            player.sendMessage(Component.translatable("factions.building.place.cancelled"));
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        if (event.getPlayer().equals(player.getPlayer())) {
            HandlerList.unregisterAll(this);
        }
    }
}
