package de.erethon.factions.building;

import de.erethon.factions.Factions;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.util.FTutorial;
import de.erethon.factions.util.FUtil;
import de.erethon.factions.util.IntRange;
import io.papermc.paper.math.Position;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
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
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

    private File file;
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
    private final Map<Material, Set<BuildSiteCoordinate>> blocksOfInterest = new HashMap<>();
    private boolean finished;
    private boolean active;
    private boolean hasTicket = false;
    private boolean isBusy = false;
    private Inventory inventory;
    private final Set<BuildingEffect> buildingEffects = new HashSet<>();
    private final Set<ItemStack> buildingStorage = new HashSet<>();

    private final Set<ItemStack> inputItems = new HashSet<>();
    private final Set<ItemStack> outputItems = new HashSet<>();
    private boolean requiresInputChest = false;
    private boolean requiresOutputChest = false;
    private final Set<Position> chestPositions = new HashSet<>();
    private final HashMap<String, String> additionalData = new HashMap<>();

    private UUID progressHoloUUID = null;

    private int blockChangeCounter = 0;
    private TextDisplay progressHolo;
    private Location inputChestLocation;
    private Location outputChestLocation;

    public BuildSite(@NotNull Building building, @NotNull Region region, @NotNull Location loc1, @NotNull Location loc2, @NotNull Location center) {
        this.building = building;
        this.region = region;
        if (region.getOwner() == null) {
            FLogger.BUILDING.log("Region owner is null for " + region.getName());
            return;
        }
        finished = false;
        corner = loc1;
        otherCorner = loc2;
        interactive = center.add(0, 1.5, 0);
        chunkKey = center.getChunk().getChunkKey();
        FLogger.BUILDING.log("Created new building site in " + this.region.getName() + ". Building type: " + building.getId());
        region.getBuildSites().add(this);
        region.getOwner().getFactionBuildings().add(this);
        uuid = UUID.randomUUID();
        updateHolo();
        BuildSiteCache cache = plugin.getBuildSiteCache();
        if (!cache.isInCache(uuid)) {
            cache.addBuildSite(this);
        }
    }

    public BuildSite(File file) {
        this.file = file;
    }

    public void updateHolo() {
        if (progressHoloUUID != null) {
            progressHolo = (TextDisplay) Bukkit.getEntity(progressHoloUUID);
        } else {
            progressHolo = interactive.getWorld().spawn(interactive, TextDisplay.class);
            progressHoloUUID = progressHolo.getUniqueId();
        }
        if (progressHolo == null) {
            progressHolo = interactive.getWorld().spawn(interactive, TextDisplay.class);
            progressHoloUUID = progressHolo.getUniqueId();
            return;
        }
        progressHolo.setBillboard(Display.Billboard.CENTER);
        progressHolo.setDefaultBackground(false);
        progressHolo.setBackgroundColor(Color.fromARGB(0,0,0,0));
        Component content = Component.translatable("factions.building.buildings." + building.getId() + ".name").color(NamedTextColor.GOLD);
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
        this.buildingEffects.clear();
        FLogger.BUILDING.log("Finishing building " + building.getId() + " for faction " + (getFaction() != null ? getFaction().getName() : "N/A") + " in region " + region.getName() + ". Instantiating effects.");

        if (getFaction() == null) {
            FLogger.BUILDING.log("Cannot finish building " + building.getId() + ": Faction owner is null for region " + region.getName());
            this.problemMessage = "Cannot activate: No owning faction. Disbanded?";
            this.hasTicket = true;
            updateHolo();
            return;
        }

        if (building != null && building.getEffects() != null) {
            for (BuildingEffectData effectData : building.getEffects()) {
                BuildingEffect newEffectInstance = effectData.newEffect(this);
                if (newEffectInstance != null) {
                    this.buildingEffects.add(newEffectInstance);
                    FLogger.BUILDING.log("Instantiated and added effect " + newEffectInstance.getClass().getSimpleName() + " for finished building " + building.getId());
                } else {
                    FLogger.BUILDING.log("Failed to instantiate effect from data: " + effectData.getId() + " for finished building " + building.getId());
                }
            }
        }  else {
            FLogger.BUILDING.log("Building or building effects list is null while finishing build site " + uuid + ". No effects instantiated.");
        }

        finished = true;
        problemMessage = null;
        hasTicket = false;

        setActive(true); // Activate the newly added effects

        getRegion().getOwner().sendTranslatable("factions.building.status.accepted", Component.text(getBuilding().getId()), Component.text(getRegion().getName()));
        updateHolo();
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
                    for (Player player : interactive.getNearbyPlayers(16)) {
                        FTutorial.showHint(player, "building.first_site_completed");
                    }
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
                    if (building.getBlocksOfInterest().contains(type)) {
                        blocksOfInterest.getOrDefault(type, new HashSet<>()).add(new BuildSiteCoordinate(block.getX(), block.getY(), block.getZ()));
                    }
                }
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
        Region rg = fPlayer.getCurrentRegion();
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

    public Set<BuildSiteCoordinate> getCoordinatesFor(Material material) {
        return blocksOfInterest.getOrDefault(material, new HashSet<>());
    }

    public Set<String> getMissingSections() {
        Set<String> result = new HashSet<>();
        for (String section : building.getRequiredSections()) {
            if (sections.stream().noneMatch(buildSiteSection -> buildSiteSection.name().equalsIgnoreCase(section))) {
                result.add(section);
            }
        }
        return result;
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
    private void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.TRAPPED_CHEST) {
            return;
        }
        if (event.getClickedBlock().getLocation().equals(inputChestLocation)) {

        }
        if (event.getClickedBlock().getLocation().equals(outputChestLocation)) {
            event.setCancelled(true);
            createInventoryFromStorage();
            Player player = event.getPlayer();
            if (inventory == null) {
                player.sendMessage(Component.translatable("factions.building.storage.error").color(NamedTextColor.RED));
                return;
            }
            player.openInventory(inventory);
        }
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

    public int getBlockCount(Material material) {
        return placedBlocks.getOrDefault(material, 0);
    }

    public @NotNull long getChunkKey() {
        return chunkKey;
    }

    public @NotNull Location getInteractive() {
        return interactive;
    }

    public World getWorld() {
        return interactive.getWorld();
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

    public void setRequiresInputChest(boolean requiresInputChest) {
        this.requiresInputChest = requiresInputChest;
    }

    public void setRequiresOutputChest(boolean requiresOutputChest) {
        this.requiresOutputChest = requiresOutputChest;
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

    public Set<ItemStack> getInputItems() {
        if (inputChestLocation == null) {
            return new HashSet<>();
        }
        if ((inputChestLocation.getBlock().getType() != Material.TRAPPED_CHEST && inputChestLocation.getBlock().getType() != Material.CHEST)) {
            FLogger.BUILDING.log("Input chest location is not a chest: " + inputChestLocation);
            return new HashSet<>();
        }
        Chest inputChest = (Chest) inputChestLocation.getBlock().getState();
        Inventory inv = inputChest.getInventory();
        Set<ItemStack> items = new HashSet<>();
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                items.add(item);
            }
        }
        inputItems.clear();
        inputItems.addAll(items);
        return inputItems;
    }

    public Set<ItemStack> getOutputItems() {
        return outputItems;
    }

    public Set<BuildingEffect> getEffects() {
        return buildingEffects;
    }

    public Set<Position> getChestLocations() {
        return chestPositions;
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
            onChunkLoad(); // Create chests etc if they don't exist yet
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

    public void onPrePayday() {
        if (!active) {
            return;
        }
        for (BuildingEffect effect : buildingEffects) {
            effect.onPrePayday();
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

    public void onEntityDeath(FPlayer player, EntityDeathEvent event) {
        if (!active) {
            return;
        }
        for (BuildingEffect effect : buildingEffects) {
            effect.onEntityKill(player, event);
        }
    }

    public void onChunkLoad() {
        if (!active) {
            return;
        }
        for (BuildingEffect effect : buildingEffects) {
            effect.onChunkLoad();
        }
        if (inputChestLocation == null || outputChestLocation == null) {
            Location interactiveLocation = getInteractive();
            if (getNamedPositions().containsKey("input_chest")) {
                inputChestLocation = getNamedPositions().get("input_chest").toLocation(getWorld());
            } else if (requiresInputChest) {
                inputChestLocation = interactiveLocation.clone().add(0, 0, 1);
                inputChestLocation.getBlock().setType(Material.TRAPPED_CHEST);
                Chest inputChest = (Chest) inputChestLocation.getBlock().getState();
                inputChest.customName(Component.translatable("factions.building.common.input_chest"));
                getNamedPositions().put("input_chest", inputChestLocation);
            }
            if (getNamedPositions().containsKey("output_chest")) {
                outputChestLocation = getNamedPositions().get("output_chest").toLocation(getWorld());
            } else if (requiresOutputChest) {
                outputChestLocation = interactiveLocation.clone().add(0, 0, -1);
                outputChestLocation.getBlock().setType(Material.CHEST);
                Chest outputChest = (Chest) outputChestLocation.getBlock().getState();
                outputChest.customName(Component.translatable("factions.building.common.output_chest"));
                getNamedPositions().put("output_chest", outputChestLocation);
            }
        }
    }

    public void onChunkUnload() {
        if (!active) {
            return;
        }
        for (BuildingEffect effect : buildingEffects) {
            effect.onChunkUnload();
        }
    }

    //
    // Serialization
    //
    public void load() throws IOException, InvalidConfigurationException {
        super.load(file);
        if (!file.exists()) {
            FLogger.BUILDING.log("File " + file.getName() + " does not exist. Cannot load build site.");
            return;
        }
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
        plugin.getBuildSiteCache().addToChunkCache(this);
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
        if (contains("chestPositions")) {
            for (String id : getStringList("chestPositions")) {
                chestPositions.add(FUtil.parsePosition(id));
            }
        }
        if (contains("buildingStorage")) {
            for (String id : getStringList("buildingStorage")) {
                buildingStorage.add(ItemStack.deserializeBytes(Base64.getDecoder().decode(id)));
            }
        }
        if (contains("outputItems")) {
            for (String id : getStringList("outputItems")) {
                outputItems.add(ItemStack.deserializeBytes(Base64.getDecoder().decode(id)));
            }
        }
        if (contains("placedBlocks")) {
            for (String id : getConfigurationSection("placedBlocks").getKeys(false)) {
                placedBlocks.put(Material.valueOf(id), getInt("placedBlocks." + id));
            }
        }
        if (contains("blocksOfInterest")) {
            ConfigurationSection section = getConfigurationSection("blocksOfInterest");
            for (String id : section.getKeys(false)) {
                List<String> coords = section.getStringList(id);
                Set<BuildSiteCoordinate> set = new HashSet<>();
                for (String coord : coords) {
                    set.add(BuildSiteCoordinate.fromString(coord));
                }
                blocksOfInterest.put(Material.valueOf(id), set);
            }
        }
        if (contains("additionalData")) {
            for (String id : getConfigurationSection("additionalData").getKeys(false)) {
                additionalData.put(id, getString("additionalData." + id));
            }
        }
        region.getBuildSites().add(this);
        if (!finished) {
            scheduleProgressUpdate();
        }
        FLogger.BUILDING.log("Loading effects for build site " + uuid + " (" + building.getId() + ")");
        this.buildingEffects.clear();
        if (building != null && building.getEffects() != null) {
            for (BuildingEffectData data : building.getEffects()) {
                try {
                    FLogger.BUILDING.log("Loading effect " + data + " for " + uuid);
                    BuildingEffect effect = data.newEffect(this);
                    if (effect != null) {
                        this.buildingEffects.add(effect);
                        FLogger.BUILDING.log("Successfully loaded and added effect " + effect.getClass().getSimpleName() + " for " + uuid);
                    } else {
                        FLogger.BUILDING.log("Failed to create effect instance from data: " + data.getId() + " for " + uuid);
                    }
                } catch (Exception e) {
                    FLogger.BUILDING.log("Failed to load building effect " + data.getId() + " for " + building.getId() + " in " + region.getName() + " (BuildSite UUID: " + uuid + ")");
                }
            }
        } else {
            FLogger.BUILDING.log("Building or building effects list is null for build site " + uuid + ". No effects loaded.");
        }

        if (finished && !isDestroyed()) {
            FLogger.BUILDING.log("Build site " + uuid + " is finished and not destroyed, activating effects.");
            setActive(true);
        } else {
            FLogger.BUILDING.log("Build site " + uuid + " is not active (finished=" + finished + ", destroyed=" + isDestroyed() + "). Effects not activated by load().");
        }
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
        List<String> positions = new ArrayList<>();
        for (Position position : chestPositions) {
            positions.add(FUtil.positionToString(position));
        }
        set("chestPositions", positions);
        List<String> items = new ArrayList<>();
        for (ItemStack stack : buildingStorage) {
            items.add(java.util.Base64.getEncoder().encodeToString(stack.serializeAsBytes()));
        }
        set("buildingStorage", items);
        for (ItemStack stack : outputItems) {
            items.add(java.util.Base64.getEncoder().encodeToString(stack.serializeAsBytes()));
        }
        for (Map.Entry<Material, Integer> entry : placedBlocks.entrySet()) {
            set("placedBlocks." + entry.getKey().name(), entry.getValue());
        }
        for (Material type : blocksOfInterest.keySet()) {
            YamlConfiguration section = new YamlConfiguration();
            List<String> coords = new ArrayList<>();
            for (BuildSiteCoordinate coordinate : blocksOfInterest.get(type)) {
                coords.add(coordinate.toString());
            }
            section.set(type.name(), coords);
            set("blocksOfInterest." + type.name(), section);
        }
        for (Map.Entry<String, String> entry : additionalData.entrySet()) {
            set("additionalData." + entry.getKey(), entry.getValue());
        }
        set("outputItems", items);
        super.save(file);
    }
}