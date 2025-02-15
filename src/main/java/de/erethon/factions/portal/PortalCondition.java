package de.erethon.factions.portal;

import de.erethon.factions.Factions;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author Fyreum
 */
public abstract class PortalCondition {

    public static final PortalCondition ALLOW_PVP = checkOrMessage("allow_pvp", fp -> Factions.get().getCurrentWarPhase().isAllowPvP(), FMessage.ERROR_PORTAL_CAPITAL_CLOSED);
    public static final PortalCondition OPEN_CAPITAL = checkOrMessage("open_capital", fp -> Factions.get().getCurrentWarPhase().isOpenCapital(), FMessage.ERROR_PORTAL_CAPITAL_CLOSED);

    private static final Map<String, PortalCondition> CONDITIONS = new HashMap<>();

    /* Class */

    private final String name;

    public PortalCondition(String name) {
        this.name = name;
        CONDITIONS.putIfAbsent(name.toLowerCase(), this);
    }

    public abstract boolean check(@NotNull FPlayer fPlayer);

    public @NotNull String getName() {
        return name;
    }

    /* Statics */

    public static @NotNull PortalCondition checkOrMessage(@NotNull String name, @NotNull Predicate<FPlayer> predicate, @NotNull FMessage message) {
        return new PortalCondition(name) {
            @Override
            public boolean check(@NotNull FPlayer fPlayer) {
                if (predicate.test(fPlayer)) {
                    return true;
                }
                fPlayer.sendMessage(message.message());
                return false;
            }
        };
    }

    public static @NotNull Set<String> getConditionNames() {
        return CONDITIONS.keySet();
    }

    public static @Nullable PortalCondition getByName(@NotNull String string) {
        return CONDITIONS.get(string.toLowerCase());
    }

}
