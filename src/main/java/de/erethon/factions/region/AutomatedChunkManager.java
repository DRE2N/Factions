package de.erethon.factions.region;

import de.erethon.aergia.util.DynamicComponent;
import de.erethon.aergia.util.TickUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import org.bukkit.Chunk;
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
        Region existingRegion = plugin.getRegionManager().getRegionByChunk(chunk);
        switch (operation) {
            case ADD -> add(existingRegion, chunk);
            case REMOVE -> remove(existingRegion, chunk);
        }
    }

    private void add(Region existingRegion, Chunk chunk) {
        if (selection == null) {
            return;
        }
        if (existingRegion != null) {
            if (existingRegion != selection) {
                sendActionBar(p -> FMessage.ACM_ADD_INSIDE_REGION.message());
            }
            return;
        }
        if (selection.addChunk(chunk)) {
            sendActionBar(p -> FMessage.ACM_ADDED_CHUNK.message(toString(chunk)));
        }
    }

    private void remove(Region existingRegion, Chunk chunk) {
        if (existingRegion == null) {
            return;
        }
        if (selection != null && existingRegion != selection){
            sendActionBar(p -> FMessage.ACM_ADD_INSIDE_REGION.message());
            return;
        }
        if (existingRegion.removeChunk(chunk)) {
            sendActionBar(p -> FMessage.ACM_REMOVED_CHUNK.message(toString(chunk)));
        }
    }

    private String toString(Chunk chunk) {
        return chunk.getX() + "," + chunk.getZ();
    }

    private void sendActionBar(DynamicComponent component) {
        fPlayer.updateOrSendActionBarCenter(ACTION_BAR_ID, p -> FMessage.ACM_PREFIX.message().append(component.get(p)), ACTION_BAR_TICKS);
    }

    public void deactivate() {
        deactivate(false);
    }

    public void deactivate(boolean ignoreOperation) {
        if (!ignoreOperation && operation == ChunkOperation.IDLE) {
            sendActionBar(p -> FMessage.ACM_NOT_SELECTED.message());
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
}
