package de.erethon.factions.event;

import de.erethon.factions.faction.Faction;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public class FactionDisbandEvent extends FactionEvent {

    private static final HandlerList handlers = new HandlerList();

    private final Reason reason;

    public FactionDisbandEvent(@NotNull Faction faction, @NotNull Reason reason) {
        super(faction);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    /* Bukkit stuff */

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public enum Reason {
        /**
         * The current admin decides to disband the faction via a command.
         */
        COMMAND,
        /**
         * The current admin left the faction or got kicked and no successor can be found.
         */
        NO_MEMBERS_LEFT,
        /**
         * The faction exceeded its debt limit because it couldn't pay its taxes.
         */
        TOO_MUCH_TAX_DEBT,
        /**
         * The reason for disbandment is unknown.
         *
         * @deprecated should not be used for clarity reasons
         */
        @Deprecated
        UNKNOWN,
    }
}
