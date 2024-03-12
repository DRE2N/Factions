package de.erethon.factions.event;

import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.player.FPlayer;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Gets called after a player has changed their alliance.
 *
 * @author Fyreum
 */
public class FPlayerChangeAllianceEvent extends FPlayerEvent {

    private static final HandlerList handlers = new HandlerList();

    private final Alliance old;

    public FPlayerChangeAllianceEvent(@NotNull FPlayer fPlayer, @Nullable Alliance old) {
        super(fPlayer);
        assert fPlayer.getAlliance() != old : "Alliance did not change";
        this.old = old;
    }

    public @Nullable Alliance getOldAlliance() {
        return old;
    }

    public @NotNull Alliance getNewAlliance() {
        return fPlayer.getAlliance();
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
