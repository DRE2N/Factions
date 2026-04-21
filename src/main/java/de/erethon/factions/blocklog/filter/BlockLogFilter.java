package de.erethon.factions.blocklog.filter;

import de.erethon.factions.blocklog.model.ChangeType;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable filter for block log queries.
 * Use {@link BlockLogFilter#builder()} to construct.
 *
 * @author Malfrador
 */
public record BlockLogFilter(
        List<String> players,
        List<String> excludedPlayers,
        @Nullable Integer radius,
        @Nullable Integer centerX,
        @Nullable Integer centerY,
        @Nullable Integer centerZ,
        @Nullable String centerWorld,
        @Nullable Integer selMinX,
        @Nullable Integer selMinY,
        @Nullable Integer selMinZ,
        @Nullable Integer selMaxX,
        @Nullable Integer selMaxY,
        @Nullable Integer selMaxZ,
        @Nullable String selWorld,
        @Nullable String world,
        @Nullable Instant since,
        @Nullable Instant before,
        List<Material> blocks,
        List<Material> excludedBlocks,
        @Nullable String regionName,
        @Nullable Integer regionId,
        List<ChangeType> changeTypes,
        int limit,
        int offset,
        boolean ascending,
        @Nullable String rawArgs // Original args string for pagination reconstruction
) {

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true if any spatial filter (area, selection) is set.
     */
    public boolean hasSpatialFilter() {
        return radius != null || selMinX != null;
    }

    /**
     * Returns a new filter with the given offset for pagination.
     */
    public BlockLogFilter withOffset(int newOffset) {
        return new BlockLogFilter(
                players, excludedPlayers, radius,
                centerX, centerY, centerZ, centerWorld,
                selMinX, selMinY, selMinZ, selMaxX, selMaxY, selMaxZ, selWorld,
                world, since, before, blocks, excludedBlocks,
                regionName, regionId, changeTypes, limit, newOffset, ascending, rawArgs
        );
    }

    /**
     * Returns a new filter with no limit or offset applied (fetches all matching rows).
     * Used when pagination needs to happen after collapsing display lines.
     */
    public BlockLogFilter withNoLimit() {
        return new BlockLogFilter(
                players, excludedPlayers, radius,
                centerX, centerY, centerZ, centerWorld,
                selMinX, selMinY, selMinZ, selMaxX, selMaxY, selMaxZ, selWorld,
                world, since, before, blocks, excludedBlocks,
                regionName, regionId, changeTypes, Integer.MAX_VALUE, 0, ascending, rawArgs
        );
    }

    public static class Builder {
        private final List<String> players = new ArrayList<>();
        private final List<String> excludedPlayers = new ArrayList<>();
        private Integer radius;
        private Integer centerX, centerY, centerZ;
        private String centerWorld;
        private Integer selMinX, selMinY, selMinZ;
        private Integer selMaxX, selMaxY, selMaxZ;
        private String selWorld;
        private String world;
        private Instant since;
        private Instant before;
        private final List<Material> blocks = new ArrayList<>();
        private final List<Material> excludedBlocks = new ArrayList<>();
        private String regionName;
        private Integer regionId;
        private final List<ChangeType> changeTypes = new ArrayList<>();
        private int limit = 15;
        private int offset = 0;
        private boolean ascending = false;
        private String rawArgs;

        public Builder player(String name) {
            if (name.startsWith("!")) {
                excludedPlayers.add(name.substring(1));
            } else {
                players.add(name);
            }
            return this;
        }

        public Builder area(int radius, int x, int y, int z, String worldName) {
            this.radius = radius;
            this.centerX = x;
            this.centerY = y;
            this.centerZ = z;
            this.centerWorld = worldName;
            return this;
        }

        public Builder selection(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String worldName) {
            this.selMinX = minX;
            this.selMinY = minY;
            this.selMinZ = minZ;
            this.selMaxX = maxX;
            this.selMaxY = maxY;
            this.selMaxZ = maxZ;
            this.selWorld = worldName;
            return this;
        }

        public Builder world(String world) {
            this.world = world;
            return this;
        }

        public Builder since(Instant since) {
            this.since = since;
            return this;
        }

        public Builder before(Instant before) {
            this.before = before;
            return this;
        }

        public Builder block(String materialName) {
            if (materialName.startsWith("!")) {
                Material mat = Material.matchMaterial(materialName.substring(1));
                if (mat != null) excludedBlocks.add(mat);
            } else {
                Material mat = Material.matchMaterial(materialName);
                if (mat != null) blocks.add(mat);
            }
            return this;
        }

        public Builder region(String name) {
            this.regionName = name;
            return this;
        }

        public Builder regionId(int id) {
            this.regionId = id;
            return this;
        }

        public Builder changeType(ChangeType type) {
            this.changeTypes.add(type);
            return this;
        }

        public Builder limit(int limit) {
            this.limit = Math.max(1, Math.min(limit, 10000));
            return this;
        }

        public Builder offset(int offset) {
            this.offset = Math.max(0, offset);
            return this;
        }

        public Builder ascending(boolean asc) {
            this.ascending = asc;
            return this;
        }

        public Builder rawArgs(String rawArgs) {
            this.rawArgs = rawArgs;
            return this;
        }

        public BlockLogFilter build() {
            return new BlockLogFilter(
                    List.copyOf(players), List.copyOf(excludedPlayers),
                    radius, centerX, centerY, centerZ, centerWorld,
                    selMinX, selMinY, selMinZ, selMaxX, selMaxY, selMaxZ, selWorld,
                    world, since, before,
                    List.copyOf(blocks), List.copyOf(excludedBlocks),
                    regionName, regionId,
                    List.copyOf(changeTypes),
                    limit, offset, ascending, rawArgs
            );
        }
    }
}

