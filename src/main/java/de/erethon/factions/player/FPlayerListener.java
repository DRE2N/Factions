package de.erethon.factions.player;

import de.erethon.aergia.ui.UIComponent;
import de.erethon.factions.Factions;
import de.erethon.factions.event.FPlayerCrossRegionEvent;
import de.erethon.factions.region.Region;
import de.erethon.factions.integrations.UIFactionsListener;
import de.erethon.factions.region.RegionType;
import de.erethon.factions.war.entities.LoggedOutPlayer;
import de.erethon.papyrus.PlayerSwitchProfileEvent;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Fyreum
 */
public class FPlayerListener implements Listener {

    final Factions plugin = Factions.get();

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }
        Player player = event.getPlayer();
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayerIfCached(player);
        if (fPlayer == null) {
            return;
        }
        fPlayer.getAutomatedChunkManager().handle(event.getTo().getChunk());
        updateLastRegion(fPlayer, event.getTo());
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayerIfCached(player);
        if (fPlayer == null) {
            return;
        }
        Region region = fPlayer.getLastRegion();
        // If the player is in a warzone, spawn a LoggedOutPlayer entity so that the player can be killed
        if (region != null && (region.getType() == RegionType.WAR_ZONE || region.getType() == RegionType.CAPITAL)) {
            CraftWorld world = (CraftWorld) region.getWorld();
            ServerLevel level = world.getHandle();
            LoggedOutPlayer loggedOutPlayer = new LoggedOutPlayer(level, player);
            level.addFreshEntity(loggedOutPlayer);
        }
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayerIfCached(event.getPlayer());
        if (fPlayer == null) {
            return;
        }
        Region region = fPlayer.getLastRegion();
        if (region != null && (region.getType() == RegionType.WAR_ZONE || region.getType() == RegionType.CAPITAL)) {
            if (plugin.getFConfig().getCommandsDisabledInWarZone().contains(event.getMessage())) {
                fPlayer.sendMessage(Component.translatable("factions.war.commandDisabled"));
                event.setCancelled(true);
            }
        }
    }

    private void updateLastRegion(FPlayer fPlayer, Location to) {
        Region region = plugin.getRegionManager().getRegionByLocation(to);
        if (region == fPlayer.getLastRegion()) {
            return;
        }
        fPlayer.setLastRegion(region);
        UIComponent component = fPlayer.getUIBossBar().getCenter().getById(UIFactionsListener.REGION_DISPLAY_ID);
        if (component != null) {
            component.resetDuration();
        }
        new FPlayerCrossRegionEvent(fPlayer, fPlayer.getLastRegion(), region).callEvent();
    }


}
