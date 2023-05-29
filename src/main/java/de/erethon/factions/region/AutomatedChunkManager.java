package de.erethon.factions.region;

import de.erethon.aergia.util.DynamicComponent;
import de.erethon.aergia.util.TickUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private int radius = 1;

    public AutomatedChunkManager(@NotNull FPlayer fPlayer) {
        this.fPlayer = fPlayer;
    }

    public void handle(@NotNull Chunk chunk) {
        if (operation == ChunkOperation.IDLE) {
            return;
        }
        if (selection != null && !selection.getWorldId().equals(chunk.getWorld().getUID())) {
            deactivate(true);
            return;
        }
        if (radius == 1) {
            Region existingRegion = plugin.getRegionManager().getRegionByChunk(chunk);
            switch (operation) {
                case ADD -> add(existingRegion, chunk);
                case REMOVE -> remove(existingRegion, chunk);
            }
            return;
        }
    }

    private void add(Region existingRegion, Chunk chunk) {
        if (selection == null) {
            return;
        }
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
        if (selection == null) {
            return;
        }
        int added = 0;
        int maxX = chunk.getX() + radius;
        int maxZ = chunk.getZ() + radius;
        for (int x = chunk.getX() - radius; x < maxX; x++) {
            for (int z = chunk.getZ() - radius; z < maxZ; z++) {
                LazyChunk lazyChunk = new LazyChunk(x, z);
                Region existingRegion = plugin.getRegionManager().getCache(chunk.getWorld()).getByChunk(lazyChunk);
                if (existingRegion != null) {
                    continue;
                }
                selection.addChunk(lazyChunk);
                added++;
            }
        }
        sendActionBar(FMessage.ACM_ADDED_CHUNKS.message(String.valueOf(added)));
    }

    private void remove(Region existingRegion, Chunk chunk) {
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

    private boolean remove(Region existingRegion, LazyChunk chunk) {
        if (existingRegion == null || (selection != null && existingRegion != selection)){
            return false;
        }
        return (existingRegion.removeChunk(chunk));
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
            sendActionBar(p -> FMessage.ACM_SELECTED.message(region.getName()));
        } else if (selection != null) {
            sendActionBar(p -> FMessage.ACM_UNSELECTED.message(selection.getName()));
        }
        this.selection = region;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        assert radius >= 1 && radius <= 3 : "Radius must lie between 1 and 3";
        this.radius = radius;
    }
}
