package de.erethon.factions.region;

import de.erethon.aergia.util.DynamicComponent;
import de.erethon.aergia.util.TickUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * @author Fyreum
 */
public class AutomatedChunkManager {

    public static final String ACTION_BAR_ID = "automatedChunkManager";
    public static final long ACTION_BAR_TICKS = TickUtil.SECOND * 3;

    final Factions plugin = Factions.get();

    private final FPlayer fPlayer;
    private ChunkOperation operation = ChunkOperation.IDLE;
    private ChunkOperation.Shape shape = ChunkOperation.Shape.SQUARE;
    private Region selection;
    private int radius = 0;
    private LazyChunk lastChunk;

    public AutomatedChunkManager(@NotNull FPlayer fPlayer) {
        this.fPlayer = fPlayer;
    }

    public void handle(@NotNull Chunk chunk) {
        if (operation == ChunkOperation.IDLE) {
            return;
        }
        UUID worldId = chunk.getWorld().getUID();
        if (selection != null && !selection.getWorldId().equals(worldId) || plugin.getRegionManager().getCache(worldId) == null) {
            deactivate(true);
            return;
        }
        // Stop early when inside the same chunk, to improve performance.
        if (lastChunk != null && lastChunk.equalsChunk(chunk)) {
            return;
        }
        lastChunk = new LazyChunk(chunk.getX(), chunk.getZ());
        switch (operation) {
            case ADD -> add(chunk);
            case REMOVE -> remove(chunk);
        }
    }

    private void add(Chunk chunk) {
        if (selection == null) {
            return;
        }
        if (radius > 0) {
            addMultiple(chunk);
            return;
        }
        Region existingRegion = plugin.getRegionManager().getRegionByChunk(chunk);
        if (existingRegion != null) {
            if (existingRegion != selection) {
                sendActionBar(FMessage.ACM_ADD_INSIDE_REGION.message());
            }
            return;
        }
        if (selection.addChunk(chunk)) {
            sendActionBar(FMessage.ACM_ADDED_CHUNK.message(toString(chunk)));
        }
    }

    private void addMultiple(Chunk chunk) {
        BiPredicate<LazyChunk, Region> action = (c, rg) -> rg == null && selection.addChunk(c);
        squaredAction(chunk, action, FMessage.ACM_ADDED_CHUNKS);
    }

    private void remove(Chunk chunk) {
        if (radius > 0) {
            removeMultiple(chunk);
            return;
        }
        Region existingRegion = plugin.getRegionManager().getRegionByChunk(chunk);
        if (existingRegion == null) {
            return;
        }
        if (selection != null && existingRegion != selection){
            sendActionBar(FMessage.ACM_ADD_INSIDE_REGION.message());
            return;
        }
        if (existingRegion.removeChunk(chunk)) {
            sendActionBar(FMessage.ACM_REMOVED_CHUNK.message(toString(chunk)));
        }
    }

    private void removeMultiple(Chunk chunk) {
        BiPredicate<LazyChunk, Region> action = (c, rg) -> rg != null && (selection == null || rg == selection) && rg.removeChunk(c);
        squaredAction(chunk, action, FMessage.ACM_REMOVED_CHUNKS);
    }

    private void squaredAction(Chunk chunk, BiPredicate<LazyChunk, Region> action, FMessage message) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            RegionCache regionCache = plugin.getRegionManager().getCache(chunk.getWorld());
            LazyChunk center = new LazyChunk(chunk);

            int modified = 0;
            int maxX = center.getX() + radius + 1;
            int maxZ = center.getZ() + radius + 1;

            for (int x = center.getX() - radius; x < maxX; x++) {
                for (int z = center.getZ() - radius; z < maxZ; z++) {
                    LazyChunk lazyChunk = new LazyChunk(x, z);
                    Region existingRegion = regionCache.getByChunk(lazyChunk);

                    if (shape.isIncluded(center, radius, lazyChunk) && action.test(lazyChunk, existingRegion)) {
                        modified++;
                    }
                }
            }
            if (modified > 0) {
                sendActionBar(message.message(String.valueOf(modified)));
            }
        });
    }

    private String toString(Chunk chunk) {
        return chunk.getX() + "," + chunk.getZ();
    }

    private void sendActionBar(Component component) {
        sendActionBar(p -> component);
    }

    private void sendActionBar(DynamicComponent component) {
        fPlayer.updateOrSendActionBarCenter(ACTION_BAR_ID, p -> FMessage.ACM_PREFIX.message(component.get(p)), ACTION_BAR_TICKS);
    }

    public void deactivate() {
        deactivate(false);
    }

    public void deactivate(boolean ignoreOperation) {
        if (!ignoreOperation && operation == ChunkOperation.IDLE) {
            sendActionBar(FMessage.ACM_NOT_SELECTED.message());
        } else {
            setOperation(ChunkOperation.IDLE);
            setSelection(null);
        }
    }

    private void resetLastChunkIf(boolean b) {
        if (b) {
            lastChunk = null;
        }
    }

    /* Getters and setters */

    public @NotNull ChunkOperation getOperation() {
        return operation;
    }

    public void setOperation(@NotNull ChunkOperation operation) {
        resetLastChunkIf(this.operation != operation);
        this.operation = operation;
    }

    public @NotNull ChunkOperation.Shape getShape() {
        return shape;
    }

    public void setShape(@NotNull ChunkOperation.Shape shape) {
        sendActionBar(FMessage.ACM_SHAPE.message(shape.name()));
        resetLastChunkIf(this.shape != shape);
        this.shape = shape;
    }

    public boolean isActivated() {
        return operation != ChunkOperation.IDLE;
    }

    public @Nullable Region getSelection() {
        return selection;
    }

    public void setSelection(@Nullable Region region) {
        if (region != null) {
            sendActionBar(FMessage.ACM_SELECTED.message(region.getName()));
        } else if (selection != null) {
            sendActionBar(FMessage.ACM_UNSELECTED.message(selection.getName()));
        }
        resetLastChunkIf(this.selection != region);
        this.selection = region;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        int maxRadius = plugin.getFConfig().getMaximumAutomatedChunkManagerRadius();
        if (radius < 0 || radius > maxRadius) {
            sendActionBar(FMessage.ACM_ILLEGAL_RADIUS.message("0", String.valueOf(maxRadius)));
            return;
        } else {
            sendActionBar(FMessage.ACM_RADIUS_SELECTED.message(String.valueOf(radius)));
        }
        resetLastChunkIf(this.radius != radius);
        this.radius = radius;
    }
}
