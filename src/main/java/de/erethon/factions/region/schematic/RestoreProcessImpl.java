package de.erethon.factions.region.schematic;

import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author Fyreum
 */
@SuppressWarnings("UnstableApiUsage")
public class RestoreProcessImpl implements RestoreProcess {

    private final World world;
    private final Position start;
    private final RegionSchematic schematic;
    private Runnable onFinish;
    private final Deque<BlockPosition> remainingPositions = new ArrayDeque<>();
    private int t;

    public RestoreProcessImpl(@NotNull World world, @NotNull Position start, @NotNull RegionSchematic schematic) {
        this.world = world;
        this.start = start.toBlock();
        this.schematic = schematic;
        schematic.foreach((x, y, z, data) -> remainingPositions.add(Position.block(start.blockX() + x, start.blockY() + y, start.blockZ() + z)));
    }

    @Override
    public void proceed() {
        BlockPosition pos = remainingPositions.poll();
        if (pos == null) {
            return;
        }
        String data = schematic.getBlockAt(pos.blockX(), pos.blockY(), pos.blockZ());
        BlockData blockData = data == null ? Material.AIR.createBlockData() : Bukkit.createBlockData(data);

        int newX = start.blockX() + pos.blockX(),
            newY = start.blockY() + pos.blockY(),
            newZ = start.blockZ() + pos.blockZ();
        // If the block is already set to the correct blockData, we can skip it.
        if (world.getBlockData(newX, newY, newZ).matches(blockData)) {
            proceed();
        } else {
            world.setBlockData(newX, newY, newZ, blockData);
        }
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
