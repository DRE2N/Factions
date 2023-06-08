package de.erethon.factions.util;

import de.erethon.factions.faction.Faction;
import de.erethon.factions.region.Region;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * @author Fyreum
 */
public class FUtil {

    public static boolean isAdjacent(@NotNull Faction faction, @NotNull Faction other) {
        for (Region region : faction.getRegions()) {
            for (Region otherRegion : other.getRegions()) {
                if (region.isAdjacentRegion(otherRegion)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SafeVarargs
    public static <V> @Nullable V getNotNull(@NotNull Supplier<@Nullable V>... suppliers) {
        return getNotNullOr(null, suppliers);
    }

    @SafeVarargs
    @Contract("!null, _ -> !null; null, _ -> _")
    public static <V> @Nullable V getNotNullOr(@Nullable V def, @NotNull Supplier<@Nullable V>... suppliers) {
        for (Supplier<V> supplier : suppliers) {
            V value = supplier.get();
            if (value != null) {
                return value;
            }
        }
        return def;
    }

    @SafeVarargs
    public static <V> @NotNull V getNotNullOrThrow(@NotNull Supplier<@Nullable V>... suppliers) {
        V value = getNotNullOr(null, suppliers);
        assert value != null : "Null value not permitted";
        return value;
    }
}
