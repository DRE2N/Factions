package de.erethon.factions.region;

import de.erethon.bedrock.misc.FileUtil;
import de.erethon.bedrock.misc.JavaUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.util.FException;
import de.erethon.factions.util.FLogger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Fyreum
 */
public class RegionManager {

    private final File folder;
    private final Map<UUID, RegionCache> caches = new HashMap<>();
    private final Set<Integer> unloadedRegionIds = new HashSet<>();
    private int presumableUnusedId = 0;

    public RegionManager(@NotNull File folder) {
        this.folder = folder;
        for (File worldFolder : FileUtil.getSubFolders(folder)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(worldFolder.getName());
            } catch (IllegalArgumentException e) {
                FLogger.ERROR.log("Couldn't load world folder '" + worldFolder.getName() + "': Invalid UUID");
                addUnloadedRegionIds(worldFolder);
                continue;
            }
            if (Bukkit.getWorld(uuid) == null) {
                addUnloadedRegionIds(worldFolder);
                FLogger.ERROR.log("Couldn't load world folder '" + worldFolder.getName() + "': World not found");
                continue;
            }
            caches.put(uuid, new RegionCache(this, uuid, worldFolder));
        }
    }

    protected void addUnloadedRegionIds(@NotNull File worldFolder) {
        for (File regionFile : FileUtil.getSubFolders(worldFolder)) {
            addUnloadedRegionId(regionFile);
        }
    }

    protected void addUnloadedRegionId(@NotNull File regionFile) {
        JavaUtil.runSilent(() -> {
            int id = Integer.parseInt(regionFile.getName().replace(".yml", ""));
            FLogger.REGION.log("Adding unloaded region ID '" + id + "', to prevent ID duplication...");
            unloadedRegionIds.add(id);
        });
    }

    public @NotNull RegionCache createRegionCache(@NotNull World world) {
        UUID uuid = world.getUID();
        RegionCache cached = caches.get(uuid);
        if (cached != null) {
            return cached;
        }
        caches.put(uuid, cached = new RegionCache(this, uuid, new File(folder, uuid.toString())));
        return cached;
    }

    public void loadWorld(@NotNull World world) {
        UUID uuid = world.getUID();
        if (caches.containsKey(uuid)) {
            return;
        }
        File worldFolder = new File(folder, uuid.toString());
        if (!worldFolder.exists()) {
            return;
        }
        caches.put(uuid, new RegionCache(this, uuid, worldFolder));
    }

    public void unloadWorld(@NotNull World world) {
        RegionCache cache = caches.remove(world.getUID());
        if (cache == null) {
            return;
        }
        cache.saveAll();
    }

    public void loadAll() {
        int loaded = 0;
        for (RegionCache cache : caches.values()) {
            cache.loadAll();
            loaded += cache.getCache().size();
        }
        FLogger.INFO.log("Loaded " + loaded + " regions");
    }

    public void saveAll() {
        for (RegionCache cache : caches.values()) {
            cache.saveAll();
        }
    }

    /**
     * @param chunk the chunk to create a region for
     * @return the created region or null
     */
    public @NotNull Region createRegion(@NotNull Chunk chunk) throws FException {
        return createRegion(chunk, null);
    }

    /**
     * @param chunk the chunk to create a region for
     * @param name the name of the region
     * @return the created region or null
     */
    public @NotNull Region createRegion(@NotNull Chunk chunk, @Nullable String name) throws FException {
        World world = chunk.getWorld();
        RegionCache regionCache = getCache(world);
        if (regionCache != null) {
            FException.throwIf(regionCache.getByChunk(chunk) != null, "Couldn't create region: Chunk already part of a region", FMessage.ERROR_CHUNK_ALREADY_A_REGION);
        } else {
            regionCache = createRegionCache(world);
        }
        LazyChunk lazyChunk = new LazyChunk(chunk);
        int id = generateId(true);
        if (name == null) {
            return regionCache.createRegion(id, lazyChunk);
        }
        FException.throwIf(getRegionByName(name) != null, "Couldn't create region: Name already in use", FMessage.ERROR_NAME_IN_USE, name);
        return regionCache.createRegion(id, lazyChunk, name);
    }

    public @NotNull CompletableFuture<Region> splitRegion(@NotNull Region region, @NotNull LazyChunk start, @NotNull Vector direction) {
        FException.throwIf(!region.getChunks().contains(start), "The starting point is not inside the region", FMessage.ERROR_CHUNK_OUTSIDE_THE_REGION);
        CompletableFuture<Region> completableFuture = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(Factions.get(), () -> {
            Set<LazyChunk> rightChunks = new HashSet<>();
            Iterator<LazyChunk> iterator = region.getChunks().iterator();

            while (iterator.hasNext()) {
                LazyChunk chunk = iterator.next();
                double d = direction.getX() * (chunk.getZ() - start.getZ()) - direction.getZ() * (chunk.getX() - start.getX());
                if (d > 0) {
                    continue;
                }
                rightChunks.add(chunk);
                iterator.remove();
            }
            int id = generateId(true);
            Region newRegion = region.getRegionCache().createRegion(id, start);
            newRegion.addChunks(rightChunks);
            completableFuture.complete(newRegion);
        });
        return completableFuture;
    }

    /**
     * Returns an unused region ID. Uses a variable to presume an unused ID
     * on each call. The variable gets incremented each time an ID is generated
     * (if 'updatePrediction' is set true).
     *
     * @param updatePrediction whether the presumable unused variable should be changed
     * @return an unused region ID
     */
    public synchronized int generateId(boolean updatePrediction) {
        int id = presumableUnusedId;
        while (getRegionById(id) != null || unloadedRegionIds.contains(id)) {
            id++;
        }
        if (updatePrediction) {
            presumableUnusedId = id + 1;
        }
        return id;
    }
    
    /* Getters and setters */

    public @NotNull File getFolder() {
        return folder;
    }

    public @NotNull Map<UUID, RegionCache> getCaches() {
        return caches;
    }

    public @Nullable RegionCache getCache(World world) {
        return getCache(world.getUID());
    }

    public @Nullable RegionCache getCache(UUID worldId) {
        return caches.get(worldId);
    }

    public int getCachedRegionsAmount() {
        int amount = 0;
        for (RegionCache cache : caches.values()) {
            amount += cache.getCache().size();
        }
        return amount;
    }

    public @Nullable Region getRegionByPlayer(@NotNull Player player) {
        return getRegionByLocation(player.getLocation());
    }

    public @Nullable Region getRegionByLocation(@NotNull Location location) {
        RegionCache cache = getCache(location.getWorld());
        return cache != null ? cache.getByLocation(location) : null;
    }

    public @Nullable Region getRegionByChunk(@NotNull Chunk chunk) {
        RegionCache cache = getCache(chunk.getWorld());
        return cache != null ? cache.getByChunk(chunk) : null;
    }

    public @Nullable Region getRegionById(int id) {
        for (RegionCache cache : caches.values()) {
            Region region = cache.getById(id);
            if (region != null) {
                return region;
            }
        }
        return null;
    }

    public @Nullable Region getRegionByName(@NotNull String name) {
        for (RegionCache cache : caches.values()) {
            Region region = cache.getByName(name);
            if (region != null) {
                return region;
            }
        }
        return null;
    }

    public @NotNull Set<Integer> getUnloadedRegionIds() {
        return unloadedRegionIds;
    }
}
