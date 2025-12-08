package de.erethon.factions.region;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the RegionBorderCalculator algorithms.
 *
 * @author Malfrador
 */
public class RegionBorderCalculatorTest {

    @Test
    void testChunkGridPackUnpack() {
        System.out.println("=== testChunkGridPackUnpack ===");
        RegionBorderCalculator.ChunkGrid grid = new RegionBorderCalculator.ChunkGrid();

        // Test basic set and get
        grid.set(0, 0, 1);
        assertEquals(1, grid.get(0, 0));
        System.out.println("  Basic set/get (0,0) -> 1: PASS");

        // Test negative coordinates
        grid.set(-100, -200, 42);
        assertEquals(42, grid.get(-100, -200));
        System.out.println("  Negative coords (-100,-200) -> 42: PASS");

        // Test large positive coordinates
        grid.set(100000, 200000, 99);
        assertEquals(99, grid.get(100000, 200000));
        System.out.println("  Large coords (100000,200000) -> 99: PASS");

        // Test wilderness (unset coordinate)
        assertEquals(-1, grid.get(999, 999));
        System.out.println("  Wilderness (unset) (999,999) -> -1: PASS");

        System.out.println("  [RESULT] ChunkGrid coordinate packing works correctly");
    }

    @Test
    void testContiguityValidation_Connected() {
        System.out.println("=== testContiguityValidation_Connected ===");
        // Create a simple connected region (L-shape)
        Set<LazyChunk> chunks = new HashSet<>();
        chunks.add(new LazyChunk(0, 0));
        chunks.add(new LazyChunk(1, 0));
        chunks.add(new LazyChunk(2, 0));
        chunks.add(new LazyChunk(0, 1));
        chunks.add(new LazyChunk(0, 2));

        System.out.println("  L-shaped region:");
        System.out.println("    X X X");
        System.out.println("    X . .");
        System.out.println("    X . .");

        boolean result = isContiguous(chunks);
        assertTrue(result);
        System.out.println("  [RESULT] L-shaped region is contiguous: " + result);
    }

    @Test
    void testContiguityValidation_Disconnected() {
        System.out.println("=== testContiguityValidation_Disconnected ===");
        // Create two disconnected chunks
        Set<LazyChunk> chunks = new HashSet<>();
        chunks.add(new LazyChunk(0, 0));
        chunks.add(new LazyChunk(5, 5)); // Far away, not connected

        System.out.println("  Two disconnected chunks at (0,0) and (5,5)");

        boolean result = isContiguous(chunks);
        assertFalse(result);
        System.out.println("  [RESULT] Disconnected chunks detected: " + !result);
    }

    @Test
    void testContiguityValidation_DiagonalNotConnected() {
        System.out.println("=== testContiguityValidation_DiagonalNotConnected ===");
        // Diagonal chunks are NOT connected (only 4-directional connectivity)
        Set<LazyChunk> chunks = new HashSet<>();
        chunks.add(new LazyChunk(0, 0));
        chunks.add(new LazyChunk(1, 1)); // Diagonal - not connected

        System.out.println("  Diagonal chunks at (0,0) and (1,1)");
        System.out.println("    X .");
        System.out.println("    . X");

        boolean result = isContiguous(chunks);
        assertFalse(result);
        System.out.println("  [RESULT] Diagonal chunks are NOT connected (4-directional only): " + !result);
    }

    @Test
    void testEdgeChunkDetection_SingleChunk() {
        System.out.println("=== testEdgeChunkDetection_SingleChunk ===");
        RegionBorderCalculator.ChunkGrid grid = new RegionBorderCalculator.ChunkGrid();
        grid.set(0, 0, 1);

        Set<LazyChunk> chunks = Set.of(new LazyChunk(0, 0));
        Set<LazyChunk> edges = findEdgeChunks(chunks, grid, 1);

        System.out.println("  Single chunk region at (0,0)");
        System.out.println("  Edge chunks found: " + edges.size());

        // Single chunk is always an edge
        assertEquals(1, edges.size());
        assertTrue(edges.contains(new LazyChunk(0, 0)));
        System.out.println("  [RESULT] Single chunk is always an edge: PASS");
    }

