package de.erethon.factions.region.schematic;

import de.erethon.factions.Factions;
import de.erethon.factions.region.LazyChunk;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Fyreum
 */
@SuppressWarnings("UnstableApiUsage")
public class RestoreProcessImpl implements RestoreProcess {

    private static final Function<LazyChunk, Deque<BlockPosition>> COMPUTE_DEQUE = _ -> new ArrayDeque<>();

    private final World world;
    private final Position start;
    private final RegionSchematic schematic;
    private Runnable onFinish;
    private final Deque<BlockPosition> remainingPositions = new ArrayDeque<>();
    private final Map<LazyChunk, Deque<BlockPosition>> remainingUnloadedPositions = new HashMap<>();

    public RestoreProcessImpl(@NotNull World world, @NotNull Position start, @NotNull RegionSchematic schematic) {
        this.world = world;
        this.start = start.toBlock();
        this.schematic = schematic;
        schematic.foreach((x, y, z, _) -> remainingPositions.add(Position.block(start.blockX() + x, start.blockY() + y, start.blockZ() + z)));
    }

    @Override
    public void proceed() {
        BlockPosition pos = remainingPositions.poll();
        if (pos == null) {
            return;
        }
        int newX = start.blockX() + pos.blockX(),
            newY = start.blockY() + pos.blockY(),
            newZ = start.blockZ() + pos.blockZ();

        if (!world.isChunkLoaded(newX >> 4, newZ >> 4)) {
            LazyChunk chunk = new LazyChunk(newX >> 4, newZ >> 4);
            remainingUnloadedPositions.computeIfAbsent(chunk, COMPUTE_DEQUE).add(Position.block(newX, newY, newZ));
            return;
        }
        // If the block is already set to the correct blockData, we can skip it.
        if (world.getBlockData(newX, newY, newZ).matches(schematic.getBlockDataAt(newX, newY, newZ))) {
            proceed();
        } else {
            world.setBlockData(newX, newY, newZ, schematic.getBlockDataAt(newX, newY, newZ));
        }
    }

    @Override
    public void onChunkLoad(@NotNull Chunk chunk) {
        Deque<BlockPosition> positions = remainingUnloadedPositions.remove(new LazyChunk(chunk));
        if (positions == null) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(Factions.get(), () -> {
            for (BlockPosition pos : positions) {
                world.setBlockData(pos.blockX(), pos.blockY(), pos.blockZ(), schematic.getBlockDataAt(pos.blockX(), pos.blockY(), pos.blockZ()));
            }
        });
    }

    /* Getters and setters */

    @Override
    public @Nullable Runnable getOnFinish() {
        return onFinish;
    }

    @Override
    public void setOnFinish(@Nullable Runnable onFinish) {
        this.onFinish = onFinish;
    }

    @Override
    public @NotNull Deque<BlockPosition> getRemainingPositions() {
        return remainingPositions;
    }

    @Override
    public void addRemainingPosition(@NotNull BlockPosition position) {
        remainingPositions.add(position);
    }
}
