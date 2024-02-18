package de.erethon.factions.building;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.misc.FileUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FLogger;
import de.erethon.hephaestus.HItem;
import de.erethon.hephaestus.ItemLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Malfrador
 */
public class BuildingManager implements Listener {

    private final static ResourceLocation ITEM_ID = new ResourceLocation("factions", "building_item");

    Factions plugin = Factions.get();

    private final List<Building> buildings = new CopyOnWriteArrayList<>();
    private final List<BuildSite> buildingTickets = new ArrayList<>();

    private Queue<BuildingEffect> tickingEffects = new PriorityQueue<>();
    private int effectsPerTick = 5;

    public BuildingManager(@NotNull File dir) {
        load(dir);
        effectsPerTick = plugin.getFConfig().getEffectsPerTick();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tickBuildingEffects, 0, plugin.getFConfig().getTicksPerBuildingTick());
        // Register the building item if it doesn't exist
        ItemLibrary lib = Main.itemLibrary;
        if (!lib.has(ITEM_ID)) {
            HItem item = new HItem.Builder(plugin, ITEM_ID).baseItem(Material.CHEST).register();
            item.setPlugin(plugin);
            item.setBehaviour(new FBuildingItemBehaviour(item));
            lib.enableHandler(plugin);
        }
    }

    public @Nullable Building getById(@NotNull String id) {
        for (Building building : buildings) {
            if (building.getId().equals(id)) {
                return building;
            }
        }
        return null;
    }

    public @NotNull List<Building> getBuildings() {
        return buildings;
    }

    public void load(@NotNull File dir) {
        for (File file : FileUtil.getFilesForFolder(dir)) {
            buildings.add(new Building(file));
        }
        if (buildings.isEmpty()) {
            FLogger.INFO.log("No buildings found. Please create some.");
            return;
        }
        File effectsFolder = new File(dir, "effects");
        if (!effectsFolder.exists()) {
            effectsFolder.mkdirs();
        }
        FLogger.INFO.log("Loaded " + buildings.size() + " Buildings.");
    }

    public void deleteBuilding(@NotNull Player player) {
        Region rg = plugin.getFPlayerCache().getByPlayer(player).getLastRegion();
        if (rg == null) {
            MessageUtil.sendMessage(player, "&cNot in Region.");
            return;
        }
        BuildSite buildSite = getBuildSite(player.getTargetBlockExact(20), rg);
        if (buildSite == null) {
            MessageUtil.sendMessage(player, "&cNot a build site.");
        }
        buildSite.getRegion().getBuildSites().remove(buildSite);
        Faction owner = buildSite.getRegion().getOwner();
        owner.getFactionBuildings().remove(buildSite);
        MessageUtil.sendMessage(player, "&aBuildSite deleted.");
    }

    public @Nullable BuildSite getBuildSite(@NotNull Location loc, @NotNull Region region) {
        for (BuildSite buildSite : region.getBuildSites()) {
            if (buildSite.isInBuildSite(loc)) {
                return buildSite;
            }
        }
        return null;
    }

    public boolean hasOverlap(@NotNull Location corner1, @NotNull Location corner2, @NotNull BuildSite existingSite) {
        return existingSite.isInBuildSite(corner1) || existingSite.isInBuildSite(corner2);
    }

    @Contract("null, _ -> null; !null, _ -> _")
    public @Nullable BuildSite getBuildSite(@Nullable Block check, @NotNull Region region) {
        if (check == null) {
            return null;
        }
        for (BuildSite buildSite : region.getBuildSites()) {
            if (buildSite.getInteractive().equals(check.getLocation())) {
                return buildSite;
            }
        }
        return null;
    }

    private void tickBuildingEffects() {
        for (int i = 0; i < effectsPerTick; i++) {
            if (tickingEffects.isEmpty()) {
                return;
            }
            BuildingEffect effect = tickingEffects.poll();
            if (effect == null) {
                return;
            }
            effect.tick();
            tickingEffects.add(effect); // Add it back to the queue for the next run
        }
    }

    public List<BuildSite> getBuildingTickets() {
        return buildingTickets;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        BukkitRunnable loadTask = new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getBuildSiteCache().loadForChunk(event.getChunk());
            }
        };
        loadTask.runTaskAsynchronously(plugin);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        BukkitRunnable saveTask = new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getBuildSiteCache().saveForChunk(event.getChunk());
            }
        };
        saveTask.runTaskAsynchronously(plugin);
    }

    public static ItemStack getBuildingItemStack(Building building) {
        HItem item = Main.itemLibrary.get(ITEM_ID);
        ItemStack stack = item.getItem().getBukkitStack();
        stack.editMeta(meta -> {
            meta.getPersistentDataContainer().set(FBuildingItemBehaviour.KEY, PersistentDataType.STRING, building.getId());
            meta.displayName(building.getName());
        });
        return stack;
    }
}