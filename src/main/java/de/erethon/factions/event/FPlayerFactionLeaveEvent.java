package de.erethon.factions.event;

import de.erethon.bedrock.config.Message;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public class FPlayerFactionLeaveEvent extends FactionEvent {

    private static final HandlerList handlers = new HandlerList();

    private final FPlayer fPlayer;
    private final Reason reason;
    private Component message;

    public FPlayerFactionLeaveEvent(@NotNull Faction faction, @NotNull FPlayer fPlayer, @NotNull Reason reason) {
        super(faction);
        this.fPlayer = fPlayer;
        this.reason = reason;
        this.message = reason.getMessage().message(fPlayer.getLastName());
    }

    public @NotNull FPlayer getFPlayer() {
        return fPlayer;
    }

    public @NotNull Reason getReason() {
        return reason;
    }

    public @NotNull Component getMessage() {
        return message;
    }

    public void setMessage(@NotNull Component message) {
        this.message = message;
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
         * The player decided to leave the faction via a command.
         */
        COMMAND(FMessage.FACTION_INFO_PLAYER_LEFT),
        /**
         * The player did not join the server in a long time.
         */
        INACTIVE(FMessage.FACTION_INFO_INACTIVE_PLAYER_KICKED),
        /**
         * The player got kicked by another player.
         */
        KICKED(FMessage.FACTION_INFO_PLAYER_GOT_KICKED);

        private final Message message;

        Reason(@NotNull Message message) {
            this.message = message;
        }

        public @NotNull Message getMessage() {
            return message;
        }
    }
}
