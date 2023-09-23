package de.erethon.factions.poll;

import de.erethon.factions.Factions;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.util.FLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

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

    default @NotNull Map<String, Poll<?>> loadPolls(@Nullable ConfigurationSection config) {
        if (config == null) {
            return Map.of();
        }
        Map<String, Poll<?>> map = new HashMap<>();
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) {
                FLogger.ERROR.log("Could not load poll '" + key + "': Section is null");
                continue;
            }
            Function<ConfigurationSection, Poll<?>> deserializer = Factions.get().getPollDeserializerRegistry().get(section.getString("type"));
            if (deserializer == null) {
                FLogger.ERROR.log("Illegal poll type in poll '" + key + "' found: " + section.getString("type"));
                continue;
            }
            Poll<?> poll;
            try {
                poll = deserializer.apply(section);
            } catch (IllegalArgumentException e) {
                FLogger.ERROR.log("Could not load poll '" + key + "': " + e.getMessage());
                continue;
            }
            if (poll.closeTime <= System.currentTimeMillis()) {
                poll.closePoll();
                continue;
            }
            map.put(poll.getName(), poll);
            poll.openPoll(poll.closeTime - System.currentTimeMillis());
        }
        return map;
    }

    default @NotNull Map<String, Object> serializePolls() {
        Map<String, Object> map = new HashMap<>();
        for (Poll<?> poll : getPolls().values()) {
            map.put(poll.getName(), poll.serialize());
        }
        return map;
    }

}
