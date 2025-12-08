package de.erethon.factions.util.display;

import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.LazyChunk;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Displays a region map as a TextDisplay entity mounted to the player.
 *
 * @author Malfrador
 */
public class RegionMapDisplay implements Listener {

    private static final Map<UUID, RegionMapDisplay> activeDisplays = new HashMap<>();

    /**
     * Global tile cache: World UUID -> ZoomLevel -> TileKey -> RegionId (or -1 for wilderness)
     * TileKey is encoded as (tileX << 32) | (tileZ & 0xFFFFFFFFL)
     */
    private static final Map<UUID, Map<ZoomLevel, Map<Long, Integer>>> tileCache = new ConcurrentHashMap<>();

    /**
     * Cache for border status: World UUID -> ZoomLevel -> TileKey -> isBorder
     */
    private static final Map<UUID, Map<ZoomLevel, Map<Long, Boolean>>> borderCache = new ConcurrentHashMap<>();

    private static final char CHUNK_CHAR = '█';
    private static final char WILDERNESS_CHAR = '█';
    private static final float UI_FORWARD_OFFSET = 0.9f;
    private static final float UI_VERTICAL_OFFSET = -1f;

    /**
     * Zoom levels for the map display.
     * Each level defines how many chunks per character (or chars per chunk for detailed views).
     * Positive chunksPerChar = multiple chunks per character (zoomed out)
     * Negative chunksPerChar = multiple characters per chunk (zoomed in), absolute value is chars per chunk
     */
    public enum ZoomLevel {
        DETAIL(-3, 32, 0.1f, "Detail"),       // 3x3 chars per chunk
        VERY_CLOSE(-2, 32, 0.1f, "Very Close"), // 2x2 chars per chunk
        CLOSE(1, 32, 0.1f, "Close"),          // 1:1 chunk to char
        MEDIUM(2, 32, 0.1f, "Medium"),        // 2x2 chunks per char
        FAR(4, 32, 0.1f, "Far"),              // 4x4 chunks per char
        WORLD(8, 32, 0.1f, "World");          // 8x8 chunks per char

        private final int chunksPerChar; // negative = chars per chunk
        private final int radius; // in characters
        private final float scale;
        private final String displayName;

        ZoomLevel(int chunksPerChar, int radius, float scale, String displayName) {
            this.chunksPerChar = chunksPerChar;
            this.radius = radius;
            this.scale = scale;
            this.displayName = displayName;
        }

        public int getChunksPerChar() { return chunksPerChar; }
        public int getRadius() { return radius; }
        public float getScale() { return scale; }
        public String getDisplayName() { return displayName; }

        /**
         * Returns chars per chunk for detailed zoom levels, or 1 for normal/zoomed out levels.
         */
        public int getCharsPerChunk() {
            return chunksPerChar < 0 ? -chunksPerChar : 1;
        }

        /**
         * Returns true if this is a detailed zoom level (multiple chars per chunk).
         */
        public boolean isDetailedZoom() {
            return chunksPerChar < 0;
        }

        /**
         * Returns the actual chunks per char for zoomed out levels, or 1 for detailed/normal levels.
         */
        public int getActualChunksPerChar() {
            return chunksPerChar > 0 ? chunksPerChar : 1;
        }

