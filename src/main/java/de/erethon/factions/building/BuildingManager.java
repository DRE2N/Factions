package de.erethon.factions.building;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.misc.FileUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.economy.FEconomy;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.LazyChunk;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FLogger;
import de.erethon.hephaestus.Hephaestus;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
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
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Malfrador
 */
public class BuildingManager implements Listener {

    private static final NamespacedKey BUILDING_ID = new NamespacedKey(Factions.get(), "building_id");
    private static final NamespacedKey FACTION_ID = new NamespacedKey(Factions.get(), "faction_id");

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
            Building building;
            try {
                if (file.getName().toLowerCase().contains("ploppable")) {
                    building = new PloppableBuilding(file, this);
                } else {
                    building = new Building(file, this);
                }
            }
            catch (Exception e) {
                FLogger.ERROR.log("Failed to load building from file " + file.getName() + ": " + e.getMessage());
                continue;
            }
            buildings.add(building);
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
        Region rg = plugin.getFPlayerCache().getByPlayer(player).getCurrentRegion();
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

    public Set<BuildSite> getBuildSites(@NotNull Location loc, @NotNull Region region) {
        Set<BuildSite> buildSites = new HashSet<>();
        for (BuildSite buildSite : region.getBuildSites()) {
            if (buildSite.isInBuildSite(loc)) {
                buildSites.add(buildSite);
            }
        }
        return buildSites;
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
                continue;
            }
            BuildingEffect effect = tickingEffects.poll();
            if (effect == null) {
                continue;
            }
            if (!effect.getSite().isActive()) {
                continue;
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
        // Check if we need to spawn any NPCs
        Region region = plugin.getRegionManager().getRegionByChunk(event.getChunk());
        if (region == null) {
            return;
        }
        if (region.getOwner() == null) {
            return;
        }
        Faction owner = region.getOwner();
        if (owner.getCoreRegion() != region) {
            return;
        }
        LazyChunk lazyChunk = new LazyChunk(event.getChunk());
        if (lazyChunk.equals(owner.getFHomeChunk())) {
            owner.spawnNPC();
        }
        // If the revolt started and no chunks were found, we try again
        if (owner.getUnrestLevel() > FEconomy.REVOLT_THRESHOLD && !owner.hasOngoingRevolt()) {
            int revoltAttempts = (int) Math.ceil(owner.getUnrestLevel()  * FEconomy.UNREST_SPAWN_MULTIPLIER);
            if (revoltAttempts > 0) {
                FLogger.ECONOMY.log(String.format("[%s] High unrest (%.2f) on chunk load, attempting to spawn revolt with %d attempts.",
                        owner.getName(),
                        owner.getUnrestLevel(),
                        revoltAttempts));
                owner.getEconomy().spawnRevolt(owner, Math.min(revoltAttempts, 10));
            }
        }
    }

    public static ItemStack getBuildingItemStack(Building building, Faction faction, Player player) {
        ItemStack stack = new ItemStack(Material.CHEST);
        stack.editMeta(meta -> {
            Component title = Component.translatable("factions.building.buildings." + building.getId() + ".name");
            meta.displayName(title);
            List<Component> lore = new ArrayList<>();
            for (int i = 1; i < 6; i++) {
                String key = "factions.building.buildings." + building.getId() + ".description." + i;
                Component line = Component.translatable(key);
                lore.add(line);
            }
            meta.lore(lore);
            meta.getPersistentDataContainer().set(BUILDING_ID, PersistentDataType.STRING, building.getId());
            meta.getPersistentDataContainer().set(FACTION_ID, PersistentDataType.INTEGER, faction.getId());
        });
        return stack;
    }

    public static List<Building> getUnlockedBuildingsForPlacement(FPlayer fPlayer, Faction faction, Region region) {
        List<Building> available = new ArrayList<>();
        for (Building building : Factions.get().getBuildingManager().getBuildings()) {
            if (building.getRequiredBuildings().isEmpty()) {
                available.add(building);
                continue;
            }
            Set<Building> factionBuildings = new HashSet<>();
            for (BuildSite site : faction.getFactionBuildings()) {
                if (site.isActive() && site.isFinished()) {
                    factionBuildings.add(site.getBuilding());
                }
            }
            for (BuildSite site : region.getBuildSites()) {
                if (site.isActive() && site.isFinished()) {
                    factionBuildings.add(site.getBuilding());
                }
            }
            for (Map.Entry<String, Integer> entry : building.getRequiredBuildings().entrySet()) {
                Building required = Factions.get().getBuildingManager().getById(entry.getKey());
                int buildingsInFaction = 0;
                for (Building factionBuilding : factionBuildings) {
                    if (factionBuilding.getId().equals(required.getId())) {
                        buildingsInFaction++;
                    }
                }
                if (buildingsInFaction < entry.getValue()) {
                    break;
                }
                available.add(building);
            }
        }
        return available;
    }
}