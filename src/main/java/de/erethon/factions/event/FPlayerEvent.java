package de.erethon.factions.event;

import de.erethon.factions.player.FPlayer;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public abstract class FPlayerEvent extends Event {

    protected final FPlayer fPlayer;

    public FPlayerEvent(@NotNull FPlayer fPlayer) {
        this.fPlayer = fPlayer;
    }

    public @NotNull FPlayer getFPlayer() {
        return fPlayer;
    }
}
