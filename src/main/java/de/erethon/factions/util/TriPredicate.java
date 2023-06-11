package de.erethon.factions.util;

import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
@FunctionalInterface
public interface TriPredicate<T, U, V> {

    boolean test(T t, U u, V v);

    default @NotNull TriPredicate<T, U, V> and(@NotNull TriPredicate<? super T, ? super U, ? super V> other) {
        return (T t, U u, V v) -> test(t, u, v) && other.test(t, u, v);
    }

    default @NotNull TriPredicate<T, U, V> negate() {
        return (T t, U u, V v) -> !test(t, u, v);
    }

    default @NotNull TriPredicate<T, U, V> or(@NotNull TriPredicate<? super T, ? super U, ? super V> other) {
        return (T t, U u, V v) -> test(t, u, v) || other.test(t, u, v);
    }

}
