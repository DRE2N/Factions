package de.erethon.factions.event;

import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public class FPlayerFactionJoinEvent extends FactionEvent {

    private static final HandlerList handlers = new HandlerList();

    private final FPlayer fPlayer;

    public FPlayerFactionJoinEvent(@NotNull Faction faction, @NotNull FPlayer fPlayer) {
        super(faction);
        this.fPlayer = fPlayer;
    }

    public @NotNull FPlayer getFPlayer() {
        return fPlayer;
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
