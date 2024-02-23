package de.erethon.factions.util;

import de.erethon.factions.faction.Faction;
import de.erethon.factions.region.LazyChunk;
import de.erethon.factions.region.Region;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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

    @SuppressWarnings("UnstableApiUsage")
    public static @NotNull String toString(@NotNull Position position) {
        return toString(position, ",");
    }

    @SuppressWarnings("UnstableApiUsage")
    public static @NotNull String toString(@NotNull Position position, String separator) {
        return position.x() + separator + position.y() + separator + position.z();
    }

    @SuppressWarnings("UnstableApiUsage")
    public static boolean regionContainsAABB(@NotNull Region region, @NotNull Position pos1, @NotNull Position pos2) {
        return regionContainsAABB(region, new LazyChunk(pos1.blockX() >> 4, pos1.blockZ() >> 4), new LazyChunk(pos2.blockX() >> 4, pos2.blockZ() >> 4));
    }

    public static boolean regionContainsAABB(@NotNull Region region, @NotNull LazyChunk pos1, @NotNull LazyChunk pos2) {
        if (pos1.equals(pos2)) {
            return region.getChunks().contains(pos1);
        }
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        for (int x = Math.min(pos1.getX(), pos2.getX()); x < maxX; x++) {
            for (int z = Math.min(pos1.getZ(), pos2.getZ()); z < maxZ; z++) {
                if (!region.getChunks().contains(new LazyChunk(x, z))) {
                    return false;
                }
            }
        }
        return true;
    }

    @SuppressWarnings("UnstableApiUsage")
    public static BlockPosition parsePosition(String string) {
        String[] split = string.split(";");
        return Position.block(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
    }

    @SuppressWarnings("UnstableApiUsage")
    public static String positionToString(Position position) {
        return position.x() + ";" + position.y() + ";" + position.z();
    }

    // This constructor is just so damn long, so let's put it here
    // Note: If the fake type is not living, the client lags when attributes get updated. Use LivingEntity#syncAttributes = false to prevent this.
    public static ClientboundAddEntityPacket getAddEntityPacketWithType(Entity entity, EntityType<?> type) {
        return new ClientboundAddEntityPacket(entity.getId(), entity.getUUID(), entity.getX(), entity.getY(), entity.getZ(), entity.getXRot(), entity.getYRot(), type, 0, entity.getDeltaMovement(), entity.getYHeadRot(), entity);
    }
}
