package de.erethon.factions.blocklog;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import de.erethon.factions.Factions;
import de.erethon.factions.blocklog.model.BlockChange;
import de.erethon.factions.blocklog.model.ChangeType;
import de.erethon.factions.blocklog.util.BlockDataSerializer;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hooks into WorldEdit/FAWE edit sessions and stores effective player edits in the block log.
 * Changes are coalesced per position within an edit session to keep write volume manageable.
 */
public class FaweBlockLogBridge {

    private final Factions plugin;
    private final BlockLogManager blockLogManager;
    private volatile boolean registered;

    public FaweBlockLogBridge(Factions plugin, BlockLogManager blockLogManager) {
        this.plugin = plugin;
        this.blockLogManager = blockLogManager;
    }

    public void register() {
        if (registered) {
            return;
        }
        WorldEdit.getInstance().getEventBus().register(this);
        registered = true;
        FLogger.REGION.log("FAWE block-log bridge enabled");
    }

    public void unregister() {
        if (!registered) {
            return;
        }
        WorldEdit.getInstance().getEventBus().unregister(this);
        registered = false;
        FLogger.REGION.log("FAWE block-log bridge disabled");
    }

    @Subscribe
    public void onEditSession(EditSessionEvent event) {
        if (event.getStage() != EditSession.Stage.BEFORE_CHANGE) {
            return;
        }
        if (!blockLogManager.isReady() || blockLogManager.isExternalEditLoggingSuppressed()) {
            return;
        }

        Actor actor = event.getActor();
        if (actor == null) {
            return;
        }

        String worldName = event.getWorld().getName();
        World bukkitWorld = Bukkit.getWorld(worldName);
        if (bukkitWorld == null) {
            return;
        }

        event.setExtent(new LoggingExtent(
                event.getExtent(),
                plugin,
                blockLogManager,
                bukkitWorld,
                actor.getUniqueId(),
                actor.getName()
        ));
    }

    private static final class LoggingExtent extends AbstractDelegateExtent {

        private final Factions plugin;
        private final BlockLogManager blockLogManager;
        private final World world;
        private final UUID actorUuid;
        private final String actorName;
        private final Map<Long, PendingChange> pending = new HashMap<>();
        private boolean flushed;

        private LoggingExtent(Extent extent,
                              Factions plugin,
                              BlockLogManager blockLogManager,
                              World world,
                              UUID actorUuid,
                              String actorName) {
            super(extent);
            this.plugin = plugin;
            this.blockLogManager = blockLogManager;
            this.world = world;
            this.actorUuid = actorUuid;
            this.actorName = actorName;
        }

        @Override
        public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) throws com.sk89q.worldedit.WorldEditException {
            BlockState oldState = getExtent().getBlock(location);
            String oldStateString = oldState.getAsString();
            String newStateString = block.toImmutableState().getAsString();
            boolean changed = super.setBlock(location, block);
            if (changed) {
                remember(location.x(), location.y(), location.z(), oldStateString, newStateString);
            }
            return changed;
        }

        @Override
        public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block) throws com.sk89q.worldedit.WorldEditException {
            return setBlock(BlockVector3.at(x, y, z), block);
        }

        @Override
        public com.sk89q.worldedit.function.operation.Operation commit() {
            flushPending();
            return super.commit();
        }

        private void remember(int x, int y, int z, String oldState, String newState) {
            if (oldState.equals(newState)) {
                return;
            }

            long key = packPosition(x, y, z);
            PendingChange existing = pending.get(key);
            if (existing == null) {
                pending.put(key, new PendingChange(x, y, z, oldState, newState));
                return;
            }

            PendingChange merged = new PendingChange(x, y, z, existing.oldState(), newState);
            if (merged.oldState().equals(merged.newState())) {
                pending.remove(key);
                return;
            }
            pending.put(key, merged);
        }

        private void flushPending() {
            if (flushed || pending.isEmpty()) {
                flushed = true;
                return;
            }
            flushed = true;

            Map<String, Integer> paletteCache = new HashMap<>();
            Map<Long, Region> chunkRegionCache = new HashMap<>();
            int written = 0;

            for (PendingChange change : pending.values()) {
                long chunkKey = packChunk(change.x() >> 4, change.z() >> 4);
                if (!chunkRegionCache.containsKey(chunkKey)) {
                    Location location = new Location(world, change.x(), change.y(), change.z());
                    chunkRegionCache.put(chunkKey, plugin.getRegionManager().getRegionByLocation(location));
                }
                Region region = chunkRegionCache.get(chunkKey);
                if (region == null) {
                    continue;
                }

                int oldId = paletteId(change.oldState(), paletteCache);
                int newId = paletteId(change.newState(), paletteCache);
                if (oldId == newId) {
                    continue;
                }

                ChangeType type = newId == BlockPalette.AIR_ID ? ChangeType.PLAYER_BREAK : ChangeType.PLAYER_PLACE;
                int baselineVersion = blockLogManager.getRegionBaselineDAO()
                        .getCurrentVersion(region.getId())
                        .orElse(0);

                blockLogManager.logBlockChange(new BlockChange(
                        region.getId(),
                        change.x(),
                        change.y(),
                        change.z(),
                        world.getName(),
                        oldId,
                        newId,
                        type,
                        actorUuid,
                        actorName,
                        Timestamp.from(Instant.now()),
                        baselineVersion
                ));
                written++;
            }

            if (written > 0) {
                FLogger.REGION.log("Logged " + written + " coalesced FAWE edits by " + actorName);
            }
            pending.clear();
        }

        private int paletteId(String state, Map<String, Integer> cache) {
            Integer cached = cache.get(state);
            if (cached != null) {
                return cached;
            }
            byte[] data = BlockDataSerializer.serializeBlockDataString(state);
            int id = blockLogManager.getPalette().getOrCreate(data);
            cache.put(state, id);
            return id;
        }

        private static long packPosition(int x, int y, int z) {
            return ((long) x & 0x3FFFFFFL) << 38 | ((long) y & 0xFFFL) << 26 | ((long) z & 0x3FFFFFFL);
        }

        private static long packChunk(int x, int z) {
            return (((long) x) << 32) ^ (z & 0xffffffffL);
        }
    }

    private record PendingChange(int x, int y, int z, String oldState, String newState) {}
}