        public ZoomLevel next() {
            ZoomLevel[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public ZoomLevel previous() {
            ZoomLevel[] values = values();
            return values[(ordinal() - 1 + values.length) % values.length];
        }
    }

    private final Factions plugin;
    private final Player player;
    private final FPlayer fPlayer;
    private int centerX; // in chunks
    private int centerZ; // in chunks
    private final String title;
    private ZoomLevel zoomLevel;

    private int mapMinX, mapMaxX, mapMinZ, mapMaxZ; // in chunks
    private int mapWidth, mapHeight; // in characters

    private final Map<Integer, Region> regionMap = new HashMap<>();

    private TextDisplay display;
    private float lastYaw;
    private int lastPlayerChunkX;
    private int lastPlayerChunkZ;

    /**
     * Creates a new RegionMapDisplay centered on the player's location.
     */
    public RegionMapDisplay(@NotNull Factions plugin, @NotNull Player player, @NotNull FPlayer fPlayer) {
        this(plugin, player, fPlayer,
                player.getLocation().getBlockX() >> 4,
                player.getLocation().getBlockZ() >> 4,
                "Region Map", ZoomLevel.CLOSE);
    }

    /**
     * Creates a new RegionMapDisplay centered on the player's location with specified zoom.
     */
    public RegionMapDisplay(@NotNull Factions plugin, @NotNull Player player, @NotNull FPlayer fPlayer,
                            @NotNull ZoomLevel zoomLevel) {
        this(plugin, player, fPlayer,
                player.getLocation().getBlockX() >> 4,
                player.getLocation().getBlockZ() >> 4,
                "Region Map", zoomLevel);
    }

    /**
     * Creates a new RegionMapDisplay centered on a specific region.
     */
    public RegionMapDisplay(@NotNull Factions plugin, @NotNull Player player, @NotNull FPlayer fPlayer,
                            @NotNull Region region) {
        this(plugin, player, fPlayer,
                getRegionCenterX(region),
                getRegionCenterZ(region),
                "Region: " + region.getName(), ZoomLevel.CLOSE);
    }

    /**
     * Creates a new RegionMapDisplay with custom center coordinates.
     */
    public RegionMapDisplay(@NotNull Factions plugin, @NotNull Player player, @NotNull FPlayer fPlayer,
                            int centerX, int centerZ, @NotNull String title, @NotNull ZoomLevel zoomLevel) {
        this.plugin = plugin;
        this.player = player;
        this.fPlayer = fPlayer;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.title = title;
        this.zoomLevel = zoomLevel;
    }

    public void show() {
        RegionCache cache = plugin.getRegionManager().getCache(player.getWorld());
        if (cache == null) {
            return;
        }

        removeExisting(player.getUniqueId());

        calculateMapBounds();
        Component mapComponent = buildMapComponent(cache);

        lastYaw = player.getLocation().getYaw();
        lastPlayerChunkX = player.getLocation().getBlockX() >> 4;
        lastPlayerChunkZ = player.getLocation().getBlockZ() >> 4;

        display = player.getWorld().spawn(player.getLocation(), TextDisplay.class, entity -> {
            entity.text(mapComponent);
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            entity.setLineWidth(Integer.MAX_VALUE);
            entity.setInterpolationDuration(2);
            entity.setTeleportDuration(2);
            entity.setSeeThrough(false);
            entity.setShadowed(true);
            entity.setBackgroundColor(Color.fromARGB(220, 20, 20, 30));
            entity.setTransformation(createTransformation());
            entity.setVisibleByDefault(false);
            entity.setPersistent(false);
            entity.setRotation(lastYaw + 180, 0);
        });

        player.addPassenger(display);
        player.showEntity(plugin, display);
        activeDisplays.put(player.getUniqueId(), this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void calculateMapBounds() {
        int radiusInChars = zoomLevel.getRadius();
        int radiusInChunks;

        if (zoomLevel.isDetailedZoom()) {
            // For detailed zoom: radius in chars / chars per chunk = radius in chunks
            radiusInChunks = radiusInChars / zoomLevel.getCharsPerChunk();
        } else {
            // For normal/zoomed out: radius in chars * chunks per char = radius in chunks
            radiusInChunks = radiusInChars * zoomLevel.getActualChunksPerChar();
        }

        mapMinX = centerX - radiusInChunks;
        mapMaxX = centerX + radiusInChunks;
        mapMinZ = centerZ - radiusInChunks;
        mapMaxZ = centerZ + radiusInChunks;

        if (zoomLevel.isDetailedZoom()) {
            // For detailed zoom: each chunk is multiple chars
            int chunksWide = mapMaxX - mapMinX + 1;
            int chunksHigh = mapMaxZ - mapMinZ + 1;
            mapWidth = chunksWide * zoomLevel.getCharsPerChunk();
            mapHeight = chunksHigh * zoomLevel.getCharsPerChunk();
        } else {
            // For normal/zoomed out: multiple chunks per char
            mapWidth = (mapMaxX - mapMinX) / zoomLevel.getActualChunksPerChar() + 1;
            mapHeight = (mapMaxZ - mapMinZ) / zoomLevel.getActualChunksPerChar() + 1;
        }
    }

    private Transformation createTransformation() {
        Vector3f translation = new Vector3f(0, UI_VERTICAL_OFFSET, -UI_FORWARD_OFFSET);

        AxisAngle4f leftRotation = new AxisAngle4f(0, 0, 0, 1);
        Vector3f scaleVec = new Vector3f(zoomLevel.getScale(), zoomLevel.getScale(), zoomLevel.getScale());
        AxisAngle4f rightRotation = new AxisAngle4f(0, 0, 0, 1);

        return new Transformation(translation, leftRotation, scaleVec, rightRotation);
    }

    private void updateDisplayRotation(float newYaw) {
        if (display == null || !display.isValid()) return;

        lastYaw = newYaw;
        display.setRotation(newYaw + 180, 0);
    }

    private Component buildMapComponent(RegionCache cache) {
        Map<Integer, TextColor> regionColors = new HashMap<>();
        Map<Integer, TextColor> regionBorderColors = new HashMap<>();
        Map<Integer, String> regionNames = new HashMap<>();

        // Build the map text
        TextComponent.Builder mapBuilder = Component.text();

        // Header
        mapBuilder.append(Component.text("═══ " + title + " ═══", NamedTextColor.GOLD));
        mapBuilder.append(Component.newline());
        mapBuilder.append(Component.text("Zoom: " + zoomLevel.getDisplayName(), NamedTextColor.AQUA));
        mapBuilder.append(Component.text(" | ", NamedTextColor.DARK_GRAY));
        if (zoomLevel.isDetailedZoom()) {
            mapBuilder.append(Component.text(zoomLevel.getCharsPerChunk() + ":1", NamedTextColor.GRAY));
        } else {
            mapBuilder.append(Component.text("1:" + zoomLevel.getActualChunksPerChar(), NamedTextColor.GRAY));
        }
        mapBuilder.append(Component.newline());
        mapBuilder.append(Component.newline());

        // Player is always at center
        int playerCharX = mapWidth / 2;
        int playerCharZ = mapHeight / 2;

        // Render map rows
        for (int charZ = 0; charZ < mapHeight; charZ++) {
            for (int charX = 0; charX < mapWidth; charX++) {
                boolean isPlayerPos = (charX == playerCharX && charZ == playerCharZ);

                if (isPlayerPos) {
                    mapBuilder.append(Component.text("◆", NamedTextColor.GOLD));
                    continue;
                }

                // Calculate which chunk this character represents
                int chunkX, chunkZ;
                boolean isBorderChar = false; // For detailed view: is this char at the edge of its chunk?

                if (zoomLevel.isDetailedZoom()) {
                    // Detailed zoom: multiple chars per chunk
                    int charsPerChunk = zoomLevel.getCharsPerChunk();
                    chunkX = mapMinX + (charX / charsPerChunk);
                    chunkZ = mapMinZ + (charZ / charsPerChunk);
                    // Check if this char is at the border of its chunk
                    int localX = charX % charsPerChunk;
                    int localZ = charZ % charsPerChunk;
                    isBorderChar = (localX == 0 || localX == charsPerChunk - 1 ||
                                   localZ == 0 || localZ == charsPerChunk - 1);
                } else {
                    // Normal/zoomed out: multiple chunks per char
                    chunkX = mapMinX + charX * zoomLevel.getActualChunksPerChar();
                    chunkZ = mapMinZ + charZ * zoomLevel.getActualChunksPerChar();
                }

                // Find the region for this cell
                Region dominantRegion;
                if (zoomLevel.isDetailedZoom()) {
                    // Detailed zoom: single chunk lookup
                    dominantRegion = cache.getByChunk(new LazyChunk(chunkX, chunkZ));
                } else {
                    // Zoomed out: find dominant region in area
                    dominantRegion = findDominantRegion(cache, chunkX, chunkZ,
                            zoomLevel.getActualChunksPerChar());
                }

                if (dominantRegion == null) {
                    mapBuilder.append(Component.text(WILDERNESS_CHAR, NamedTextColor.DARK_GRAY));
                } else {
                    if (!regionColors.containsKey(dominantRegion.getId())) {
                        TextColor baseColor = getRegionColor(dominantRegion);
                        regionColors.put(dominantRegion.getId(), baseColor);
                        regionBorderColors.put(dominantRegion.getId(), darkenColor(baseColor, 0.5f));
                        regionNames.put(dominantRegion.getId(), dominantRegion.getName());
                        regionMap.put(dominantRegion.getId(), dominantRegion);
                    }

                    // Check if this is a border cell
                    boolean isBorder;
                    if (zoomLevel.isDetailedZoom()) {
                        // For detailed zoom: check if this chunk borders a different region
                        // and this char is at the edge of the chunk facing that direction
                        isBorder = isBorderChar && isChunkAtRegionBorder(cache, chunkX, chunkZ, dominantRegion.getId());
                    } else {
                        isBorder = isRegionBorderCell(cache, chunkX, chunkZ,
                                zoomLevel.getActualChunksPerChar(), dominantRegion.getId());
                    }
                    TextColor color = isBorder ? regionBorderColors.get(dominantRegion.getId())
                            : regionColors.get(dominantRegion.getId());
                    mapBuilder.append(Component.text(CHUNK_CHAR, color));
                }
            }
            mapBuilder.append(Component.newline());
        }

        // Legend
        mapBuilder.append(Component.newline());
        mapBuilder.append(Component.text("◆", NamedTextColor.GOLD));
        mapBuilder.append(Component.text("=You ", NamedTextColor.GRAY));
        mapBuilder.append(Component.text(WILDERNESS_CHAR, NamedTextColor.DARK_GRAY));
        mapBuilder.append(Component.text("=Wild ", NamedTextColor.GRAY));
        mapBuilder.append(Component.text("(bright=owned, faded=unclaimed)", NamedTextColor.DARK_GRAY));

        // Region names list (for closer zoom levels)
        if (!regionNames.isEmpty() && zoomLevel.ordinal() <= ZoomLevel.MEDIUM.ordinal()) {
            mapBuilder.append(Component.newline());
            mapBuilder.append(Component.text("Regions: ", NamedTextColor.GRAY));

            int count = 0;
            for (Map.Entry<Integer, String> entry : regionNames.entrySet()) {
                if (count > 0) {
                    mapBuilder.append(Component.text(", ", NamedTextColor.DARK_GRAY));
                }
                TextColor color = regionColors.get(entry.getKey());
                mapBuilder.append(Component.text(entry.getValue(), color));
                count++;
                if (count >= 8) {
                    int remaining = regionNames.size() - count;
                    if (remaining > 0) {
                        mapBuilder.append(Component.text(" +" + remaining + " more", NamedTextColor.DARK_GRAY));
                    }
                    break;
                }
            }
        }

        return mapBuilder.build();
    }

    /**
     * Finds the dominant region within a cell area.
     * For zoom level 1, this is a single chunk lookup.
     * For higher zoom levels, samples multiple chunks and returns the most common region.
     */
    private Region findDominantRegion(RegionCache cache, int startX, int startZ, int size) {
        if (size == 1) {
            return cache.getByChunk(new LazyChunk(startX, startZ));
        }

        // Check global tile cache first
        World world = player.getWorld();
        UUID worldId = world.getUID();
        Map<ZoomLevel, Map<Long, Integer>> worldTileCache = tileCache.get(worldId);
        if (worldTileCache != null) {
            Map<Long, Integer> zoomTileCache = worldTileCache.get(zoomLevel);
            if (zoomTileCache != null) {
                long tileKey = ((long) startX << 32) | (startZ & 0xFFFFFFFFL);
                Integer cachedRegionId = zoomTileCache.get(tileKey);
                if (cachedRegionId != null) {
                    return cachedRegionId == -1 ? null : regionMap.computeIfAbsent(cachedRegionId,
                            id -> plugin.getRegionManager().getRegionById(id));
                }
            }
        }

        // Sample the cell area to find dominant region
        Map<Integer, Integer> regionCounts = new HashMap<>();
        int sampleStep = Math.max(1, size / 2); // Sample at most 4 points for performance

        for (int dz = 0; dz < size; dz += sampleStep) {
            for (int dx = 0; dx < size; dx += sampleStep) {
                Region r = cache.getByChunk(new LazyChunk(startX + dx, startZ + dz));
                int regionId = r == null ? -1 : r.getId();
                if (regionId != -1) {
                    regionCounts.merge(regionId, 1, Integer::sum);
                }
            }
        }

        if (regionCounts.isEmpty()) {
            return null;
        }

        // Find region with highest count
        int maxCount = 0;
        int dominantId = -1;
        for (Map.Entry<Integer, Integer> entry : regionCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                dominantId = entry.getKey();
            }
        }

        // Update global tile cache
        worldTileCache = tileCache.computeIfAbsent(worldId, k -> new ConcurrentHashMap<>());
        Map<Long, Integer> zoomTileCacheForUpdate = worldTileCache.computeIfAbsent(zoomLevel, k -> new ConcurrentHashMap<>());
        long tileKey = ((long) startX << 32) | (startZ & 0xFFFFFFFFL);
        zoomTileCacheForUpdate.put(tileKey, dominantId);

        return dominantId == -1 ? null : regionMap.computeIfAbsent(dominantId,
                id -> plugin.getRegionManager().getRegionById(id));
    }

    /**
     * Checks if a cell is at the border of a region.
     */
    private boolean isRegionBorderCell(RegionCache cache, int startX, int startZ, int size, int regionId) {
        int[] dx = {size, -size, 0, 0};
        int[] dz = {0, 0, size, -size};

        for (int i = 0; i < 4; i++) {
            int checkX = startX + dx[i];
            int checkZ = startZ + dz[i];

            // Check global border cache first
            World world = player.getWorld();
            UUID worldId = world.getUID();
            Map<ZoomLevel, Map<Long, Boolean>> worldBorderCache = borderCache.get(worldId);
            if (worldBorderCache != null) {
                Map<Long, Boolean> zoomBorderCache = worldBorderCache.get(zoomLevel);
                if (zoomBorderCache != null) {
                    long tileKey = ((long) checkX << 32) | (checkZ & 0xFFFFFFFFL);
                    Boolean cachedIsBorder = zoomBorderCache.get(tileKey);
                    if (cachedIsBorder != null) {
                        return cachedIsBorder;
                    }
                }
            }

            Region neighbor = cache.getByChunk(new LazyChunk(checkX, checkZ));
            int neighborId = neighbor == null ? -1 : neighbor.getId();

            boolean isBorder = neighborId != regionId;

            // Update global border cache
            worldBorderCache = borderCache.computeIfAbsent(worldId, k -> new ConcurrentHashMap<>());
            Map<Long, Boolean> zoomBorderCacheForUpdate = worldBorderCache.computeIfAbsent(zoomLevel, k -> new ConcurrentHashMap<>());
            long tileKey = ((long) checkX << 32) | (checkZ & 0xFFFFFFFFL);
            zoomBorderCacheForUpdate.put(tileKey, isBorder);

            if (isBorder) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a chunk is at the border of its region (has a neighbor with different region).
     * Used for detailed zoom levels.
     */
    private boolean isChunkAtRegionBorder(RegionCache cache, int chunkX, int chunkZ, int regionId) {
        int[] dx = {1, -1, 0, 0};
        int[] dz = {0, 0, 1, -1};

        for (int i = 0; i < 4; i++) {
            Region neighbor = cache.getByChunk(new LazyChunk(chunkX + dx[i], chunkZ + dz[i]));
            int neighborId = neighbor == null ? -1 : neighbor.getId();
            if (neighborId != regionId) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handles player movement to update display rotation when player turns,
     * and refresh map content when player crosses a chunk border.
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.getPlayer().equals(player)) return;
        if (display == null || !display.isValid()) return;

        float newYaw = event.getTo().getYaw();
        if (Math.abs(newYaw - lastYaw) > 0.01f) {
            updateDisplayRotation(newYaw);
        }

        int newChunkX = event.getTo().getBlockX() >> 4;
        int newChunkZ = event.getTo().getBlockZ() >> 4;

        if (newChunkX != lastPlayerChunkX || newChunkZ != lastPlayerChunkZ) {
            lastPlayerChunkX = newChunkX;
            lastPlayerChunkZ = newChunkZ;
            refreshMapContent();
        }
    }

    /**
     * Refreshes the map content, recentering on the player's current position.
     */
    private void refreshMapContent() {
        if (display == null || !display.isValid()) return;

        RegionCache cache = plugin.getRegionManager().getCache(player.getWorld());
        if (cache == null) return;

        // Recenter map on player
        centerX = lastPlayerChunkX;
        centerZ = lastPlayerChunkZ;

        // Recalculate map bounds
        calculateMapBounds();

        // Rebuild map text
        regionMap.clear();
        Component mapComponent = buildMapComponent(cache);
        display.text(mapComponent);
    }

    /**
     * Handles player quit to clean up display.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer().equals(player)) {
            remove();
        }
    }

    /**
     * Gets the color for a region based on its Alliance.
     * Owned regions (with a faction) use the full alliance color.
     * Unclaimed regions (no faction owner) use a lighter/faded version of the alliance color.
     * Regions without an alliance use gray.
     */
    private TextColor getRegionColor(Region region) {
        Alliance alliance = region.getAlliance();
        if (alliance == null) {
            return NamedTextColor.GRAY;
        }

        TextColor allianceColor = alliance.getColor();
        if (allianceColor == null) {
            return NamedTextColor.GRAY;
        }

        // Check if region is owned by a faction
        if (region.isOwned() && region.getOwner() != null) {
            // Full alliance color for owned regions
            return allianceColor;
        } else {
            // Lighter/faded color for unclaimed regions
            return lightenColor(allianceColor, 0.4f);
        }
    }

    /**
     * Lightens a color by blending it with white.
     */
    private TextColor lightenColor(TextColor color, float factor) {
        int r = (int) (color.red() + (255 - color.red()) * factor);
        int g = (int) (color.green() + (255 - color.green()) * factor);
        int b = (int) (color.blue() + (255 - color.blue()) * factor);
        return TextColor.color(r, g, b);
    }

    private TextColor darkenColor(TextColor color, float factor) {
        int r = (int) (color.red() * factor);
        int g = (int) (color.green() * factor);
        int b = (int) (color.blue() * factor);
        return TextColor.color(r, g, b);
    }

    private static int getRegionCenterX(Region region) {
        if (region.getChunks().isEmpty()) {
            return 0;
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        for (LazyChunk chunk : region.getChunks()) {
            minX = Math.min(minX, chunk.getX());
            maxX = Math.max(maxX, chunk.getX());
        }
        return (minX + maxX) / 2;
    }

    private static int getRegionCenterZ(Region region) {
        if (region.getChunks().isEmpty()) {
            return 0;
        }
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (LazyChunk chunk : region.getChunks()) {
            minZ = Math.min(minZ, chunk.getZ());
            maxZ = Math.max(maxZ, chunk.getZ());
        }
        return (minZ + maxZ) / 2;
    }

    /**
     * Removes the display and cleans up resources.
     */
    public void remove() {
        if (display != null && display.isValid()) {
            display.remove();
        }
        display = null;
        HandlerList.unregisterAll(this);
        activeDisplays.remove(player.getUniqueId());
    }

    /**
     * Removes an existing display for a player if one exists.
     */
    public static void removeExisting(@NotNull UUID playerId) {
        RegionMapDisplay existing = activeDisplays.get(playerId);
        if (existing != null) {
            existing.remove();
        }
    }

    /**
     * Checks if a player has an active map display.
     */
    public static boolean hasActiveDisplay(@NotNull UUID playerId) {
        return activeDisplays.containsKey(playerId);
    }

    /**
     * Gets the active display for a player, if any.
     */
    @Nullable
    public static RegionMapDisplay getActiveDisplay(@NotNull UUID playerId) {
        return activeDisplays.get(playerId);
    }

    /**
     * Invalidates the global tile cache for a specific world.
     * Call this when regions change in that world.
     */
    public static void invalidateCache(@NotNull UUID worldId) {
        tileCache.remove(worldId);
        borderCache.remove(worldId);
    }

    /**
     * Invalidates the entire global tile cache.
     * Call this when regions change globally.
     */
    public static void invalidateAllCaches() {
        tileCache.clear();
        borderCache.clear();
    }

    public ZoomLevel getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(ZoomLevel zoomLevel) {
        this.zoomLevel = zoomLevel;
        if (display != null && display.isValid()) {
            display.setTransformation(createTransformation());
            refreshMapContent();
        }
    }

    public void zoomIn() {
        setZoomLevel(zoomLevel.previous());
    }

    public void zoomOut() {
        setZoomLevel(zoomLevel.next());
    }
}
