package de.erethon.factions.region;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.erethon.factions.Factions;
import de.erethon.factions.util.FLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Calculates region borders (polygons) and neighbor relationships.
 *
 * @author Malfrador
 */
public class RegionBorderCalculator {

    private static final int WILDERNESS_ID = -1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Factions plugin;
    private final File cacheFolder;

    private final Map<UUID, WorldBorderData> worldBorderData = new HashMap<>();

    public RegionBorderCalculator(@NotNull Factions plugin, @NotNull File cacheFolder) {
        this.plugin = plugin;
        this.cacheFolder = cacheFolder;
        if (!cacheFolder.exists()) {
            cacheFolder.mkdirs();
        }
    }

    public void loadCache() {
        for (RegionCache regionCache : plugin.getRegionManager()) {
            UUID worldId = regionCache.getWorldId();
            File cacheFile = getCacheFile(worldId);
            if (cacheFile.exists()) {
                try {
                    WorldBorderData data = loadFromFile(cacheFile);
                    if (data != null) {
                        worldBorderData.put(worldId, data);
                        FLogger.REGION.log("Loaded border cache for world " + worldId + " with " + data.regionData.size() + " regions");
                    }
                } catch (Exception e) {
                    FLogger.ERROR.log("Failed to load border cache for world " + worldId + ": " + e.getMessage());
                }
            }
        }
    }

