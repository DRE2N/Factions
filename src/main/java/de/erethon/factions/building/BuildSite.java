package de.erethon.factions.building;

import de.erethon.factions.Factions;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.util.FUtil;
import io.papermc.paper.math.Position;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang.math.IntRange;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.Cancellable;
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

/**
 * @author Malfrador
 */
public class BuildSite extends YamlConfiguration implements InventoryHolder, Listener {

    Factions plugin = Factions.get();
    BuildingManager buildingManager = plugin.getBuildingManager();

    private UUID uuid;

    private Building building;
    private Region region;
    private Location corner;
    private Location otherCorner;
    private final Set<BuildSiteSection> sections = new HashSet<>();
    private final HashMap<String, Position> namedPositions = new HashMap<>();
    private long chunkKey;
    private Location interactive;
    private String problemMessage = null;
    private Map<Material, Integer> placedBlocks = new HashMap<>();
    private boolean finished;
    private boolean active;
    private boolean hasTicket = false;
    private boolean isBusy = false;
    private Inventory inventory;
    private final Set<BuildingEffect> buildingEffects = new HashSet<>();
    private final Set<ItemStack> buildingStorage = new HashSet<>();

    private UUID progressHoloUUID = null;

    private int blockChangeCounter = 0;
    private TextDisplay progressHolo;

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
        updateHolo();
        if (region.getOwner() == null) {
            FLogger.BUILDING.log("Region owner is null for " + region.getName());
        }
    }

    public BuildSite(File file) {
        try {
            load(file);
        } catch (IOException | InvalidConfigurationException | NullPointerException e) {
            FLogger.BUILDING.log("Failed to load build site from file " + file.getName() + ". Error: " + e.getMessage());
        }
    }

    public void updateHolo() {
        if (progressHoloUUID != null) {
            BukkitRunnable run = new BukkitRunnable() {
                @Override
                public void run() {
                    progressHolo = (TextDisplay) Bukkit.getEntity(progressHoloUUID);
                }
            };
            run.runTask(plugin); // This is otherwise called on the chunk loading thread and throws an exception.
        } else {
            progressHolo = interactive.getWorld().spawn(interactive, TextDisplay.class);
            progressHoloUUID = progressHolo.getUniqueId();
        }
        progressHolo.setBillboard(Display.Billboard.CENTER);
        progressHolo.setDefaultBackground(false);
        progressHolo.setBackgroundColor(Color.fromARGB(0,0,0,0));
        Component content = building.getName().color(NamedTextColor.GOLD);
        content = content.append(Component.newline());
        if (!finished) {
            for (Material material : building.getRequiredBlocks().keySet()) {
                content = content.append(Component.translatable(material.translationKey(), NamedTextColor.GOLD).append(Component.text(": ", NamedTextColor.DARK_GRAY)).append(getProgressComponent(material))).append(Component.newline());
            }
            progressHolo.text(content);
            return;
        }
        if (problemMessage != null && hasTicket) {
            content = content.append(Component.translatable("factions.building.status.problem", NamedTextColor.DARK_RED));
            content = content.append(Component.text(problemMessage, NamedTextColor.RED));
            progressHolo.text(content);
            return;
        }
        progressHolo.text(content);
    }


    public void finishBuilding() {
        for (BuildingEffectData effect : building.getEffects()) {
            getRegion().getOwner().getBuildingEffects().add(new BuildingEffect(effect, this));
        }
        finished = true;
        problemMessage = null;
        hasTicket = false;
        getRegion().getOwner().sendTranslatable("factions.building.status.accepted", Component.text(getBuilding().getId()), Component.text(getRegion().getName()));
    }

    public void removeEffects() {
        for (BuildingEffect effect : getRegion().getOwner().getBuildingEffects()) {
            if (effect.getSite() == this) {
                effect.remove();
            }
        }
    }

    public void blockPlaced(Player player, Cancellable event) { // Only schedule a new update after x number of blocks have been changed.
        blockChangeCounter++;
        if (blockChangeCounter >= 5) {
            blockChangeCounter = 0;
            scheduleProgressUpdate();
        }
        if (!active) {
            return;
        }
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(player);
        for (BuildingEffect effect : buildingEffects) {
            effect.onPlaceBlock(fPlayer, player.getLocation().getBlock(), getSectionsForLocation(player.getLocation()), event);
        }
    }

    public void blockBroken(Player player, Cancellable event) {
        blockChangeCounter++;
        if (blockChangeCounter >= 5) {
            blockChangeCounter = 0;
            scheduleProgressUpdate();
        }
        if (!active) {
            return;
        }
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(player);
        for (BuildingEffect effect : buildingEffects) {
            effect.onBreakBlock(fPlayer, player.getLocation().getBlock(), getSectionsForLocation(player.getLocation()), event);
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
                updateHolo();
                for (Material material : building.getRequiredBlocks().keySet()) {
                    if (getPlacedBlocks().get(material) < building.getRequiredBlocks().get(material)) {
                        fini = false;
                    }
                }
                if (finished && !fini) {
                    finished = false;
                    getRegion().getOwner().sendTranslatable("factions.building.status.destroyed", Component.text(getBuilding().getId()), Component.text(getRegion().getName()));
                    removeEffects();
                    return;
                }
                if (fini && !isFinished() && !hasTicket) {
                    buildingManager.getBuildingTickets().add(getSite());
                    hasTicket = true;
                    getRegion().getOwner().sendTranslatable("factions.building.status.completed.info", Component.text(getBuilding().getId()), Component.text(getRegion().getName()));
                    getRegion().getOwner().sendTranslatable("factions.building.status.completed.ticketHint");
                    FLogger.BUILDING.log("A new BuildSite ticket for " + getBuilding().getId() + " in " + getRegion().getName() + " was created.");
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

    public @NotNull Component getProgressComponent(@Nullable Material block) {
        int total = 0;
        if (building.getRequiredBlocks().get(block) != null) {
            total = building.getRequiredBlocks().get(block);
        }
        int placed = 0;
        if (getPlacedBlocks().get(block) != null) {
            placed = getPlacedBlocks().get(block);
        }
        if (placed >= total) {
            return Component.text(placed, NamedTextColor.GREEN).append(Component.text("/" + total, NamedTextColor.DARK_GRAY).append(Component.text(" ✔", NamedTextColor.GREEN)));
        }
        return Component.text(placed, NamedTextColor.RED).append(Component.text("/" + total, NamedTextColor.DARK_GRAY).append(Component.text(" ✘", NamedTextColor.RED)));
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

    /**
     * @param location The location to check for sections.
     * @return A set of sections that contain the given location. Can be empty.
     */
    public Set<BuildSiteSection> getSectionsForLocation(Location location) {
        Set<BuildSiteSection> result = new HashSet<>();
        for (BuildSiteSection section : sections) {
            if (section.contains(location)) {
                result.add(section);
            }
        }
        return result;
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
        inventory = Bukkit.createInventory(this, 54, Component.translatable("factions.building.storage.title"));
        for (ItemStack item : buildingStorage) {
            inventory.addItem(item);
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        return inventory;
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

    //
    // Event handlers
    //
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

    //
    // Getters and setters
    //
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

    public @NotNull Set<BuildSiteSection> getSections() {
        return sections;
    }

    public @NotNull Map<String, Position> getNamedPositions() {
        return namedPositions;
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

    public String getUUIDString() {
        return uuid.toString();
    }

    public Set<ItemStack> getBuildingStorage() {
        return buildingStorage;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public Set<BuildingEffect> getEffects() {
        return buildingEffects;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            for (BuildingEffect effect : buildingEffects) {
                effect.remove();
            }
        } else {
            for (BuildingEffect effect : buildingEffects) {
                effect.apply();
            }
        }
    }

    //
    // Effect triggers
    //
    public void onEnter(FPlayer player) {
        if (!active) {
            return;
        }
        for (BuildingEffect effect : buildingEffects) {
            effect.onEnter(player);
        }
    }

    public void onLeave(FPlayer player) {
        if (!active) {
            return;
        }
        for (BuildingEffect effect : buildingEffects) {
            effect.onLeave(player);
        }
    }

    public void onFactionJoin(FPlayer player) {
        if (!active) {
            return;
        }
        for (BuildingEffect effect : buildingEffects) {
            effect.onFactionJoin(player);
        }
    }

    public void onFactionLeave(FPlayer player) {
        if (!active) {
            return;
        }
        for (BuildingEffect effect : buildingEffects) {
            effect.onFactionLeave(player);
        }
    }

    public void onPayday() {
        if (!active) {
            return;
        }
        for (BuildingEffect effect : buildingEffects) {
            effect.onPayday();
        }
    }

    public void onBlockBreakInRegion(FPlayer player, Block block, Cancellable event) {
        if (!active) {
            return;
        }
        for (BuildingEffect effect : buildingEffects) {
            effect.onBreakBlockRegion(player, block, event);
        }
    }

    public void onBlockPlaceInRegion(FPlayer player, Block block, Cancellable event) {
        if (!active) {
            return;
        }
        for (BuildingEffect effect : buildingEffects) {
            effect.onPlaceBlockRegion(player, block, event);
        }
    }

    //
    // Serialization
    //
    @Override
    public void load(@NotNull File file) throws IOException, InvalidConfigurationException {
        super.load(file);
        uuid = UUID.fromString(file.getName().replace(".yml", ""));
        progressHoloUUID = UUID.fromString(getString("progressHoloUUID", "00000000-0000-0000-0000-000000000000"));
        building = buildingManager.getById(getString("building"));
        finished = getBoolean("finished");
        hasTicket = getBoolean("hasTicket");
        problemMessage = getString("problemMessage");
        region = plugin.getRegionManager().getRegionById(getInt("region"));
        corner = Location.deserialize(getConfigurationSection("location.corner").getValues(false));
        otherCorner = Location.deserialize(getConfigurationSection("location.otherCorner").getValues(false));
        interactive = Location.deserialize(getConfigurationSection("location.interactable").getValues(false));
        if (contains("sections")) {
            for (String id : getConfigurationSection("sections").getKeys(false)) {
                ConfigurationSection section = getConfigurationSection("sections." + id);
                Position corner1 = FUtil.parsePosition(section.getString("corner1"));
                Position corner2 = FUtil.parsePosition(section.getString("corner2"));
                boolean protectedSection = section.getBoolean("protectedSection", false);
                BuildSiteSection buildSiteSection = new BuildSiteSection(id, corner1, corner2, protectedSection);
                sections.add(buildSiteSection);
            }
        }
        if (contains("namedPositions")) {
            for (String id : getConfigurationSection("namedPositions").getKeys(false)) {
                namedPositions.put(id, FUtil.parsePosition(getString("namedPositions." + id)));
            }
        }
        region.getBuildSites().add(this);
        if (!finished) {
            scheduleProgressUpdate();
        }
        plugin.getBuildSiteCache().add(this, interactive.getChunk());
        for (BuildingEffectData data : building.getEffects()) {
            data.newEffect(this);
        }
        if (finished && !isDestroyed()) {
            setActive(true);
        }
        updateHolo();
        FLogger.BUILDING.log("Loaded build site " + uuid + " for " + building.getId() + " in " + region.getName());
    }

    public void save() throws IOException {
        File file = new File(Factions.BUILD_SITES, uuid + ".yml");
        set("progressHoloUUID", progressHoloUUID.toString());
        set("building", building.getId());
        set("region", region.getId());
        set("location.corner", corner.serialize());
        set("location.otherCorner", otherCorner.serialize());
        set("location.interactable", interactive.serialize());
        set("finished", finished);
        set("hasTicket", hasTicket);
        set("problemMessage", problemMessage);
        for (BuildSiteSection section : sections) {
            set("sections." + section.name() + ".corner1", FUtil.positionToString(section.corner1()));
            set("sections." + section.name() + ".corner2", FUtil.positionToString(section.corner2()));
            set("sections." + section.name() + ".protectedSection", section.protectedSection());
        }
        for (Map.Entry<String, Position> entry : namedPositions.entrySet()) {
            set("namedPositions." + entry.getKey(), FUtil.positionToString(entry.getValue()));
        }
        super.save(file);
    }
}