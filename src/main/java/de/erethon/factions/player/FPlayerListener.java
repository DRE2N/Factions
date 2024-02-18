package de.erethon.factions.player;

import de.erethon.aergia.ui.UIComponent;
import de.erethon.factions.Factions;
import de.erethon.factions.event.FPlayerCrossRegionEvent;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionCache;
import de.erethon.factions.ui.UIFactionsListener;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

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
