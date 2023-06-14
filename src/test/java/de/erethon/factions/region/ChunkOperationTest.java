package de.erethon.factions.region;

import org.junit.jupiter.api.Test;

/**
 * @author Fyreum
 */

public class ChunkOperationTest {

    @Test
    void testShapes() {
        for (ChunkOperation.Shape shape : ChunkOperation.Shape.values()) {
            System.out.println("Shape: " + shape.name());
            System.out.println();
            testShape(shape, 6);
            System.out.println();
        }
    }

    void testShape(ChunkOperation.Shape shape, int radius) {
        LazyChunk center = new LazyChunk(radius, radius);

        int size = 1 + 2 * radius;
        char[][] display = new char[size][size];

        int included = 0;
        int maxX = center.getX() + radius + 1;
        int maxZ = center.getZ() + radius + 1;

        for (int x = center.getX() - radius; x < maxX; x++) {
            for (int z = center.getZ() - radius; z < maxZ; z++) {
                LazyChunk lazyChunk = new LazyChunk(x, z);
                if (!shape.isIncluded(center, radius, lazyChunk)) {
                    display[z][x] = '_';
                    continue;
                }
                display[z][x] = 'x';
                included++;
                System.out.println("Included: " + lazyChunk);
            }
        }
        System.out.println();
        for (char[] row : display) {
            StringBuilder sb = new StringBuilder(row.length * 2);
            for (char c : row) {
                sb.append(c).append(" ");
            }
            System.out.println(sb);
        }
        System.out.println();
        System.out.println("Included chunks: " + included);
    }

}
