package de.erethon.factions.util;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * @author Fyreum
 */
public class FBroadcastUtil {

    public static void broadcastIf(@NotNull FMessage message, @NotNull Predicate<FPlayer> filter) {
        broadcastIf(message.message(), filter);
    }

    public static void broadcastIf(@NotNull FMessage message, @NotNull Predicate<FPlayer> filter, @NotNull String... args) {
        broadcastIf(message.message(args), filter);
    }

    public static void broadcastIf(@NotNull FMessage message, @NotNull Predicate<FPlayer> filter, @NotNull Component... args) {
        broadcastIf(message.message(args), filter);
    }

    public static void broadcastIf(@NotNull Component message, @NotNull Predicate<FPlayer> filter) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!filter.test(Factions.get().getFPlayerCache().getByPlayer(player))) {
                continue;
            }
            player.sendMessage(message);
        }
    }

    public static void broadcastWar(@NotNull Component message) {
        broadcastWarFinal(prefixWar(message));
    }

    public static void broadcastWar(@NotNull FMessage message) {
        broadcastWarFinal(prefixWar(message.message()));
    }

    public static void broadcastWar(@NotNull FMessage message, String... args) {
        broadcastWarFinal(prefixWar(message.message(args)));
    }

    public static void broadcastWar(@NotNull FMessage message, Component... args) {
        broadcastWarFinal(prefixWar(message.message(args)));
    }

    private static Component prefixWar(Component msg) {
        return FMessage.WAR_PREFIX.message(msg);
    }

    private static void broadcastWarFinal(@NotNull Component finalMessage) {
        FLogger.WAR.log(finalMessage);
        Bukkit.getServer().broadcast(finalMessage);
    }
}
