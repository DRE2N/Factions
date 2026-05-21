package de.erethon.factions.building;

import de.erethon.factions.Factions;
import de.erethon.factions.util.FLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author Malfrador
 */
public class BuildSiteCache implements Listener {

    private final File cacheFolder;

    private final HashMap<String, BuildSite> sites = new HashMap<>();
    private final HashMap<Long, Set<BuildSite>> chunkCache = new HashMap<>();

    public BuildSiteCache(File cacheFolder) {
        this.cacheFolder = cacheFolder;
        if (!cacheFolder.exists()) {
            cacheFolder.mkdirs();
        }
        Bukkit.getPluginManager().registerEvents(this, Factions.get());
    }

    public BuildSite loadFromUUID(String uuid) {
        File file = new File(cacheFolder, uuid + ".yml");
        if (!file.exists()) {
            return null;
        }
        if (sites.containsKey(uuid)) {
            return sites.get(uuid);
        }
        BuildSite loaded;
        try {
            loaded = new BuildSite(file);
            loaded.load();
        } catch (Exception e) {
            FLogger.ERROR.log("Failed to load build site from file: " + file.getName());
            e.printStackTrace();
            return null;
        }
        sites.put(uuid, loaded);
        addToChunkCache(loaded);
        return loaded;
    }

    public void addBuildSite(BuildSite site) {
        sites.put(site.getUUIDString(), site);
        long chunkKey = site.getInteractive().getChunk().getChunkKey();
        Set<BuildSite> chunkSites = chunkCache.computeIfAbsent(chunkKey, k -> new HashSet<>());
        chunkSites.add(site);
    }

    public void addToChunkCache(BuildSite site) {
        long chunkKey = site.getInteractive().getChunk().getChunkKey();
        Set<BuildSite> chunkSites = chunkCache.computeIfAbsent(chunkKey, k -> new HashSet<>());
        chunkSites.add(site);
    }

    public Set<BuildSite> get(long chunkKey) {
        return chunkCache.getOrDefault(chunkKey, new HashSet<>());
    }

    public boolean isInCache(UUID uuid) {
        return sites.containsKey(uuid.toString());
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        Set<BuildSite> sites = chunkCache.get(event.getClickedBlock().getChunk().getChunkKey());
        if (sites == null) {
            return;
        }
        for (BuildSite site : sites) {
            site.handleContainerInteract(event);
        }
    }

    @EventHandler
    private void onHologramInteract(PlayerInteractEntityEvent event) {
        BuildSite site = getByHologramInteraction(event.getRightClicked().getUniqueId());
        if (site == null) {
            return;
        }
        event.setCancelled(true);
        site.handleHologramRightClick(event.getPlayer());
    }

    @EventHandler
    private void onHologramDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        BuildSite site = getByHologramInteraction(event.getEntity().getUniqueId());
        if (site == null) {
            return;
        }
        event.setCancelled(true);
        site.handleHologramLeftClick(player);
    }

    private BuildSite getByHologramInteraction(UUID entityId) {
        for (BuildSite site : sites.values()) {
            if (site.isHologramInteractionId(entityId)) {
                return site;
            }
        }
        return null;
    }

    @EventHandler
    private void onChunkLoad(ChunkLoadEvent event) {
        long chunkKey = event.getChunk().getChunkKey();
        Set<BuildSite> sites = chunkCache.get(chunkKey);
        if (sites != null) {
            for (BuildSite site : sites) {
                site.onChunkLoad();
            }
        }
    }

    @EventHandler
    private void onEntitiesLoad(EntitiesLoadEvent event) {
        long chunkKey = event.getChunk().getChunkKey();
        Set<BuildSite> sites = chunkCache.get(chunkKey);
        if (sites != null) {
            for (BuildSite site : sites) {
                site.updateHolo();
            }
        }
    }

    @EventHandler
    private void onChunkUnload(ChunkUnloadEvent event) {
        long chunkKey = event.getChunk().getChunkKey();
        Set<BuildSite> sites = chunkCache.get(chunkKey);
        if (sites != null) {
            for (BuildSite site : sites) {
                site.onChunkUnload();
            }
        }
    }
}
