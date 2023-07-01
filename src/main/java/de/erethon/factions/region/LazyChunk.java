package de.erethon.factions.region;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Arrays;

/**
 * @author Fyreum
 */
public class LazyChunk {

    private final int x;
    private final int z;
    private WeakReference<Chunk> bukkit;

    public LazyChunk(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public LazyChunk(@NotNull Chunk chunk) {
        this.x = chunk.getX();
        this.z = chunk.getZ();
        this.bukkit = new WeakReference<>(chunk);
    }

    public LazyChunk(@NotNull String string) throws NumberFormatException, ArrayIndexOutOfBoundsException {
        String[] args = string.split(",");
        this.x = Integer.parseInt(args[0]);
        this.z = Integer.parseInt(args[1]);
    }

    public @NotNull Chunk asBukkitChunk(@NotNull World world) {
        Chunk chunk;
        if (bukkit == null || (chunk = bukkit.get()) == null) {
            chunk = world.getChunkAt(x, z);
            bukkit = new WeakReference<>(chunk);
        }
        return chunk;
    }

    /* Getters and setters */

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public boolean equalsChunk(@NotNull Chunk chunk) {
        return this.x == chunk.getX() && this.z == chunk.getZ();
    }

    @Override
    public boolean equals(Object chunk) {
        if(!(chunk instanceof LazyChunk other)) return false;
        return this.getX() == other.getX() && this.getZ() == other.getZ();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new int[]{x, z});
    }

    @Override
    public String toString() {
        return x + "," + z;
    }

}
