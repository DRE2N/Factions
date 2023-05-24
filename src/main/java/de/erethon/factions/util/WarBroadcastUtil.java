package de.erethon.factions.util;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.data.FMessage;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public class WarBroadcastUtil {

    public static void broadcast(@NotNull FMessage message) {
        broadcastFinal(prefix(message.message()));
    }

    public static void broadcast(@NotNull FMessage message, String... args) {
        broadcastFinal(prefix(message.message(args)));
    }

    public static void broadcast(@NotNull FMessage message, Component... args) {
        broadcastFinal(prefix(message.message(args)));
    }

    private static Component prefix(Component msg) {
        return FMessage.WAR_PREFIX.message().append(msg);
    }

    private static void broadcastFinal(@NotNull Component finalMessage) {
        FLogger.WAR.log(() -> finalMessage);
        MessageUtil.broadcastMessage(finalMessage);
    }
}
