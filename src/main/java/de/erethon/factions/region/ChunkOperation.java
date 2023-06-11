package de.erethon.factions.region;

import de.erethon.factions.util.TriPredicate;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public enum ChunkOperation {

    /**
     * Adds the current chunk to the region.
     */
    ADD,
    /**
     * Nothing happens => idle.
     */
    IDLE,
    /**
     * Removes the current chunk from the region.
     */
    REMOVE;

    /**
     * The shape defines, whether a chunk will be progressed, or not.
     * <p>
     * Shapes will only handle chunks inside a square area. The square
     * covers following area: (1 + 2r)².
     * <p>
     * This means the {@link #SQUARE} shape won't need to take a look at the chunk,
     * as it's already inside the square.
     */
    public enum Shape {

        /**
         * Chunks inside the circular radius will be progressed.
         */
        CIRCLE((c, r, o) -> Math.pow(o.getX() - c.getX(), 2) + Math.pow(o.getZ() - c.getZ(), 2) <= Math.pow(r, 2)),

        /**
         * Chunks inside the square will be progressed.
         */
        SQUARE((c, r, o) -> true);

        private final TriPredicate<LazyChunk, Integer, LazyChunk> filter;

        Shape(@NotNull TriPredicate<LazyChunk, Integer, LazyChunk> filter) {
            this.filter = filter;
        }

        public boolean isIncluded(@NotNull LazyChunk center, int radius, @NotNull LazyChunk toCheck) {
            return filter.test(center, radius, toCheck);
        }
    }
}
