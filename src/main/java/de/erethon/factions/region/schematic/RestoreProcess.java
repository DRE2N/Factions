package de.erethon.factions.region.schematic;

import io.papermc.paper.math.Position;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Fyreum
 */
@SuppressWarnings("UnstableApiUsage")
public class RestoreProcess {

    private final World world;
    private final Position start;
    private final String[][][] blocks;
    private int x, y, z;
    private boolean finished = false;
    private Runnable onFinish;

    public RestoreProcess(@NotNull World world, @NotNull Position start, @NotNull String[][][] blocks) {
        this.world = world;
        this.start = start.toBlock();
        this.blocks = blocks;
    }

    public void proceed() {
        if (x >= blocks.length) {
            x = 0;
            ++y;
        }
        if (y >= blocks[0].length) {
            y = 0;
            ++z;
        }
        if (z >= blocks[0][0].length) {
            if (finished) {
                throw new IllegalStateException("Restore process is already finished.");
            }
            finished = true;
            if (onFinish != null) {
                onFinish.run();
            }
            return;
        }
        String data = blocks[x][y][z];
        BlockData blockData = data == null ? Material.AIR.createBlockData() : Bukkit.createBlockData(data);
        int newX = start.blockX() + x++,
            newY = start.blockY() + y,
            newZ = start.blockZ() + z;
        // If the block is already set to the correct blockData, we can skip it.
        if (world.getBlockData(newX, newY, newZ).matches(blockData)) {
            proceed();
        } else {
            world.setBlockData(newX, newY, newZ, blockData);
        }
    }

    public void proceedTillFinished() {
        while (!finished) {
            proceed();
        }
    }

    /* Getters and setters */

    public boolean isFinished() {
        return finished;
    }

    public @Nullable Runnable getOnFinish() {
        return onFinish;
    }

    public void setOnFinish(@Nullable Runnable onFinish) {
        this.onFinish = onFinish;
    }
}
