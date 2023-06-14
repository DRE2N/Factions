package de.erethon.factions.region;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Fyreum
 */
public class RegionManagerTest {

    @Test
    void splitRegion() {
        System.out.println("0 degrees");
        splitRegion(9, 0, 1);
        System.out.println("45 degrees");
        splitRegion(9, -1, 1);
        System.out.println("67.5 degrees");
        splitRegion(9, -0.5, 1);
    }

    void splitRegion(int size, double directionX, double directionZ) {
        Set<LazyChunk> chunks = new HashSet<>();
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                chunks.add(new LazyChunk(x, z));
            }
        }
        LazyChunk start = new LazyChunk(size / 2, size / 2);
        Set<LazyChunk> rightChunks = new HashSet<>();

        Iterator<LazyChunk> iterator = chunks.iterator();
        while (iterator.hasNext()) {
            LazyChunk chunk = iterator.next();
            double d = directionX * (chunk.getZ() - start.getZ()) - directionZ * (chunk.getX() - start.getX());
            if (d > 0) { // If left side of the line
                continue;
            }
            rightChunks.add(chunk);
            iterator.remove();
        }
        // Display the result
        char[][] display = new char[size][size];
        for (LazyChunk chunk : chunks) {
            display[chunk.getZ()][chunk.getX()] = 'L';
        }
        for (LazyChunk chunk : rightChunks) {
            display[chunk.getZ()][chunk.getX()] = 'R';
        }
        display[start.getZ()][start.getX()] = 'S';
        System.out.println();
        for (char[] row : display) {
            StringBuilder sb = new StringBuilder(row.length * 2);
            for (char c : row) {
                sb.append(c).append(" ");
            }
            System.out.println(sb);
        }
        System.out.println();
    }

}