    @Test
    void testEdgeChunkDetection_Square() {
        System.out.println("=== testEdgeChunkDetection_Square ===");
        // 3x3 square - only outer chunks are edges
        RegionBorderCalculator.ChunkGrid grid = new RegionBorderCalculator.ChunkGrid();
        Set<LazyChunk> chunks = new HashSet<>();

        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                chunks.add(new LazyChunk(x, z));
                grid.set(x, z, 1);
            }
        }

        System.out.println("  3x3 square region:");
        System.out.println("    1 1 1");
        System.out.println("    1 1 1");
        System.out.println("    1 1 1");

        Set<LazyChunk> edges = findEdgeChunks(chunks, grid, 1);

        System.out.println("  Total chunks: " + chunks.size());
        System.out.println("  Edge chunks: " + edges.size());
        System.out.println("  Center (1,1) is edge: " + edges.contains(new LazyChunk(1, 1)));

        // All 8 outer chunks should be edges, center (1,1) should not
        assertEquals(8, edges.size());
        assertFalse(edges.contains(new LazyChunk(1, 1)));
        System.out.println("  [RESULT] 8 outer chunks are edges, center is not: PASS");
    }

    @Test
    void testNeighborDetection() {
        System.out.println("=== testNeighborDetection ===");
        RegionBorderCalculator.ChunkGrid grid = new RegionBorderCalculator.ChunkGrid();

        // Region 1: chunks at (0,0), (1,0)
        grid.set(0, 0, 1);
        grid.set(1, 0, 1);

        // Region 2: chunks at (2,0), (3,0) - adjacent to region 1
        grid.set(2, 0, 2);
        grid.set(3, 0, 2);

        // Region 3: chunks at (0,2) - not adjacent to region 1
        grid.set(0, 2, 3);

        System.out.println("  Layout:");
        System.out.println("    Row 0: [1][1][2][2]");
        System.out.println("    Row 1: [ ][ ][ ][ ]");
        System.out.println("    Row 2: [3][ ][ ][ ]");

        Set<LazyChunk> region1Chunks = Set.of(new LazyChunk(0, 0), new LazyChunk(1, 0));
        Set<Integer> neighbors = findNeighbors(region1Chunks, grid, 1);

        System.out.println("  Region 1 neighbors: " + neighbors);

        // Region 1 should only have region 2 as neighbor
        assertEquals(1, neighbors.size());
        assertTrue(neighbors.contains(2));
        assertFalse(neighbors.contains(3));
        System.out.println("  [RESULT] Region 1 neighbors = [2] (region 3 not adjacent): PASS");
    }

    @Test
    void testEnclosedRegionDetection() {
        System.out.println("=== testEnclosedRegionDetection ===");
        // Create a donut-shaped region 1 surrounding region 2
        // Region 1 forms a ring around region 2
        //   1 1 1 1 1
        //   1 2 2 2 1
        //   1 2 2 2 1
        //   1 2 2 2 1
        //   1 1 1 1 1

        RegionBorderCalculator.ChunkGrid grid = new RegionBorderCalculator.ChunkGrid();

        // Set up region 1 (outer ring)
        for (int x = 0; x < 5; x++) {
            grid.set(x, 0, 1); // Top row
            grid.set(x, 4, 1); // Bottom row
        }
        for (int z = 1; z < 4; z++) {
            grid.set(0, z, 1); // Left column
            grid.set(4, z, 1); // Right column
        }

        // Set up region 2 (inner 3x3)
        for (int x = 1; x < 4; x++) {
            for (int z = 1; z < 4; z++) {
                grid.set(x, z, 2);
            }
        }

        System.out.println("  Donut-shaped layout:");
        System.out.println("    1 1 1 1 1");
        System.out.println("    1 2 2 2 1");
        System.out.println("    1 2 2 2 1");
        System.out.println("    1 2 2 2 1");
        System.out.println("    1 1 1 1 1");

        boolean region2Enclosed = isEnclosed(grid, 2, 1);
        boolean region1Enclosed = isEnclosed(grid, 1, 2);

        System.out.println("  Region 2 enclosed by Region 1: " + region2Enclosed);
        System.out.println("  Region 1 enclosed by Region 2: " + region1Enclosed);

        // Region 2 should be detected as enclosed by region 1
        assertTrue(region2Enclosed);

        // Region 1 should NOT be enclosed
        assertFalse(region1Enclosed);
        System.out.println("  [RESULT] Enclosed region detection works correctly: PASS");
    }

    @Test
    void testNonEnclosedRegion() {
        System.out.println("=== testNonEnclosedRegion ===");
        // Region 2 touches wilderness, so not enclosed
        //   1 1 1 . .
        //   1 2 2 2 .
        //   1 1 1 . .

        RegionBorderCalculator.ChunkGrid grid = new RegionBorderCalculator.ChunkGrid();

        // Region 1
        grid.set(0, 0, 1);
        grid.set(1, 0, 1);
        grid.set(2, 0, 1);
        grid.set(0, 1, 1);
        grid.set(0, 2, 1);
        grid.set(1, 2, 1);
        grid.set(2, 2, 1);

        // Region 2 - touches wilderness on the right
        grid.set(1, 1, 2);
        grid.set(2, 1, 2);
        grid.set(3, 1, 2);

        System.out.println("  Layout (. = wilderness):");
        System.out.println("    1 1 1 . .");
        System.out.println("    1 2 2 2 .");
        System.out.println("    1 1 1 . .");

        boolean enclosed = isEnclosed(grid, 2, 1);
        System.out.println("  Region 2 touches wilderness at (3,1) -> (4,1)");
        System.out.println("  Region 2 enclosed: " + enclosed);

        // Region 2 should NOT be enclosed (touches wilderness)
        assertFalse(enclosed);
        System.out.println("  [RESULT] Region touching wilderness is NOT enclosed: PASS");
    }

    @Test
    void testPolygonSimplification() {
        System.out.println("=== testPolygonSimplification ===");
        // Test that collinear points are removed
        List<int[]> vertices = List.of(
            new int[]{0, 0},
            new int[]{16, 0},  // Collinear with prev and next
            new int[]{32, 0},
            new int[]{32, 16},
            new int[]{0, 16}
        );

        System.out.println("  Input vertices: " + vertices.size());
        System.out.println("    (0,0) -> (16,0) -> (32,0) -> (32,16) -> (0,16)");
        System.out.println("  Point (16,0) is collinear between (0,0) and (32,0)");

        List<int[]> simplified = simplifyPolygon(vertices);

        System.out.println("  Output vertices: " + simplified.size());
        System.out.print("    ");
        for (int[] v : simplified) {
            System.out.print("(" + v[0] + "," + v[1] + ") ");
        }
        System.out.println();

        // Should remove the collinear point at (16, 0)
        assertEquals(4, simplified.size());
        System.out.println("  [RESULT] Collinear point removed, 5 -> 4 vertices: PASS");
    }

    // ==================== Helper methods that mirror the calculator logic ====================

    private boolean isContiguous(Set<LazyChunk> chunks) {
        if (chunks.size() <= 1) return true;

        Set<LazyChunk> visited = new HashSet<>();
        java.util.Queue<LazyChunk> queue = new java.util.LinkedList<>();

        LazyChunk start = chunks.iterator().next();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            LazyChunk current = queue.poll();
            int x = current.getX();
            int z = current.getZ();

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

    private Set<LazyChunk> findEdgeChunks(Set<LazyChunk> chunks, RegionBorderCalculator.ChunkGrid grid, int regionId) {
        Set<LazyChunk> edges = new HashSet<>();

        for (LazyChunk chunk : chunks) {
            int x = chunk.getX();
            int z = chunk.getZ();

            int[] neighborIds = {
                grid.get(x + 1, z),
                grid.get(x - 1, z),
                grid.get(x, z + 1),
                grid.get(x, z - 1)
            };

            for (int neighborId : neighborIds) {
                if (neighborId != regionId) {
                    edges.add(chunk);
                    break;
                }
            }
        }

        return edges;
    }

    private Set<Integer> findNeighbors(Set<LazyChunk> chunks, RegionBorderCalculator.ChunkGrid grid, int regionId) {
        Set<Integer> neighbors = new HashSet<>();

        for (LazyChunk chunk : chunks) {
            int x = chunk.getX();
            int z = chunk.getZ();

            int[] neighborIds = {
                grid.get(x + 1, z),
                grid.get(x - 1, z),
                grid.get(x, z + 1),
                grid.get(x, z - 1)
            };

            for (int neighborId : neighborIds) {
                if (neighborId != regionId && neighborId != -1) {
                    neighbors.add(neighborId);
                }
            }
        }

        return neighbors;
    }

    /**
     * Check if a region is completely enclosed by another region.
     * A region is enclosed if all its edge neighbors belong to the enclosing region only.
     */
    private boolean isEnclosed(RegionBorderCalculator.ChunkGrid grid, int potentiallyEnclosedId, int enclosingId) {
        // Collect all chunks of the potentially enclosed region
        // We need to scan the grid to find them (in real code this would come from the Region object)
        Set<LazyChunk> enclosedChunks = new HashSet<>();

        // Scan a reasonable area to find chunks (for test purposes)
        for (int x = -10; x < 20; x++) {
            for (int z = -10; z < 20; z++) {
                if (grid.get(x, z) == potentiallyEnclosedId) {
                    enclosedChunks.add(new LazyChunk(x, z));
                }
            }
        }

        // Check all edge chunks of the enclosed region
        for (LazyChunk chunk : enclosedChunks) {
            int x = chunk.getX();
            int z = chunk.getZ();

            int[] neighborIds = {
                grid.get(x + 1, z),
                grid.get(x - 1, z),
                grid.get(x, z + 1),
                grid.get(x, z - 1)
            };

            for (int neighborId : neighborIds) {
                // If any neighbor is wilderness (-1) or a region other than the enclosing one,
                // then this region is NOT enclosed
                if (neighborId != potentiallyEnclosedId && neighborId != enclosingId) {
                    return false;
                }
            }
        }

        return true;
    }

    private List<int[]> simplifyPolygon(List<int[]> vertices) {
        if (vertices.size() < 3) return vertices;

        java.util.List<int[]> simplified = new java.util.ArrayList<>();
        int n = vertices.size();

        for (int i = 0; i < n; i++) {
            int[] prev = vertices.get((i - 1 + n) % n);
            int[] curr = vertices.get(i);
            int[] next = vertices.get((i + 1) % n);

            // Check if collinear using cross product
            int cross = (curr[0] - prev[0]) * (next[1] - curr[1]) - (next[0] - curr[0]) * (curr[1] - prev[1]);
            if (cross != 0) {
                simplified.add(curr);
            }
        }

        return simplified.isEmpty() ? vertices : simplified;
    }
}
