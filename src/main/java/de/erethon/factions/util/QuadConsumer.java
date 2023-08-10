package de.erethon.factions.util;

import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
@FunctionalInterface
public interface QuadConsumer<T, U, V, W> {

    void accept(T t, U u, V v, W w);

    default @NotNull QuadConsumer<T, U, V, W> andThen(@NotNull QuadConsumer<? super T, ? super U, ? super V, ? super W> after) {
        return (t, u, v, w) -> {
            accept(t, u, v, w);
            after.accept(t, u, v, w);
        };
    }

}
