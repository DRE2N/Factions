package de.erethon.factions.region.schematic;

import io.papermc.paper.math.BlockPosition;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;

/**
 * @author Fyreum
 */
@SuppressWarnings("UnstableApiUsage")
public interface RestoreProcess {

    void proceed();

    default void proceedTillFinished() {
        while (!hasRemainingPositions()) {
            proceed();
        }
    }

    default boolean hasRemainingPositions() {
        return !getRemainingPositions().isEmpty();
    }

    @Nullable Runnable getOnFinish();

    void setOnFinish(@Nullable Runnable onFinish);

    @NotNull Deque<BlockPosition> getRemainingPositions();

    default void addRemainingPosition(@NotNull Block block) {
        addRemainingPosition(block.getLocation().toBlock());
    }

    void addRemainingPosition(@NotNull BlockPosition position);

    default void onChunkLoad(@NotNull Chunk chunk) {

    }

}
