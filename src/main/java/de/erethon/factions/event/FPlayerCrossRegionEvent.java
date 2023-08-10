package de.erethon.factions.event;

import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Fyreum
 */
public class FPlayerCrossRegionEvent extends FPlayerEvent {

    private static final HandlerList handlers = new HandlerList();

    private final Region oldRegion, newRegion;

    public FPlayerCrossRegionEvent(@NotNull FPlayer fPlayer, @Nullable Region oldRegion, @Nullable Region newRegion) {
        super(fPlayer);
        this.oldRegion = oldRegion;
        this.newRegion = newRegion;
    }

    /* Getters */

    public @Nullable Region getOldRegion() {
        return oldRegion;
    }

    public @Nullable Region getNewRegion() {
        return newRegion;
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
