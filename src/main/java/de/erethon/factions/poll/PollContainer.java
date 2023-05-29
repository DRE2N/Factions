package de.erethon.factions.poll;

import de.erethon.factions.player.FPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Fyreum
 */
public interface PollContainer {

    default void addPoll(@NotNull Poll<?> poll) {
        addPoll(poll, Poll.DEFAULT_DURATION);
    }

    void addPoll(@NotNull Poll<?> poll, long duration);

    void removePoll(@NotNull Poll<?> poll);

    default @Nullable Poll<?> getPoll(@NotNull String name) {
        return getPolls().get(name);
    }

    @NotNull Map<String, Poll<?>> getPolls();

    default @NotNull Map<String, Poll<?>> getPollsFor(@NotNull FPlayer fPlayer) {
        Map<String, Poll<?>> polls = new HashMap<>();
        for (Poll<?> poll : getPolls().values()) {
            if (poll.canParticipate(fPlayer)) {
                polls.put(poll.getName(), poll);
            }
        }
        return polls;
    }

}
