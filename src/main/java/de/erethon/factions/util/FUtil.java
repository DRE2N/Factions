package de.erethon.factions.util;

import de.erethon.factions.faction.Faction;
import de.erethon.factions.region.Region;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Supplier;

/**
 * @author Fyreum
 */
public class FUtil {

    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Berlin");

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

    public static @NotNull ZonedDateTime getDateTime() {
        return ZonedDateTime.now(ZONE_ID);
    }

    public static @NotNull ZonedDateTime getMidnightDateTime() {
        return ZonedDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT, ZONE_ID);
    }

    public static @NotNull ZonedDateTime getNoonDateTime() {
        return ZonedDateTime.of(LocalDate.now(), LocalTime.NOON, ZONE_ID);
    }
}
