package de.erethon.factions.integrations;

import de.erethon.aergia.Aergia;
import de.erethon.factions.Factions;
import de.erethon.factions.event.FPlayerFactionJoinEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * @author Fyreum
 */
public class DiscordBotListener implements Listener {

    final Factions plugin = Factions.get();
    final Aergia aergia = Aergia.inst();

    @EventHandler
    public void onFactionJoin(FPlayerFactionJoinEvent event) {

    }

}
