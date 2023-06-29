package de.erethon.factions.region.structure;

import de.erethon.factions.Factions;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import io.papermc.paper.math.Position;
import net.kyori.adventure.util.TriState;
import org.apache.commons.lang.math.IntRange;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an area inside a region with its own kind of rules.
 *
 * @author Fyreum
 */
@SuppressWarnings("UnstableApiUsage")
public class RegionStructure {

    protected final Factions plugin = Factions.get();
    protected final IntRange xRange, yRange, zRange;

    public RegionStructure(@NotNull Position a, @NotNull Position b) {
        this.xRange = new IntRange(a.x(), b.x());
        this.yRange = new IntRange(a.y(), b.y());
        this.zRange = new IntRange(a.z(), b.z());
    }

    /* Protection */

    public @NotNull TriState canBuild(@NotNull FPlayer fPlayer, @NotNull Region region) {
        return TriState.NOT_SET;
    }

    public @NotNull TriState canAttack(@NotNull FPlayer fPlayer, @NotNull Region region) {
        return TriState.NOT_SET;
    }

    /* Getters */

    public boolean containsPosition(@NotNull Position position) {
        return containsPosition(position.x(), position.y(), position.z());
    }

    public boolean containsPosition(double x, double y, double z) {
        return xRange.containsDouble(x) && yRange.containsDouble(y) && zRange.containsDouble(z);
    }

    public @NotNull IntRange getXRange() {
        return xRange;
    }

    public @NotNull IntRange getYRange() {
        return yRange;
    }

    public @NotNull IntRange getZRange() {
        return zRange;
    }

    public @NotNull Position getMinPosition() {
        return Position.block(xRange.getMinimumInteger(), yRange.getMinimumInteger(), zRange.getMinimumInteger());
    }

    public @NotNull Position getMaxPosition() {
        return Position.block(xRange.getMaximumInteger(), yRange.getMaximumInteger(), zRange.getMaximumInteger());
    }

    public @NotNull Position getCenterPosition() {
        return Position.block(
                (xRange.getMinimumInteger() + xRange.getMaximumInteger()) / 2,
                (yRange.getMinimumInteger() + yRange.getMaximumInteger()) / 2,
                (zRange.getMinimumInteger() + zRange.getMaximumInteger()) / 2
        );
    }

    /* Serialization */

    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> serialized = new HashMap<>(2);
        serialized.put("minPosition", Map.of("x", xRange.getMinimumInteger(), "y", yRange.getMinimumInteger(), "z", zRange.getMinimumInteger()));
        serialized.put("maxPosition", Map.of("x", xRange.getMaximumInteger(), "y", yRange.getMaximumInteger(), "z", zRange.getMaximumInteger()));
        return serialized;
    }

    public static @NotNull RegionStructure deserialize(@NotNull ConfigurationSection config) {
        Position minPosition = deserializePosition(config.getConfigurationSection("minPosition"));
        Position maxPosition = deserializePosition(config.getConfigurationSection("maxPosition"));
        return switch (config.getString("type")) {
            case "WarCastle" -> new WarCastleStructure(minPosition, maxPosition);
            case null, default -> new RegionStructure(minPosition, maxPosition);
        };
    }

    protected static Position deserializePosition(ConfigurationSection section) {
        if (section == null) {
            return Position.BLOCK_ZERO;
        }
        return Position.block(section.getInt("x"), section.getInt("y"), section.getInt("z"));
    }
}
