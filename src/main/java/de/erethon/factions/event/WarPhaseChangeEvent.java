package de.erethon.factions.event;

import de.erethon.factions.war.WarPhase;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public class WarPhaseChangeEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final WarPhase oldPhase, newPhase;

    public WarPhaseChangeEvent(@NotNull WarPhase oldPhase, @NotNull WarPhase newPhase) {
        super(!Bukkit.isPrimaryThread());
        this.oldPhase = oldPhase;
        this.newPhase = newPhase;
    }

    public @NotNull WarPhase getOldPhase() {
        return oldPhase;
    }

    public @NotNull WarPhase getNewPhase() {
        return newPhase;
    }

    /* Bukkit stuff */

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }
}
