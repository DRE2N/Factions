package de.erethon.factions.building;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BuildSiteCache {

    public static final NamespacedKey KEY = new NamespacedKey(Factions.get(), "build_sites");

    private final File cacheFolder;

    private final ConcurrentHashMap<Long, Set<BuildSite>> loaded = new ConcurrentHashMap<>();

    public BuildSiteCache(File cacheFolder) {
        this.cacheFolder = cacheFolder;
    }

    public Set<BuildSite> get(long chunkKey) {
        return loaded.get(chunkKey);
    }

    public void add(BuildSite site, Chunk chunk) {
        if (!loaded.containsKey(chunk.getChunkKey())) {
            loaded.put(chunk.getChunkKey(), new HashSet<>());
        }
        loaded.get(chunk.getChunkKey()).add(site);
    }

    public void remove(BuildSite site, Chunk chunk) {
        if (!loaded.containsKey(chunk.getChunkKey())) {
            return;
        }
        loaded.get(chunk.getChunkKey()).remove(site);
    }

    public void saveAllPendingChunks() {
        for (Long key : loaded.keySet()) {
            saveForChunk(Bukkit.getWorlds().get(0).getChunkAt(key));
        }
    }

    public boolean isLoaded(BuildSite site) {
        return loaded.containsKey(site.getChunkKey());
    }

    public void saveForChunk(Chunk chunk) {
        Set<BuildSite> sites = loaded.get(chunk.getChunkKey());
        if (sites == null) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (BuildSite site : sites) {
            try {
                site.save(new File(cacheFolder, site.getUuid().toString() + ".yml"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            builder.append(site.getUuid().toString()).append(";");
            i++;
        }
        MessageUtil.log("Saved " + i + " build sites for " + chunk);
        chunk.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, builder.toString());
        loaded.remove(chunk.getChunkKey());
    }


    public void loadForChunk(Chunk chunk) {
        if (!chunk.getPersistentDataContainer().has(KEY, PersistentDataType.STRING)) {
            return;
        }
        String data = chunk.getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
        if (data == null) {
            return;
        }
        String[] split = data.split(";");
        Set<BuildSite> sites = new HashSet<>();
        int i = 0;
        for (String s : split) {
            File file = new File(cacheFolder, s + ".yml");
            if (!file.exists()) {
                continue;
            }
            BuildSite site = new BuildSite(file);
            sites.add(site);
            i++;
        }
        MessageUtil.log("Loaded " + i + " build sites for " + chunk);
        loaded.put(chunk.getChunkKey(), sites);
        Factions.get().getRegionManager().getRegionByChunk(chunk).getBuildSites().addAll(sites);
    }


}
