package de.erethon.factions.event;

import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public class FactionCreateEvent extends FPlayerFactionJoinEvent {

    private static final HandlerList handlers = new HandlerList();

    public FactionCreateEvent(@NotNull Faction faction, @NotNull FPlayer fPlayer) {
        super(faction, fPlayer);
    }

    /* Bukkit stuff */

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
