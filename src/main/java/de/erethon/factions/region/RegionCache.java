package de.erethon.factions.region;

import de.erethon.factions.data.FMessage;
import de.erethon.factions.entity.FEntityCache;
import de.erethon.factions.util.FLogger;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Fyreum
 */
public class RegionCache extends FEntityCache<Region> {

    private final RegionManager regionManager;
    private final UUID worldId;
    private final Map<LazyChunk, Region> chunkToRegion = new HashMap<>();

    public RegionCache(@NotNull RegionManager regionManager, @NotNull UUID worldId, @NotNull File folder) {
        super(folder);
        this.regionManager = regionManager;
        this.worldId = worldId;
    }

    @Override
    protected @Nullable Region create(@NotNull File file) {
        try {
            // Read the type first to determine which subclass to create
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String typeName = config.getString("type", RegionType.BARREN.name());
            RegionType type = RegionType.getByName(typeName, RegionType.BARREN);
            return createRegionForType(type, file);
        } catch (NumberFormatException e) {
            FLogger.ERROR.log("Couldn't load region file '" + file.getName() + "': Invalid ID");
            regionManager.addUnloadedRegionId(file);
            return null;
        }
    }

    /**
     * Creates a region instance of the appropriate subclass based on the region type.
     */
    private @NotNull Region createRegionForType(@NotNull RegionType type, @NotNull File file) {
        return switch (type) {
            case CAPITAL -> new CapitalRegion(this, file);
            case WAR_ZONE -> new WarRegion(this, file);
            default -> new ClaimableRegion(this, file);
        };
    }

    protected @NotNull Region createRegion(int id, @NotNull LazyChunk chunk) {
        return createRegion(id, chunk, FMessage.GENERAL_REGION_DEFAULT_NAME_PREFIX.getMessage() + id);
    }

    protected @NotNull Region createRegion(int id, @NotNull LazyChunk chunk, @NotNull String name) {
        return createRegion(id, chunk, name, RegionType.BARREN);
    }

    protected @NotNull Region createRegion(int id, @NotNull LazyChunk chunk, @NotNull String name, @NotNull RegionType type) {
        FLogger.REGION.log("Creating region " + id + " (" + name + ") at " + chunk.getX() + "," + chunk.getZ() + "...");
        File file = new File(folder, id + ".yml");
        Region region = switch (type) {
            case CAPITAL -> new CapitalRegion(this, file, id, name, null);
            case WAR_ZONE -> new WarRegion(this, file, id, name, null);
            default -> new ClaimableRegion(this, file, id, name, null);
        };
        region.setType(type);
        region.addChunk(chunk);
        cache.put(id, region);
        return region;
    }

    protected void cacheChunkForRegion(@NotNull LazyChunk lazyChunk, @NotNull Region region) {
        FLogger.REGION.log("Caching chunk '" + lazyChunk + "' for region " + region.getId());
        chunkToRegion.put(lazyChunk, region);
    }

    protected void removeChunkForRegion(@NotNull LazyChunk lazyChunk) {
        FLogger.REGION.log("Removing chunk '" + lazyChunk + "' from the cache");
        chunkToRegion.remove(lazyChunk);
    }

    protected void removeRegion(@NotNull Region region) {
        cache.remove(region.getId());
        for (LazyChunk chunk : region.getChunks()) {
            chunkToRegion.remove(chunk);
        }
    }

    /* Getters */

    public @NotNull RegionManager getRegionManager() {
        return regionManager;
    }

    public @NotNull UUID getWorldId() {
        return worldId;
    }

    public @Nullable Region getByChunk(@NotNull Chunk chunk) {
        return getByChunk(new LazyChunk(chunk));
    }

    public @Nullable Region getByChunk(@NotNull LazyChunk lazyChunk) {
        return chunkToRegion.get(lazyChunk);
    }

    public @Nullable Region getByLocation(@NotNull Location location) {
        return getByChunk(new LazyChunk(location.getBlockX() >> 4, location.getBlockZ() >> 4));
    }
}
