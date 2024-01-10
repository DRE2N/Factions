package de.erethon.factions.building;

import de.erethon.factions.Factions;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FLogger;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.math.IntRange;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BuildSite extends YamlConfiguration implements InventoryHolder, Listener {

    Factions plugin = Factions.get();
    BuildingManager buildingManager = plugin.getBuildingManager();

    private UUID uuid;

    private Building building;
    private Region region;
    private Location corner;
    private Location otherCorner;
    private long chunkKey;
    private Location interactive;
    private String problemMessage = null;
    private Map<Material, Integer> placedBlocks = new HashMap<>();
    private boolean finished;
    private boolean hasTicket = false;
    private boolean isBusy = false;
    private Inventory inventory;
    private final Set<BuildingEffect> activeBuildingEffects = new HashSet<>();
    private final Set<ItemStack> buildingStorage = new HashSet<>();

    public BuildSite(@NotNull Building building, @NotNull Region region, @NotNull Location loc1, @NotNull Location loc2, @NotNull Location center) {
        this.building = building;
        this.region = region;
        finished = false;
        corner = loc1;
        otherCorner = loc2;
        interactive = center;
        chunkKey = center.getChunk().getChunkKey();
        FLogger.BUILDING.log("Created new building site in " + this.region.getName() + ". Building type: " + building.getName());
        region.getBuildSites().add(this);
        uuid = UUID.randomUUID();
        plugin.getBuildSiteCache().add(this, center.getChunk());
        //setupHolo();
    }

    public BuildSite(File file) {
        try {
            load(file);
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /*public void setupHolo() {
        if (!fConfig.areHologramsEnabled()) {
            return;
        }
        if (progressHolo != null) {
            progressHolo.delete();
        }
        MessageUtil.log("Setting up holo");
        MessageUtil.log(interactive.toString());
        MessageUtil.log(interactive.getBlock().toString());
        progressHolo = HologramsAPI.createHologram(plugin, interactive.getBlock().getRelative(0, 2, 0).getLocation());
        progressHolo.appendTextLine(ChatColor.GOLD + building.getName()).setTouchHandler(fTouchHandler);
        progressHolo.appendTextLine(" ").setTouchHandler(fTouchHandler);
        String bar = "----------";
        progressHolo.appendTextLine(bar).setTouchHandler(fTouchHandler); // Placeholder
        Location newLoc = progressHolo.getLocation();
        for (Material material : building.getRequiredBlocks().keySet()) {
            String output = plugin.getFTranslation().getTranslatedName(material);
            progressHolo.appendTextLine(ChatColor.GRAY + output + ChatColor.DARK_GRAY + ": " + (getProgressString(material))).setTouchHandler(fTouchHandler);
            newLoc.add(0, 0.2,0); // Holo needs to be moved to not get stuck in the floor
        }
        progressHolo.teleport(newLoc);
        progressHolo.appendTextLine(FMessage.BUILDING_SITE_HINT.getMessage()).setTouchHandler(fTouchHandler);
    }

    public void updateHolo() {
        if (!fConfig.areHologramsEnabled()) {
            return;
        }
        if (progressHolo == null) {
            setupHolo();
        }
        MessageUtil.log("Update holo");
        progressHolo.clearLines();
        progressHolo.appendTextLine(ChatColor.GOLD + building.getName()).setTouchHandler(fTouchHandler);
        progressHolo.appendTextLine(" ").setTouchHandler(fTouchHandler);
        if (!isFinished() || !buildingManager.getBuildingTickets().contains(getSite())) {
            for (Material material : building.getRequiredBlocks().keySet()) {
                String output = plugin.getFTranslation().getTranslatedName(material);
                progressHolo.appendTextLine(ChatColor.GRAY + output + ChatColor.DARK_GRAY + ": " + (getProgressString(material))).setTouchHandler(fTouchHandler);
            }
        }
        if (buildingManager.getBuildingTickets().contains(getSite())) {
            progressHolo.appendTextLine(FMessage.BUILDING_SITE_WAITING.getMessage());
            if (problemMessage != null) {
                progressHolo.appendTextLine(ChatColor.RED + problemMessage);
            }
        }
        progressHolo.appendTextLine(FMessage.BUILDING_SITE_HINT.getMessage()).setTouchHandler(fTouchHandler);
    }*/

    public void finishBuilding() {
        for (BuildingEffectData effect : building.getEffects()) {
            getRegion().getOwner().getBuildingEffects().add(new BuildingEffect(effect, this));
        }
        finished = true;
        problemMessage = null;
        hasTicket = false;
        //getRegion().getOwner().sendMessage("&aEin(e) &6" + getBuilding().getName() + " &ain " + getRegion().getName() + " &awurde akzeptiert und die Effekte sind nun aktiv.");
    }

    public void removeEffects() {
        for (BuildingEffect effect : getRegion().getOwner().getBuildingEffects()) {
            if (effect.getSite() == this) {
                effect.remove();
            }
        }
    }

    public void scheduleProgressUpdate() {
        CompletableFuture<Chunk> chunk = getCorner().getWorld().getChunkAtAsync(getCorner());
        isBusy = true;
        BukkitRunnable waitForChunk = new BukkitRunnable() {
            @Override
            public void run() {
                if (chunk.isDone()) {
                    checkProgress();
                    cancel();
                }
            }
        };
        waitForChunk.runTaskTimer(plugin, 10,10);
    }

    public boolean isDestroyed() {
        boolean damaged = false;
        for (Material material : building.getRequiredBlocks().keySet()) {
            if (getPlacedBlocks().get(material) < building.getRequiredBlocks().get(material)) {
                damaged = true;
            }
        }
        return damaged;
    }

    public void checkProgress() {
        isBusy = true;
        BukkitRunnable complete = new BukkitRunnable() {
            @Override
            public void run() {
                isBusy = false;
                boolean fini = true;
                for (Material material : building.getRequiredBlocks().keySet()) {
                    if (getPlacedBlocks().get(material) < building.getRequiredBlocks().get(material)) {
                        fini = false;
                    }
                }
                if (finished && !fini) {
                    finished = false;
                    getRegion().getOwner().sendMessage("<green>Ein(e) &6" + getBuilding().getName() + " <green>in " + getRegion().getName() + " <green>wurde zerstört!");
                    removeEffects();
                    return;
                }
                if (fini && !isFinished() && !hasTicket) {
                    buildingManager.getBuildingTickets().add(getSite());
                    hasTicket = true;
                    getRegion().getOwner().sendMessage("<green>Ein(e) <gold>" + getBuilding().getName() + " <green>in " + getRegion().getName() + " <green>wurde fertiggestellt");
                    getRegion().getOwner().sendMessage("<gray>Ein Ticket wurde automatisch erstellt und das Gebäude wird zeitnah überprüft.");
                    FLogger.BUILDING.log("A new BuildSite ticket for " + getBuilding().getName() + " in " + getRegion().getName() + " was created.");
                }
            }
        };
        BukkitRunnable runAsync = new BukkitRunnable() {
            @Override
            public void run() {
                Set<Block> blocks;
                Map<Material, Integer> placed = new HashMap<>();
                blocks = getBlocks(corner.getWorld());
                FLogger.BUILDING.log(building.getRequiredBlocks().toString());
                for (Block block : blocks) {
                    Material type = block.getType();
                    if (building.getRequiredBlocks().containsKey(type)) {
                        int amount = 0;
                        if (placed.containsKey(type)) {
                            amount = placed.get(type);
                        }
                        placed.put(type, amount + 1);
                    }
                }
                FLogger.BUILDING.log("-------------------");
                FLogger.BUILDING.log(placed.toString());
                placedBlocks = placed;
                complete.runTask(plugin);
            }
        };
        runAsync.runTaskAsynchronously(plugin);
    }

    public @NotNull String getProgressString(@Nullable Material block) {
        int total = 0;
        if (building.getRequiredBlocks().get(block) != null) {
            total = building.getRequiredBlocks().get(block);
        }
        int placed = 0;
        if (getPlacedBlocks().get(block) != null) {
            placed = getPlacedBlocks().get(block);
        }
        if (placed >= total) {
            return ChatColor.translateAlternateColorCodes('&', "&a&m" + placed + "&a&m/" + total + "&a ✔");
        }
        return ChatColor.translateAlternateColorCodes('&', "&a" + placed + "&8/&7" + total);
    }

    public boolean isInBuildSite(@NotNull Location location) {
        double xp = location.getX();
        double yp = location.getY();
        double zp = location.getZ();

        double x1 = corner.getX();
        double y1 = corner.getY();
        double z1 = corner.getZ();
        double x2 = otherCorner.getX();
        double y2 = otherCorner.getY();
        double z2 = otherCorner.getZ();
        return new IntRange(x1, x2).containsDouble(xp) && new IntRange(y1, y2).containsDouble(yp) && new IntRange(z1, z2).containsDouble(zp);
    }

    public boolean isInBuildSite(@NotNull Player player) {
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(player);
        Region rg = fPlayer.getLastRegion();
        if (rg == null) {
            return false;
        }
        Location location = player.getLocation();
        return isInBuildSite(location);
    }

    public @NotNull Set<Block> getBlocks(@NotNull World world) {
        Set<Block> blockList = new HashSet<>();
        Set<Location> result = new HashSet<>();
        double minX = Math.min(corner.getX(), otherCorner.getX());
        double minY = Math.min(corner.getY(), otherCorner.getY());
        double minZ = Math.min(corner.getZ(), otherCorner.getZ());
        double maxX = Math.max(corner.getX(), otherCorner.getX());
        double maxY = Math.max(corner.getY(), otherCorner.getY());
        double maxZ = Math.max(corner.getZ(), otherCorner.getZ());
        for (double x = minX; x <= maxX; x+=1) {
            for (double y = minY; y <= maxY; y+=1) {
                for (double z = minZ; z <= maxZ; z+=1) {
                    result.add(new Location(world, x, y, z));
                }
            }
        }
        for (Location location : result) {
            blockList.add(world.getBlockAt(location));
        }
        return blockList;
    }

    public @Nullable Inventory createInventoryFromStorage() {
        if (interactive.getBlock().getType() != Material.CHEST) {
            return null;
        }
        inventory = Bukkit.createInventory(this, 54, Component.text("Building Storage"));
        for (ItemStack item : buildingStorage) {
            inventory.addItem(item);
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        return inventory;
    }

    @EventHandler
    private void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder(false) != this) {
            return;
        }
        // Do we need this if we reload the storage after closing the inventory? I don't trust the bukkit inventory api lol.
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || event.getAction() == InventoryAction.COLLECT_TO_CURSOR || event.getAction() == InventoryAction.PICKUP_ALL || event.getAction() == InventoryAction.PICKUP_HALF || event.getAction() == InventoryAction.PICKUP_ONE || event.getAction() == InventoryAction.PICKUP_SOME) {
            buildingStorage.remove(event.getCurrentItem());
            return;
        }
        if (event.getClickedInventory() == inventory) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) != this) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    private void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder(false) != this) {
            return;
        }
        buildingStorage.clear();
        for (ItemStack item : inventory.getContents()) {
            if (item == null) {
                continue;
            }
            buildingStorage.add(item);
        }
        HandlerList.unregisterAll(this);
    }

    public boolean addItemToStorage(@NotNull ItemStack item) {
        for (ItemStack itemStack : buildingStorage) {
            if (itemStack.isSimilar(item)) {
                int newAmount = itemStack.getAmount() + item.getAmount();
                if (newAmount > itemStack.getMaxStackSize()) {
                    return false;
                }
                itemStack.setAmount(newAmount);
                return true;
            }
        }
        if (buildingStorage.size() >= 54) {
            return false;
        }
        buildingStorage.add(item);
        return true;
    }

    public @NotNull BuildSite getSite(){
        return this;
    }

    public @NotNull Building getBuilding() {
        return building;
    }

    public @NotNull Region getRegion() {
        return region;
    }

    public @Nullable Faction getFaction() {
        return region.getFaction();
    }

    public @NotNull Location getCorner() {
        return corner;
    }

    public @NotNull Location getOtherCorner() {
        return otherCorner;
    }

    public @NotNull long getChunkKey() {
        return chunkKey;
    }

    public @NotNull Location getInteractive() {
        return interactive;
    }

    public @NotNull Map<Material, Integer> getPlacedBlocks() {
        return placedBlocks;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setProblemMessage(@NotNull String msg) {
        problemMessage = msg;
    }

    /**
     * @return true if there is already an async operation running on this buildsite.
     */
    public boolean isBusy() {
        return isBusy;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Set<ItemStack> getBuildingStorage() {
        return buildingStorage;
    }

    @Override
    public void load(@NotNull File file) throws IOException, InvalidConfigurationException {
        uuid = UUID.fromString(file.getName().replace(".yml", ""));
        building = buildingManager.getById(getString("building"));
        region = plugin.getRegionManager().getRegionById(getInt("region"));
        otherCorner = Location.deserialize(getConfigurationSection("location.otherCorner").getValues(false));
        interactive = Location.deserialize(getConfigurationSection("location.interactable").getValues(false));
        FLogger.BUILDING.log(corner.toString());
        finished = getBoolean("finished");
        hasTicket = getBoolean("hasTicket");
        problemMessage = getString("problemMessage");
        region.getBuildSites().add(this);
        scheduleProgressUpdate();
        for (BuildingEffectData data : building.getEffects()) {
            data.newEffect(this);
        }
    }

    @Override
    public void save(@NotNull File file) throws IOException {
        set("building", building.getId());
        set("region", region.getId());
        set("location.corner", corner.serialize());
        set("location.otherCorner", otherCorner.serialize());
        set("location.interactable", interactive.serialize());
        set("finished", finished);
        set("hasTicket", hasTicket);
        set("problemMessage", problemMessage);
        super.save(file);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}