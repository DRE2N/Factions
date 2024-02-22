package de.erethon.factions.region;

import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.player.FPlayer;
import io.papermc.paper.math.Position;
import net.kyori.adventure.util.TriState;
import org.apache.commons.lang.math.IntRange;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
    protected final Region region;
    protected final String name;
    protected final IntRange xRange, yRange, zRange;

    public RegionStructure(@NotNull Region region, @NotNull ConfigurationSection config) {
        this(region, config, deserializePosition(config.getConfigurationSection("minPosition")),
                deserializePosition(config.getConfigurationSection("maxPosition")));
    }

    public RegionStructure(@NotNull Region region, @NotNull ConfigurationSection config, @NotNull Position a, @NotNull Position b) {
        this.region = region;
        this.name = config.getString("name", getClass().getSimpleName() + "_" + region.getStructures(getClass()).size());
        this.xRange = new IntRange(a.x(), b.x());
        this.yRange = new IntRange(a.y(), b.y());
        this.zRange = new IntRange(a.z(), b.z());
        load(config);
    }

    protected void load(@NotNull ConfigurationSection config) {

    }

    public void onTemporaryOccupy(@NotNull Alliance alliance) {

    }

    public void deleteStructure() {
        region.removeStructure(this);
    }

    /* Protection */

    public @NotNull TriState canBuild(@NotNull FPlayer fPlayer, @Nullable Block block) {
        return TriState.NOT_SET;
    }

    public @NotNull TriState canAttack(@NotNull FPlayer fPlayer, @Nullable Entity target) {
        return TriState.NOT_SET;
    }

    /* Getters */

    public boolean containsPosition(@NotNull Position position) {
        return containsPosition(position.x(), position.y(), position.z());
    }

    public boolean containsPosition(double x, double y, double z) {
        return xRange.containsDouble(x) && yRange.containsDouble(y) && zRange.containsDouble(z);
    }

    public @NotNull Region getRegion() {
        return region;
    }

    public @NotNull String getName() {
        return name;
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

    public Map<String, Object> serialize() {
        Map<String, Object> serialized = new HashMap<>(2);
        serialized.put("type", getClass().getName());
        serialized.put("name", name);
        serialized.put("minPosition", Map.of("x", xRange.getMinimumInteger(), "y", yRange.getMinimumInteger(), "z", zRange.getMinimumInteger()));
        serialized.put("maxPosition", Map.of("x", xRange.getMaximumInteger(), "y", yRange.getMaximumInteger(), "z", zRange.getMaximumInteger()));
        return serialized;
    }

    public static @NotNull RegionStructure deserialize(@NotNull Region region, @NotNull ConfigurationSection config) {
        String typeName = config.getString("type");
        Class<?> type;
        try {
            type = Class.forName(typeName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Illegal region structure type for '" + config.getName() + "' found: " + typeName);
        }
        Constructor<?> constructor;
        try {
            constructor = type.getDeclaredConstructor(Region.class, ConfigurationSection.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("No matching constructor for region structure type '" + typeName + "' found: " + ConfigurationSection.class.getName());
        }
        Object object;
        try {
            object = constructor.newInstance(region, config);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Couldn't instantiate region structure '" + config.getName() + "'", e);
        }
        if (!(object instanceof RegionStructure structure)) {
            throw new IllegalArgumentException("Illegal region structure type '" + typeName + "': Not a war objective");
        }
        return structure;
    }

    public static @NotNull Position deserializePosition(@Nullable ConfigurationSection section) {
        if (section == null) {
            return Position.BLOCK_ZERO;
        }
        return Position.block(section.getInt("x"), section.getInt("y"), section.getInt("z"));
    }
}
