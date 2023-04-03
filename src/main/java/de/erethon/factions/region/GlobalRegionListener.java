package de.erethon.factions.region;

import de.erethon.factions.Factions;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

/**
 * @author Fyreum
 */
public class GlobalRegionListener implements Listener {

    final Factions plugin = Factions.get();

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        plugin.getRegionManager().loadWorld(event.getWorld());
    }

    @EventHandler
    public void onWorldUnload(WorldLoadEvent event) {
        plugin.getRegionManager().unloadWorld(event.getWorld());
    }

}
