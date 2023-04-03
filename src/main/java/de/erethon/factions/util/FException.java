package de.erethon.factions.util;

import de.erethon.bedrock.config.Message;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public class FException extends RuntimeException {

    private final Message playerMessage;
    private final String[] args;

    public FException(@NotNull String internalMessage, @NotNull Message playerMessage, @NotNull String... args) {
        super(internalMessage);
        this.playerMessage = playerMessage;
        this.args = args;
    }

    public Component getPlayerMessage() {
        return playerMessage.message(args);
    }

    public static void throwIf(boolean b, @NotNull String internalMessage, @NotNull Message playerMessage, @NotNull String... args) {
        if (b) {
            throw new FException(internalMessage, playerMessage, args);
        }
    }
}
