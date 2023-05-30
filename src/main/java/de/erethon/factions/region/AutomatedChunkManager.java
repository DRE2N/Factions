package de.erethon.factions.region;

import de.erethon.aergia.util.DynamicComponent;
import de.erethon.aergia.util.TickUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * @author Fyreum
 */
public class AutomatedChunkManager {

    public static final String ACTION_BAR_ID = "automatedChunkManager";
    public static final long ACTION_BAR_TICKS = TickUtil.SECOND * 3;

    final Factions plugin = Factions.get();

    private final FPlayer fPlayer;
    private ChunkOperation operation = ChunkOperation.IDLE;
    private Region selection;
    private int radius = 0;

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
        int added = squaredAction(chunk, (c, rg) -> rg == null && selection.addChunk(c));
        if (added > 0) {
            sendActionBar(FMessage.ACM_ADDED_CHUNKS.message(String.valueOf(added)));
        }
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
        int removed = squaredAction(chunk, (c, rg) -> rg != null && (selection == null || rg == selection) && rg.removeChunk(c));
        if (removed > 0) {
            sendActionBar(FMessage.ACM_REMOVED_CHUNKS.message(String.valueOf(removed)));
        }
    }

    private int squaredAction(Chunk chunk, BiPredicate<LazyChunk, Region> action) {
        int modified = 0;
        int maxX = chunk.getX() + radius;
        int maxZ = chunk.getZ() + radius;

        for (int x = chunk.getX() - radius; x < maxX; x++) {
            for (int z = chunk.getZ() - radius; z < maxZ; z++) {
                LazyChunk lazyChunk = new LazyChunk(x, z);
                Region existingRegion = plugin.getRegionManager().getCache(chunk.getWorld()).getByChunk(lazyChunk);
                if (action.test(lazyChunk, existingRegion)) {
                    modified++;
                }
            }
        }
        return modified;
    }

    private String toString(Chunk chunk) {
        return chunk.getX() + "," + chunk.getZ();
    }

    private void sendActionBar(Component component) {
        sendActionBar(p -> component);
    }

    private void sendActionBar(DynamicComponent component) {
        fPlayer.updateOrSendActionBarCenter(ACTION_BAR_ID, p -> FMessage.ACM_PREFIX.message().append(component.get(p)), ACTION_BAR_TICKS);
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

    /* Getters and setters */

    public @NotNull ChunkOperation getOperation() {
        return operation;
    }

    public void setOperation(@NotNull ChunkOperation operation) {
        this.operation = operation;
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
        this.selection = region;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        if (radius < 0 || radius > 2) {
            sendActionBar(FMessage.ACM_ILLEGAL_RADIUS.message("0", "2"));
            return;
        } else {
            sendActionBar(FMessage.ACM_RADIUS_SELECTED.message(String.valueOf(radius)));
        }
        this.radius = radius;
    }
}