    public void saveCache() {
        for (Map.Entry<UUID, WorldBorderData> entry : worldBorderData.entrySet()) {
            try {
                saveToFile(getCacheFile(entry.getKey()), entry.getValue());
            } catch (IOException e) {
                FLogger.ERROR.log("Failed to save border cache for world " + entry.getKey() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Recalculates borders and neighbors for all regions in a world.
     *
     * @param worldId The world UUID to recalculate
     * @return CompletableFuture that completes when calculation is done
     */
    public CompletableFuture<WorldBorderData> recalculateWorld(@NotNull UUID worldId) {
        return recalculateWorld(worldId, null);
    }

    /**
     * Recalculates borders and neighbors for all regions in a world.
     *
     * @param worldId The world UUID to recalculate
     * @param progressCallback Optional callback for progress updates (current, total, regionName)
     * @return CompletableFuture that completes when calculation is done
     */
    public CompletableFuture<WorldBorderData> recalculateWorld(@NotNull UUID worldId,
            @Nullable ProgressCallback progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            FLogger.INFO.log("Starting border calculation for world " + worldId + "...");
            long startTime = System.currentTimeMillis();

            RegionCache regionCache = plugin.getRegionManager().getCache(worldId);
            if (regionCache == null) {
                FLogger.ERROR.log("No region cache found for world " + worldId);
                return null;
            }

            // Step 1: Create snapshot of chunk data
            FLogger.INFO.log("Creating chunk grid snapshot...");
            ChunkGrid grid = createChunkGrid(regionCache);

            int totalRegions = regionCache.getSize();
            FLogger.INFO.log("Processing " + totalRegions + " regions...");

            // Step 2: Process each region
            WorldBorderData worldData = new WorldBorderData(worldId);
            int processed = 0;

            for (Region region : regionCache) {
                try {
                    RegionBorderData borderData = processRegion(region, grid);
                    worldData.regionData.put(region.getId(), borderData);
                    processed++;

                    // Report progress
                    if (progressCallback != null) {
                        final int current = processed;
                        final String regionName = region.getName();
                        plugin.getServer().getScheduler().runTask(plugin, () ->
                            progressCallback.onProgress(current, totalRegions, regionName));
                    }

                    // Log progress every 10% or every 50 regions
                    if (processed % Math.max(1, totalRegions / 10) == 0 || processed % 50 == 0) {
                        int percent = (int) ((processed / (double) totalRegions) * 100);
                        FLogger.INFO.log("Border calculation progress: " + processed + "/" + totalRegions + " (" + percent + "%)");
                    }
                } catch (Exception e) {
                    FLogger.ERROR.log("Failed to process region " + region.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                    processed++;
                }
            }

            // Step 3: Update the actual Region objects with neighbor data (on main thread)
            plugin.getServer().getScheduler().runTask(plugin, () ->
                applyNeighborData(regionCache, worldData));

            long elapsed = System.currentTimeMillis() - startTime;
            FLogger.INFO.log("Border calculation completed for world " + worldId + " in " + elapsed + "ms (" + totalRegions + " regions)");

            worldBorderData.put(worldId, worldData);

            // Save to disk
            try {
                saveToFile(getCacheFile(worldId), worldData);
            } catch (IOException e) {
                FLogger.ERROR.log("Failed to save border cache: " + e.getMessage());
            }

            return worldData;
        });
    }

    @FunctionalInterface
    public interface ProgressCallback {
        /**
         * Called when a region has been processed.
         * @param current The number of regions processed so far
         * @param total The total number of regions to process
         * @param regionName The name of the region that was just processed
         */
        void onProgress(int current, int total, String regionName);
    }

    /**
     * Recalculates borders for a single region (after chunks were added/removed).
     */
    public CompletableFuture<RegionBorderData> recalculateRegion(@NotNull Region region) {
        return CompletableFuture.supplyAsync(() -> {
            RegionCache regionCache = region.getRegionCache();
            ChunkGrid grid = createChunkGrid(regionCache);
            RegionBorderData borderData = processRegion(region, grid);

            WorldBorderData worldData = worldBorderData.computeIfAbsent(
                    region.getWorldId(), WorldBorderData::new);
            worldData.regionData.put(region.getId(), borderData);

            // Update neighbors on main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                applyNeighborDataForRegion(regionCache, region.getId(), borderData);
            });

            return borderData;
        });
    }

    public @Nullable RegionBorderData getBorderData(@NotNull UUID worldId, int regionId) {
        WorldBorderData worldData = worldBorderData.get(worldId);
        return worldData == null ? null : worldData.regionData.get(regionId);
    }

    public @Nullable WorldBorderData getWorldBorderData(@NotNull UUID worldId) {
        return worldBorderData.get(worldId);
    }


    private ChunkGrid createChunkGrid(@NotNull RegionCache regionCache) {
        ChunkGrid grid = new ChunkGrid();
        for (Region region : regionCache) {
            for (LazyChunk chunk : region.getChunks()) {
                grid.set(chunk.getX(), chunk.getZ(), region.getId());
            }
        }
        return grid;
    }

    private RegionBorderData processRegion(@NotNull Region region, @NotNull ChunkGrid grid) {
        int regionId = region.getId();
        Set<LazyChunk> chunks = region.getChunks();

        if (chunks.isEmpty()) {
            return new RegionBorderData(regionId, List.of(), List.of(), Set.of(), true);
        }

        // Step 1: Validate contiguity
        boolean isContiguous = validateContiguity(chunks, regionId);
        if (!isContiguous) {
            FLogger.WARN.log("Region " + regionId + " (" + region.getName() + ") is not contiguous!");
        }

        // Step 2: Find edge chunks and neighbors
        Set<LazyChunk> edgeChunks = new HashSet<>();
        Set<Integer> neighbors = new HashSet<>();

        for (LazyChunk chunk : chunks) {
            int x = chunk.getX();
            int z = chunk.getZ();

            // Check 4 cardinal neighbors
            int[] neighborIds = {
                    grid.get(x + 1, z),
                    grid.get(x - 1, z),
                    grid.get(x, z + 1),
                    grid.get(x, z - 1)
            };

            boolean isEdge = false;
            for (int neighborId : neighborIds) {
                if (neighborId != regionId) {
                    isEdge = true;
                    if (neighborId != WILDERNESS_ID) {
                        neighbors.add(neighborId);
                    }
                }
            }

            if (isEdge) {
                edgeChunks.add(chunk);
            }
        }

        // Step 3: Trace polygon outline from edge chunks
        List<int[]> outerPolygon = tracePolygon(edgeChunks, grid, regionId, true);
        List<List<int[]>> holes = findAndTraceHoles(chunks, edgeChunks, grid, regionId);

        return new RegionBorderData(regionId, outerPolygon, holes, neighbors, isContiguous);
    }

    /**
     * Validates that all chunks of a region are connected using flood fill (BFS).
     */
    private boolean validateContiguity(@NotNull Set<LazyChunk> chunks, int regionId) {
        if (chunks.size() <= 1) {
            return true;
        }

        Set<LazyChunk> visited = new HashSet<>();
        Queue<LazyChunk> queue = new LinkedList<>();

        // Start from any chunk
        LazyChunk start = chunks.iterator().next();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            LazyChunk current = queue.poll();
            int x = current.getX();
            int z = current.getZ();

            // Check 4 cardinal neighbors
            LazyChunk[] neighbors = {
                    new LazyChunk(x + 1, z),
                    new LazyChunk(x - 1, z),
                    new LazyChunk(x, z + 1),
                    new LazyChunk(x, z - 1)
            };

            for (LazyChunk neighbor : neighbors) {
                if (chunks.contains(neighbor) && !visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return visited.size() == chunks.size();
    }

    /**
     * Traces the polygon outline of a region using contour tracing.
     * Returns vertices in block coordinates (chunk * 16).
     */
    private List<int[]> tracePolygon(@NotNull Set<LazyChunk> edgeChunks, @NotNull ChunkGrid grid,
                                     int regionId, boolean clockwise) {
        if (edgeChunks.isEmpty()) {
            return List.of();
        }

        // Find the starting point: topmost-leftmost edge chunk
        LazyChunk start = null;
        for (LazyChunk chunk : edgeChunks) {
            if (start == null || chunk.getZ() < start.getZ() ||
                    (chunk.getZ() == start.getZ() && chunk.getX() < start.getX())) {
                start = chunk;
            }
        }

        List<int[]> vertices = new ArrayList<>();

        // Use marching squares to trace the boundary
        // We work with cell corners (vertices), not chunk centers
        // Starting from top-left corner of the starting chunk, trace clockwise

        Set<String> visitedEdges = new HashSet<>();
        int startX = start.getX();
        int startZ = start.getZ();

        // Start at top-left corner of the topmost-leftmost chunk
        // and trace the outer boundary clockwise
        traceContour(vertices, visitedEdges, grid, regionId, startX, startZ, edgeChunks);

        // Simplify: remove collinear points
        return simplifyPolygon(vertices);
    }

    /**
     * Traces the contour starting from a given position.
     */
    private void traceContour(List<int[]> vertices, Set<String> visitedEdges,
                              ChunkGrid grid, int regionId, int startX, int startZ,
                              Set<LazyChunk> edgeChunks) {

        int x = startX;
        int z = startZ;
        int edgeDir = 0;
        vertices.add(new int[]{x * 16, z * 16});
        int startEdgeDir = edgeDir;
        boolean firstMove = true;

        do {
            String edgeKey = x + "," + z + "," + edgeDir;
            if (!firstMove && visitedEdges.contains(edgeKey)) {
                break;
            }
            visitedEdges.add(edgeKey);
            firstMove = false;

            int nextX = x, nextZ = z;
            int outsideX = x, outsideZ = z;

            switch (edgeDir) {
                case 0 -> { // Walking right along top edge
                    nextX = x + 1; // Next chunk to the right
                    outsideZ = z - 1; // Outside is above
                }
                case 1 -> { // Walking down along right edge
                    nextZ = z + 1; // Next chunk below
                    outsideX = x + 1; // Outside is to the right
                }
                case 2 -> { // Walking left along bottom edge
                    nextX = x - 1; // Next chunk to the left
                    outsideZ = z + 1; // Outside is below
                }
                case 3 -> { // Walking up along left edge
                    nextZ = z - 1; // Next chunk above
                    outsideX = x - 1; // Outside is to the left
                }
            }

            boolean nextIsRegion = grid.get(nextX, nextZ) == regionId;
            boolean outsideIsRegion = grid.get(outsideX, outsideZ) == regionId;

            if (outsideIsRegion && !nextIsRegion) {
                // Concave corner: outside is our region but we can't go forward
                // Turn left into the outside chunk
                int[] cornerVertex = getOuterCornerVertex(x, z, edgeDir);
                if (!arraysEqual(vertices.getLast(), cornerVertex)) {
                    vertices.add(cornerVertex);
                }
                x = outsideX;
                z = outsideZ;
                edgeDir = (edgeDir + 3) % 4; // Turn left
            } else if (nextIsRegion) {
                // Can continue forward (next chunk is ours)
                int[] cornerVertex = getOuterCornerVertex(x, z, edgeDir);
                int nextOutsideX = nextX, nextOutsideZ = nextZ;
                switch (edgeDir) {
                    case 0 -> nextOutsideZ = nextZ - 1;
                    case 1 -> nextOutsideX = nextX + 1;
                    case 2 -> nextOutsideZ = nextZ + 1;
                    case 3 -> nextOutsideX = nextX - 1;
                }
                boolean nextOutsideIsRegion = grid.get(nextOutsideX, nextOutsideZ) == regionId;
                if (nextOutsideIsRegion) {
                    // Inner corner: add vertex and turn left after moving
                    if (!arraysEqual(vertices.getLast(), cornerVertex)) {
                        vertices.add(cornerVertex);
                    }
                    edgeDir = (edgeDir + 3) % 4; // Turn left
                }
                x = nextX;
                z = nextZ;
            } else {
                // Convex corner: neither outside nor next is our region
                // Add corner vertex and turn right
                int[] cornerVertex = getOuterCornerVertex(x, z, edgeDir);
                if (!arraysEqual(vertices.getLast(), cornerVertex)) {
                    vertices.add(cornerVertex);
                }
                edgeDir = (edgeDir + 1) % 4;
            }

            // Safety limit
            if (vertices.size() > edgeChunks.size() * 8) {
                FLogger.WARN.log("Polygon tracing exceeded safety limit for region " + regionId);
                break;
            }

        } while (x != startX || z != startZ || edgeDir != startEdgeDir);

        if (!vertices.isEmpty() && !arraysEqual(vertices.getFirst(), vertices.getLast())) {
            int[] finalVertex = getOuterCornerVertex(x, z, edgeDir);
            if (!arraysEqual(vertices.getLast(), finalVertex) && !arraysEqual(vertices.getFirst(), finalVertex)) {
                vertices.add(finalVertex);
            }
        }
    }

    /**
     * Gets the outer corner vertex for a chunk edge.
     * This is the corner at the END of walking along that edge (clockwise).
     */
    private int[] getOuterCornerVertex(int chunkX, int chunkZ, int edgeDir) {
        int blockX = chunkX * 16;
        int blockZ = chunkZ * 16;

        // When walking clockwise, each edge ends at a specific outer corner:
        return switch (edgeDir) {
            case 0 -> new int[]{blockX + 16, blockZ};       // top edge ends at top-right
            case 1 -> new int[]{blockX + 16, blockZ + 16};  // right edge ends at bottom-right
            case 2 -> new int[]{blockX, blockZ + 16};       // bottom edge ends at bottom-left
            case 3 -> new int[]{blockX, blockZ};            // left edge ends at top-left
            default -> new int[]{blockX, blockZ};
        };
    }

    /**
     * Finds holes (enclosed regions) within this region and traces their boundaries.
     * A hole exists when another region is completely surrounded by this region.
     */
    private List<List<int[]>> findAndTraceHoles(Set<LazyChunk> allChunks, Set<LazyChunk> edgeChunks,
                                                ChunkGrid grid, int regionId) {
        // Find all regions that are adjacent to this region (potential holes)
        // A region is a "hole" if ALL of its edge chunks only border this region (and no wilderness/other regions)

        // First, collect which regions border this one and from which chunks
        Map<Integer, Set<LazyChunk>> borderingRegions = new HashMap<>();

        for (LazyChunk chunk : edgeChunks) {
            int x = chunk.getX();
            int z = chunk.getZ();

            int[] neighborCoords = {
                    x + 1, z,
                    x - 1, z,
                    x, z + 1,
                    x, z - 1
            };

            for (int i = 0; i < neighborCoords.length; i += 2) {
                int neighborId = grid.get(neighborCoords[i], neighborCoords[i + 1]);
                if (neighborId != regionId && neighborId != WILDERNESS_ID) {
                    borderingRegions.computeIfAbsent(neighborId, k -> new HashSet<>()).add(chunk);
                }
            }
        }

        // For each bordering region, check if it's completely enclosed
        List<List<int[]>> holes = new ArrayList<>();

        for (Map.Entry<Integer, Set<LazyChunk>> entry : borderingRegions.entrySet()) {
            int enclosedRegionId = entry.getKey();

            if (isRegionEnclosed(enclosedRegionId, regionId, grid)) {
                // This region is a hole - trace its boundary from our perspective
                Set<LazyChunk> holeBorderChunks = entry.getValue();
                List<int[]> holePolygon = traceHolePolygon(holeBorderChunks, grid, regionId, enclosedRegionId);
                if (!holePolygon.isEmpty()) {
                    holes.add(holePolygon);
                }
            }
        }

        return holes;
    }

    /**
     * Checks if a region is completely enclosed by another region (no wilderness borders).
     */
    private boolean isRegionEnclosed(int potentiallyEnclosedId, int enclosingId, ChunkGrid grid) {
        // Find the region in any cache
        Region enclosedRegion = null;
        for (RegionCache rc : plugin.getRegionManager()) {
            enclosedRegion = rc.getById(potentiallyEnclosedId);
            if (enclosedRegion != null) break;
        }

        if (enclosedRegion == null) return false;

        // Check all edge chunks of the potentially enclosed region
        for (LazyChunk chunk : enclosedRegion.getChunks()) {
            int x = chunk.getX();
            int z = chunk.getZ();

            int[] neighborIds = {
                    grid.get(x + 1, z),
                    grid.get(x - 1, z),
                    grid.get(x, z + 1),
                    grid.get(x, z - 1)
            };

            for (int neighborId : neighborIds) {
                // If any neighbor is wilderness or a region other than the enclosing one,
                // then this region is NOT enclosed
                if (neighborId != potentiallyEnclosedId && neighborId != enclosingId) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Traces the inner boundary polygon around an enclosed region (hole).
     * This traces the boundary from the perspective of the outer region looking inward.
     */
    private List<int[]> traceHolePolygon(Set<LazyChunk> holeBorderChunks, ChunkGrid grid,
                                         int outerRegionId, int innerRegionId) {
        if (holeBorderChunks.isEmpty()) {
            return List.of();
        }

        // Find starting point: topmost-leftmost chunk that borders the inner region
        LazyChunk start = null;
        for (LazyChunk chunk : holeBorderChunks) {
            if (start == null || chunk.getZ() < start.getZ() ||
                    (chunk.getZ() == start.getZ() && chunk.getX() < start.getX())) {
                start = chunk;
            }
        }

        // Trace counter-clockwise for inner holes (opposite of outer boundary)
        List<int[]> vertices = new ArrayList<>();
        Set<String> visitedEdges = new HashSet<>();

        traceInnerContour(vertices, visitedEdges, grid, outerRegionId, innerRegionId,
                         start.getX(), start.getZ(), holeBorderChunks);

        return simplifyPolygon(vertices);
    }

    /**
     * Traces the inner contour around a hole (counter-clockwise).
     */
    private void traceInnerContour(List<int[]> vertices, Set<String> visitedEdges,
                                   ChunkGrid grid, int outerRegionId, int innerRegionId,
                                   int startX, int startZ, Set<LazyChunk> holeBorderChunks) {
        // Direction: 0=right, 1=down, 2=left, 3=up
        int[][] directions = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

        int x = startX;
        int z = startZ;
        int dir = 0; // Start moving right

        // Find which direction leads to the inner region
        for (int d = 0; d < 4; d++) {
            int nx = x + directions[d][0];
            int nz = z + directions[d][1];
            if (grid.get(nx, nz) == innerRegionId) {
                // Start facing the hole, then turn left to trace counter-clockwise
                dir = (d + 3) % 4; // Turn left from the direction to the hole
                break;
            }
        }

        // Add starting vertex
        vertices.add(calculateHoleVertex(x, z, dir));

        int iterations = 0;
        int maxIterations = holeBorderChunks.size() * 8;

        do {
            // For counter-clockwise traversal, try to turn left first
            int leftDir = (dir + 3) % 4;
            int rightDir = (dir + 1) % 4;

            int[] leftDelta = directions[leftDir];
            int[] forwardDelta = directions[dir];

            int leftX = x + leftDelta[0];
            int leftZ = z + leftDelta[1];
            int forwardX = x + forwardDelta[0];
            int forwardZ = z + forwardDelta[1];

            boolean leftIsOurs = grid.get(leftX, leftZ) == outerRegionId;
            boolean forwardIsOurs = grid.get(forwardX, forwardZ) == outerRegionId;

            if (leftIsOurs) {
                // Turn left and move
                dir = leftDir;
                x = leftX;
                z = leftZ;
            } else if (forwardIsOurs) {
                // Continue forward
                x = forwardX;
                z = forwardZ;
            } else {
                // Turn right (no move)
                dir = rightDir;
            }

            String edgeKey = x + "," + z + "," + dir;
            if (visitedEdges.contains(edgeKey)) {
                break;
            }
            visitedEdges.add(edgeKey);

            int[] vertex = calculateHoleVertex(x, z, dir);
            if (vertices.isEmpty() || !arraysEqual(vertices.getLast(), vertex)) {
                vertices.add(vertex);
            }

            iterations++;
            if (iterations > maxIterations) {
                FLogger.WARN.log("Hole polygon tracing exceeded safety limit");
                break;
            }

        } while (x != startX || z != startZ || vertices.size() < 4);
    }

    /**
     * Calculates vertex position for hole polygon (inner boundary).
     */
    private int[] calculateHoleVertex(int chunkX, int chunkZ, int direction) {
        int blockX = chunkX * 16;
        int blockZ = chunkZ * 16;

        // For inner holes traced counter-clockwise, vertices are on the inner edge
        return switch (direction) {
            case 0 -> new int[]{blockX + 16, blockZ + 16};  // bottom-right
            case 1 -> new int[]{blockX, blockZ + 16};       // bottom-left
            case 2 -> new int[]{blockX, blockZ};            // top-left
            case 3 -> new int[]{blockX + 16, blockZ};       // top-right
            default -> new int[]{blockX, blockZ};
        };
    }

    /**
     * Simplifies a polygon by removing collinear points.
     */
    private List<int[]> simplifyPolygon(List<int[]> vertices) {
        if (vertices.size() < 3) {
            return vertices;
        }

        List<int[]> simplified = new ArrayList<>();
        int n = vertices.size();

        for (int i = 0; i < n; i++) {
            int[] prev = vertices.get((i - 1 + n) % n);
            int[] curr = vertices.get(i);
            int[] next = vertices.get((i + 1) % n);

            // Check if points are collinear
            if (!areCollinear(prev, curr, next)) {
                simplified.add(curr);
            }
        }

        return simplified.isEmpty() ? vertices : simplified;
    }

    private boolean areCollinear(int[] a, int[] b, int[] c) {
        // Cross product of vectors (b-a) and (c-b)
        return (b[0] - a[0]) * (c[1] - b[1]) == (c[0] - b[0]) * (b[1] - a[1]);
    }

    private boolean arraysEqual(int[] a, int[] b) {
        return a[0] == b[0] && a[1] == b[1];
    }

    /**
     * Applies calculated neighbor data to actual Region objects.
     */
    private void applyNeighborData(@NotNull RegionCache regionCache, @NotNull WorldBorderData worldData) {
        for (Map.Entry<Integer, RegionBorderData> entry : worldData.regionData.entrySet()) {
            Region region = regionCache.getById(entry.getKey());
            if (region == null) continue;

            // Clear existing neighbors and add new ones
            region.getAdjacentRegions().clear();
            for (int neighborId : entry.getValue().neighborIds) {
                Region neighbor = regionCache.getById(neighborId);
                if (neighbor != null) {
                    region.getAdjacentRegions().add(neighbor);
                }
            }
        }
        FLogger.INFO.log("Applied neighbor data for " + worldData.regionData.size() + " regions");
    }

    /**
     * Applies neighbor data for a single region.
     */
    private void applyNeighborDataForRegion(@NotNull RegionCache regionCache, int regionId,
                                            @NotNull RegionBorderData borderData) {
        Region region = regionCache.getById(regionId);
        if (region == null) return;

        region.getAdjacentRegions().clear();
        for (int neighborId : borderData.neighborIds) {
            Region neighbor = regionCache.getById(neighborId);
            if (neighbor != null) {
                region.getAdjacentRegions().add(neighbor);
            }
        }
    }

    private File getCacheFile(@NotNull UUID worldId) {
        return new File(cacheFolder, worldId + ".json");
    }

    private WorldBorderData loadFromFile(@NotNull File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return WorldBorderData.fromJson(json);
        }
    }

    private void saveToFile(@NotNull File file, @NotNull WorldBorderData data) throws IOException {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(data.toJson(), writer);
        }
    }

    /**
     * A sparse grid mapping chunk coordinates to region IDs.
     */
    public static class ChunkGrid {
        private final Map<Long, Integer> data = new HashMap<>();

        public void set(int x, int z, int regionId) {
            data.put(packCoords(x, z), regionId);
        }

        public int get(int x, int z) {
            return data.getOrDefault(packCoords(x, z), WILDERNESS_ID);
        }

        private static long packCoords(int x, int z) {
            return ((long) x << 32) | (z & 0xFFFFFFFFL);
        }
    }

    /**
     * Border data for a single region.
     */
    public static class RegionBorderData {
        public final int regionId;
        public final List<int[]> polygon;           // Outer boundary vertices in block coords
        public final List<List<int[]>> holes;       // Inner hole polygons
        public final Set<Integer> neighborIds;      // Adjacent region IDs
        public final boolean isContiguous;          // Whether the region is properly connected

        public RegionBorderData(int regionId, List<int[]> polygon, List<List<int[]>> holes,
                                Set<Integer> neighborIds, boolean isContiguous) {
            this.regionId = regionId;
            this.polygon = polygon;
            this.holes = holes;
            this.neighborIds = neighborIds;
            this.isContiguous = isContiguous;
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("regionId", regionId);
            json.addProperty("isContiguous", isContiguous);

            JsonArray polygonArray = new JsonArray();
            for (int[] vertex : polygon) {
                JsonArray point = new JsonArray();
                point.add(vertex[0]);
                point.add(vertex[1]);
                polygonArray.add(point);
            }
            json.add("polygon", polygonArray);

            JsonArray holesArray = new JsonArray();
            for (List<int[]> hole : holes) {
                JsonArray holeArray = new JsonArray();
                for (int[] vertex : hole) {
                    JsonArray point = new JsonArray();
                    point.add(vertex[0]);
                    point.add(vertex[1]);
                    holeArray.add(point);
                }
                holesArray.add(holeArray);
            }
            json.add("holes", holesArray);

            JsonArray neighborsArray = new JsonArray();
            for (int neighborId : neighborIds) {
                neighborsArray.add(neighborId);
            }
            json.add("neighbors", neighborsArray);

            return json;
        }

        public static RegionBorderData fromJson(JsonObject json) {
            int regionId = json.get("regionId").getAsInt();
            boolean isContiguous = json.get("isContiguous").getAsBoolean();

            List<int[]> polygon = new ArrayList<>();
            for (var element : json.getAsJsonArray("polygon")) {
                JsonArray point = element.getAsJsonArray();
                polygon.add(new int[]{point.get(0).getAsInt(), point.get(1).getAsInt()});
            }

            List<List<int[]>> holes = new ArrayList<>();
            for (var holeElement : json.getAsJsonArray("holes")) {
                List<int[]> hole = new ArrayList<>();
                for (var element : holeElement.getAsJsonArray()) {
                    JsonArray point = element.getAsJsonArray();
                    hole.add(new int[]{point.get(0).getAsInt(), point.get(1).getAsInt()});
                }
                holes.add(hole);
            }

            Set<Integer> neighborIds = new HashSet<>();
            for (var element : json.getAsJsonArray("neighbors")) {
                neighborIds.add(element.getAsInt());
            }

            return new RegionBorderData(regionId, polygon, holes, neighborIds, isContiguous);
        }
    }

    /**
     * Border data for all regions in a world.
     */
    public static class WorldBorderData {
        public final UUID worldId;
        public final Map<Integer, RegionBorderData> regionData = new HashMap<>();

        public WorldBorderData(UUID worldId) {
            this.worldId = worldId;
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("worldId", worldId.toString());

            JsonObject regionsJson = new JsonObject();
            for (Map.Entry<Integer, RegionBorderData> entry : regionData.entrySet()) {
                regionsJson.add(String.valueOf(entry.getKey()), entry.getValue().toJson());
            }
            json.add("regions", regionsJson);

            return json;
        }

        public static WorldBorderData fromJson(JsonObject json) {
            UUID worldId = UUID.fromString(json.get("worldId").getAsString());
            WorldBorderData data = new WorldBorderData(worldId);

            JsonObject regionsJson = json.getAsJsonObject("regions");
            for (String key : regionsJson.keySet()) {
                int regionId = Integer.parseInt(key);
                RegionBorderData regionData = RegionBorderData.fromJson(regionsJson.getAsJsonObject(key));
                data.regionData.put(regionId, regionData);
            }

            return data;
        }
    }
}

