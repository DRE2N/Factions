package de.erethon.factions.util;

import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
@FunctionalInterface
public interface TriConsumer<T, U, V> {

    void accept(T t, U u, V v);

    default @NotNull TriConsumer<T, U, V> andThen(@NotNull TriConsumer<? super T, ? super U, ? super V> after) {
        return (t, u, v) -> {
            accept(t, u, v);
            after.accept(t, u, v);
        };
    }

}
