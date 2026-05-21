package de.erethon.factions.building;

import de.erethon.factions.Factions;
import de.erethon.factions.building.effects.AddHousing;
import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.economy.population.PopulationRequirementLines;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.ClaimableRegion;
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
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
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
import java.util.function.Predicate;

/**
 * @author Malfrador
 */
public class BuildSite extends YamlConfiguration implements InventoryHolder, Listener {

    private static final Component HOLOGRAM_LINE_BREAK = Component.text("\n");

    Factions plugin = Factions.get();
    BuildingManager buildingManager = plugin.getBuildingManager();

        private UUID uuid;

    private File file;
    private Building building;
    private ClaimableRegion region;
    private Location corner;
    private Location otherCorner;
    private final Set<BuildSiteSection> sections = new HashSet<>();
    private final HashMap<String, Position> namedPositions = new HashMap<>();
    private long chunkKey;
    private Location interactive;
    private String problemMessage = null;
    private Map<Material, Integer> placedBlocks = new HashMap<>();
    private final Map<Material, Set<BuildSiteCoordinate>> blocksOfInterest = new HashMap<>();
    private BuildSiteState state = BuildSiteState.PLACED;
    private boolean finished;
    private boolean active;
    private boolean hasTicket = false;
    private boolean isBusy = false;
    private boolean resourceInputsAvailable = true;
    private boolean upgradeReady = false;
    private boolean upgradeInProgress = false;
    private String upgradeTargetBuilding;
    private int upgradeSatisfiedPaydays = 0;
    private Inventory inventory;
    private final Set<BuildingEffect> buildingEffects = new HashSet<>();
    private final Set<ItemStack> buildingStorage = new HashSet<>();

    private final Set<ItemStack> inputItems = new HashSet<>();
    private final Set<ItemStack> outputItems = new HashSet<>();
    private final List<ItemStack> inputBuffer = new ArrayList<>();
    private final List<ItemStack> outputBuffer = new ArrayList<>();
    private int inputBufferSlots = 54;
    private int outputBufferSlots = 54;
    private boolean requiresInputChest = false;
    private boolean requiresOutputChest = false;
    private final Set<Position> chestPositions = new HashSet<>();
    private final HashMap<String, String> additionalData = new HashMap<>();

    private UUID progressHoloUUID = null;
    private UUID hologramInteractionUUID = null;

    private int blockChangeCounter = 0;
    private TextDisplay progressHolo;
    private Interaction hologramInteraction;
    private Location inputChestLocation;
    private Location outputChestLocation;

    private Map<FSetTag, Integer> placedBlocksByTag = new HashMap<>();
    private Map<Material, Integer> upgradePlacedBlocks = new HashMap<>();
    private Map<FSetTag, Integer> upgradePlacedBlocksByTag = new HashMap<>();

    public BuildSite(@NotNull Building building, @NotNull ClaimableRegion region, @NotNull Location loc1, @NotNull Location loc2, @NotNull Location center) {
        this.building = building;
        this.region = region;
        if (region.getOwner() == null) {
            FLogger.BUILDING.log("Region owner is null for " + region.getName());
            return;
        }
        finished = false;
        state = BuildSiteState.PLACED;
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
        if (interactive == null || !isLoaded(interactive)) {
            return;
        }
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
        ensureHologramInteraction();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("factions.building.buildings." + building.getId() + ".name").color(NamedTextColor.GOLD));
        lines.add(Component.translatable("factions.building.state." + state.name().toLowerCase()).color(getStateColor()));
        if (upgradeReady) {
            lines.add(Component.translatable("factions.building.upgrade.ready").color(NamedTextColor.GREEN));
        }
        if (upgradeInProgress) {
            lines.add(Component.translatable("factions.building.upgrade.in_progress",
                    getUpgradeTargetName(),
                    Component.text(getUpgradeProgressPercent() + "%", NamedTextColor.YELLOW)));
        }
        if (!finished) {
            lines.add(Component.translatable("factions.building.hologram.progress",
                    Component.text(getProgressPercent() + "%", NamedTextColor.YELLOW)));
            for (BlockRequirement req : building.getBlockRequirements()) {
                if (req.isTagRequirement()) {
                    lines.add(Component.text(req.getTag().getName(), NamedTextColor.GOLD)
                            .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                            .append(getProgressComponentForTag(req.getTag(), req.getAmount())));
                }
                if (req.isMaterialRequirement() && req.getMaterial() != null) {
                    int placed = placedBlocks.getOrDefault(req.getMaterial(), 0);
                    lines.add(Component.text(req.getMaterial().name(), NamedTextColor.GOLD)
                            .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                            .append(Component.text(placed, placed >= req.getAmount() ? NamedTextColor.GREEN : NamedTextColor.RED))
                            .append(Component.text("/" + req.getAmount(), NamedTextColor.DARK_GRAY))
                            .append(placed >= req.getAmount() ? Component.text(" ✔", NamedTextColor.GREEN) : Component.text(" ✘", NamedTextColor.RED)));
                }
            }
            lines.addAll(getConfiguredEffectStatusLines());

            progressHolo.text(joinHologramLines(lines));
            return;
        }
        if (problemMessage != null && hasTicket) {
            lines.add(Component.translatable("factions.building.status.problem").color(NamedTextColor.DARK_RED));
            lines.add(Component.text(problemMessage, NamedTextColor.RED));
            progressHolo.text(joinHologramLines(lines));
            return;
        }
        if (requiresInputChest) {
            lines.add(Component.translatable("factions.building.hologram.inputBuffer",
                    Component.text(countItems(inputBuffer), NamedTextColor.YELLOW),
                    Component.text(inputBufferSlots * 64, NamedTextColor.GRAY)));
        }
        if (requiresOutputChest) {
            lines.add(Component.translatable("factions.building.hologram.outputBuffer",
                    Component.text(countItems(outputBuffer), NamedTextColor.YELLOW),
                    Component.text(outputBufferSlots * 64, NamedTextColor.GRAY)));
        }
        if (upgradeInProgress) {
            for (BlockRequirement req : getUpgradeRequirements()) {
                lines.add(getRequirementProgressLine(req, true));
            }
        }
        boolean hasRuntimeStatus = false;
        for (BuildingEffect effect : buildingEffects) {
            for (Component line : effect.getStatusLines()) {
                lines.add(line);
                hasRuntimeStatus = true;
            }
        }
        if (!hasRuntimeStatus) {
            lines.addAll(getConfiguredEffectStatusLines());
        }
        progressHolo.text(joinHologramLines(lines));
    }

    private @NotNull Component joinHologramLines(@NotNull List<Component> lines) {
        Component content = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                content = content.append(HOLOGRAM_LINE_BREAK);
            }
            content = content.append(lines.get(i));
        }
        return content;
    }

    private void ensureHologramInteraction() {
        Location location = progressHolo.getLocation();
        Entity entity = hologramInteractionUUID == null ? null : Bukkit.getEntity(hologramInteractionUUID);
        if (entity instanceof Interaction interaction && !interaction.isDead()) {
            hologramInteraction = interaction;
            if (interaction.getLocation().distanceSquared(location) > 0.05) {
                interaction.teleport(location);
            }
        } else {
            hologramInteraction = location.getWorld().spawn(location, Interaction.class);
            hologramInteractionUUID = hologramInteraction.getUniqueId();
        }
        hologramInteraction.setInteractionWidth(2.2f);
        hologramInteraction.setInteractionHeight(2.4f);
        hologramInteraction.setResponsive(true);
        hologramInteraction.setPersistent(true);
    }

    public boolean isHologramInteraction(@NotNull Entity entity) {
        return hologramInteractionUUID != null && entity.getUniqueId().equals(hologramInteractionUUID);
    }

    public boolean isHologramInteractionId(@NotNull UUID entityId) {
        return hologramInteractionUUID != null && entityId.equals(hologramInteractionUUID);
    }

    public void handleHologramRightClick(@NotNull Player player) {
        BuildingDialogs.showBuildSiteDetails(player, this);
    }

    public void handleHologramLeftClick(@NotNull Player player) {
        showOutline(player, 10);
    }

    public @NotNull List<Component> getConfiguredEffectStatusLines() {
        List<Component> lines = new ArrayList<>();
        for (BuildingEffectData effectData : building.getEffects()) {
            if (!effectData.getId().equals("BlockDependentResourceProduction")) {
                continue;
            }
            lines.add(Component.translatable("factions.building.hologram.efficiency",
                    Component.text(getBlockDependentEfficiency(effectData))));
        }
        return lines;
    }

    public @NotNull List<Component> getConfiguredEffectDetailLines() {
        List<Component> lines = new ArrayList<>();
        for (BuildingEffectData effectData : building.getEffects()) {
            if (!effectData.getId().equals("BlockDependentResourceProduction")) {
                continue;
            }
            lines.add(Component.text("- ", NamedTextColor.GRAY).append(effectData.getDisplayName()));
            lines.add(Component.text("  ").append(Component.translatable("factions.building.effect.block_efficiency",
                    Component.text(getBlockDependentEfficiency(effectData)))));
        }
        return lines;
    }

    private int getBlockDependentEfficiency(@NotNull BuildingEffectData effectData) {
        int maximumCountedBlocks = effectData.getInt("maximumCountedBlocks", 200);
        if (maximumCountedBlocks <= 0) {
            return 100;
        }
        ConfigurationSection section = effectData.getConfigurationSection("blockModifiers");
        if (section == null) {
            return 0;
        }
        int blockCount = 0;
        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            if (material == null) {
                continue;
            }
            blockCount += getBlockCount(material);
            if (blockCount >= maximumCountedBlocks) {
                return 100;
            }
        }
        return Math.min(100, (int) Math.round((blockCount * 100.0) / maximumCountedBlocks));
    }

    public void showOutline(@NotNull Player player, int seconds) {
        new BukkitRunnable() {
            private int ticks = 0;
            private final int maxTicks = Math.max(1, seconds) * 20;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= maxTicks) {
                    cancel();
                    return;
                }
                spawnOutlineParticles(player);
                ticks += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void spawnOutlineParticles(@NotNull Player player) {
        World world = corner.getWorld();
        double minX = Math.min(corner.getX(), otherCorner.getX());
        double minY = Math.min(corner.getY(), otherCorner.getY());
        double minZ = Math.min(corner.getZ(), otherCorner.getZ());
        double maxX = Math.max(corner.getX(), otherCorner.getX());
        double maxY = Math.max(corner.getY(), otherCorner.getY());
        double maxZ = Math.max(corner.getZ(), otherCorner.getZ());
        Particle.DustOptions dust = new Particle.DustOptions(Color.AQUA, 1.2f);
        for (double x = minX; x <= maxX; x += 1) {
            for (double y = minY; y <= maxY; y += 1) {
                for (double z = minZ; z <= maxZ; z += 1) {
                    int edges = 0;
                    if (x == minX || x == maxX) {
                        edges++;
                    }
                    if (y == minY || y == maxY) {
                        edges++;
                    }
                    if (z == minZ || z == maxZ) {
                        edges++;
                    }
                    if (edges >= 2) {
                        player.spawnParticle(Particle.DUST, new Location(world, x, y, z), 1, dust);
                    }
                }
            }
        }
    }


    public void finishBuilding() {
        this.buildingEffects.clear();
        FLogger.BUILDING.log("Finishing building " + building.getId() + " for faction " + (getFaction() != null ? getFaction().getName() : "N/A") + " in region " + region.getName() + ". Instantiating effects.");

        if (getFaction() == null) {
            FLogger.BUILDING.log("Cannot finish building " + building.getId() + ": Faction owner is null for region " + region.getName());
            this.problemMessage = "Cannot activate: No owning faction. Disbanded?";
            this.hasTicket = true;
            this.state = BuildSiteState.DENIED;
            updateHolo();
            return;
        }

        if (building != null && building.getEffects() != null) {
            for (BuildingEffectData effectData : building.getEffects()) {
                BuildingEffect newEffectInstance = effectData.newEffect(this);
                if (newEffectInstance != null) {
                    this.buildingEffects.add(newEffectInstance);
                    for (BuildingContainerType type : newEffectInstance.getRequiredContainers()) {
                        requireContainer(type);
                    }
                    FLogger.BUILDING.log("Instantiated and added effect " + newEffectInstance.getClass().getSimpleName() + " for finished building " + building.getId());
                } else {
                    FLogger.BUILDING.log("Failed to instantiate effect from data: " + effectData.getId() + " for finished building " + building.getId());
                }
            }
        }  else {
            FLogger.BUILDING.log("Building or building effects list is null while finishing build site " + uuid + ". No effects instantiated.");
        }

        finished = true;
        state = BuildSiteState.ACTIVE;
        problemMessage = null;
        hasTicket = false;

        setActive(true); // Activate the newly added effects

        getRegion().getOwner().sendTranslatable("factions.building.status.accepted", Component.text(getBuilding().getId()), Component.text(getRegion().getName()));
        updateHolo();
    }

    public void removeEffects() {
        Faction owner = getRegion().getOwner();
        if (owner == null) {
            return;
        }
        Set<BuildingEffect> toRemove = new HashSet<>();
        for (BuildingEffect effect : owner.getBuildingEffects()) {
            if (effect.getSite() == this) {
                effect.remove();
                toRemove.add(effect);
            }
        }
        owner.getBuildingEffects().removeAll(toRemove);
        owner.getTickingBuildingEffects().removeAll(toRemove);
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
        if (isBusy) {
            return;
        }
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
        for (BlockRequirement req : building.getBlockRequirements()) {
            if (req.isTagRequirement()) {
                if (placedBlocksByTag.getOrDefault(req.getTag(), 0) < req.getAmount()) {
                    damaged = true;
                }
            }
            if (req.isMaterialRequirement()) {
                if (placedBlocks.getOrDefault(req.getMaterial(), 0) < req.getAmount()) {
                    damaged = true;
                }
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
                for (BlockRequirement req : building.getBlockRequirements()) {
                    if (req.isTagRequirement()) {
                        if (placedBlocksByTag.getOrDefault(req.getTag(), 0) < req.getAmount()) {
                            fini = false;
                        }
                    }
                    if (req.isMaterialRequirement()) {
                        if (placedBlocks.getOrDefault(req.getMaterial(), 0) < req.getAmount()) {
                            fini = false;
                        }
                    }
                }

                if (finished && !fini) {
                    finished = false;
                    active = false;
                    state = BuildSiteState.DAMAGED;
                    upgradeReady = false;
                    upgradeInProgress = false;
                    getRegion().getOwner().sendTranslatable("factions.building.status.destroyed", Component.text(getBuilding().getId()), Component.text(getRegion().getName()));
                    removeEffects();
                    return;
                }
                if (finished && upgradeInProgress && isUpgradeComplete()) {
                    completeUpgrade();
                    return;
                }
                if (fini && !isFinished() && !hasTicket) {
                    buildingManager.getBuildingTickets().add(getSite());
                    hasTicket = true;
                    state = BuildSiteState.READY_FOR_REVIEW;
                    getRegion().getOwner().sendTranslatable("factions.building.status.completed.info", Component.text(getBuilding().getId()), Component.text(getRegion().getName()));
                    getRegion().getOwner().sendTranslatable("factions.building.status.completed.ticketHint");
                    FLogger.BUILDING.log("A new BuildSite ticket for " + getBuilding().getId() + " in " + getRegion().getName() + " was created.");
                    for (Player player : interactive.getNearbyPlayers(16)) {
                        FTutorial.showHint(player, "building.first_site_completed");
                    }
                }
            }
        };

        BukkitRunnable scanBlocks = new BukkitRunnable() {
            @Override
            public void run() {
                Set<Block> blocks;
                Map<Material, Integer> placedByBlock = new HashMap<>();
                Map<FSetTag, Integer> placedByTag = new HashMap<>();
                Map<Material, Integer> upgradePlacedByBlock = new HashMap<>();
                Map<FSetTag, Integer> upgradePlacedByTag = new HashMap<>();
                Map<Material, Set<BuildSiteCoordinate>> foundBlocksOfInterest = new HashMap<>();
                blocks = getBlocks(corner.getWorld());
                for (Block block : blocks) {
                    Material type = block.getType();
                    for (BlockRequirement req : building.getBlockRequirements()) {
                        if (req.isTagRequirement() && req.matches(type)) {
                            FSetTag tag = req.getTag();
                            int amount = placedByTag.getOrDefault(tag, 0);
                            placedByTag.put(tag, amount + 1);
                        }
                        if (req.isMaterialRequirement() && req.matches(type)) {
                            int amount = placedByBlock.getOrDefault(type, 0);
                            placedByBlock.put(type, amount + 1);
                        }
                    }
                    for (BlockRequirement req : getUpgradeRequirements()) {
                        if (req.isTagRequirement() && req.matches(type)) {
                            FSetTag tag = req.getTag();
                            upgradePlacedByTag.put(tag, upgradePlacedByTag.getOrDefault(tag, 0) + 1);
                        }
                        if (req.isMaterialRequirement() && req.matches(type)) {
                            upgradePlacedByBlock.put(type, upgradePlacedByBlock.getOrDefault(type, 0) + 1);
                        }
                    }

                    if (building.getBlocksOfInterest().contains(type)) {
                        foundBlocksOfInterest.computeIfAbsent(type, ignored -> new HashSet<>())
                            .add(new BuildSiteCoordinate(block.getX(), block.getY(), block.getZ()));
                    }
                }

                placedBlocks = placedByBlock;
                placedBlocksByTag = placedByTag;
                upgradePlacedBlocks = upgradePlacedByBlock;
                upgradePlacedBlocksByTag = upgradePlacedByTag;
                blocksOfInterest.clear();
                blocksOfInterest.putAll(foundBlocksOfInterest);
                complete.run();
            }
        };

        scanBlocks.runTask(plugin);
    }

    public @NotNull Component getProgressComponentForTag(@NotNull FSetTag tag, int requiredAmount) {
        int placed = placedBlocksByTag.getOrDefault(tag, 0);
        if (placed >= requiredAmount) {
            return Component.text(placed, NamedTextColor.GREEN)
                    .append(Component.text("/" + requiredAmount, NamedTextColor.DARK_GRAY)
                    .append(Component.text(" ✔", NamedTextColor.GREEN)));
        }
        return Component.text(placed, NamedTextColor.RED)
                .append(Component.text("/" + requiredAmount, NamedTextColor.DARK_GRAY)
                .append(Component.text(" ✘", NamedTextColor.RED)));
    }

    public @NotNull Component getRequirementProgressLine(@NotNull BlockRequirement req, boolean upgrade) {
        int placed = getPlacedAmount(req, upgrade);
        return Component.text(req.getDisplayName(), NamedTextColor.GOLD)
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(Component.text(placed, placed >= req.getAmount() ? NamedTextColor.GREEN : NamedTextColor.RED))
                .append(Component.text("/" + req.getAmount(), NamedTextColor.DARK_GRAY))
                .append(placed >= req.getAmount() ? Component.text(" ✔", NamedTextColor.GREEN) : Component.text(" ✘", NamedTextColor.RED));
    }

    public int getPlacedAmount(@NotNull BlockRequirement req, boolean upgrade) {
        if (req.isTagRequirement()) {
            return (upgrade ? upgradePlacedBlocksByTag : placedBlocksByTag).getOrDefault(req.getTag(), 0);
        }
        if (req.isMaterialRequirement()) {
            return (upgrade ? upgradePlacedBlocks : placedBlocks).getOrDefault(req.getMaterial(), 0);
        }
        return 0;
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
        inventory = Bukkit.createInventory(this, 54, Component.translatable("factions.building.storage.title"));
        for (ItemStack item : outputBuffer) {
            inventory.addItem(item);
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        return inventory;
    }

    public boolean addItemToStorage(@NotNull ItemStack item) {
        return addOutputItem(item);
    }

    public boolean addOutputItem(@NotNull ItemStack item) {
        boolean added = addItemToBuffer(outputBuffer, item, outputBufferSlots);
        if (added) {
            pushOutputBufferToChest();
            updateHolo();
        }
        return added;
    }

    public boolean addInputItem(@NotNull ItemStack item) {
        boolean added = addItemToBuffer(inputBuffer, item, inputBufferSlots);
        if (added) {
            updateHolo();
        }
        return added;
    }

    public @Nullable ItemStack takeFirstInputMatching(@NotNull Predicate<ItemStack> matcher, int amount) {
        if (amount <= 0) {
            return null;
        }
        for (int i = 0; i < inputBuffer.size(); i++) {
            ItemStack stack = inputBuffer.get(i);
            if (!matcher.test(stack) || stack.getAmount() < amount) {
                continue;
            }
            ItemStack result = stack.clone();
            result.setAmount(amount);
            stack.setAmount(stack.getAmount() - amount);
            if (stack.getAmount() <= 0) {
                inputBuffer.remove(i);
            }
            updateHolo();
            return result;
        }
        return null;
    }

    public boolean isOutputBufferFull() {
        return outputBuffer.size() >= outputBufferSlots && outputBuffer.stream().allMatch(stack -> stack.getAmount() >= stack.getMaxStackSize());
    }

    public boolean isInputBufferFull() {
        return inputBuffer.size() >= inputBufferSlots && inputBuffer.stream().allMatch(stack -> stack.getAmount() >= stack.getMaxStackSize());
    }

    public void requireContainer(@NotNull BuildingContainerType type) {
        if (type == BuildingContainerType.INPUT) {
            requiresInputChest = true;
        } else if (type == BuildingContainerType.OUTPUT) {
            requiresOutputChest = true;
        }
    }

    public boolean isContainerBlock(@NotNull Block block) {
        Location location = block.getLocation();
        return isSameBlock(location, inputChestLocation) || isSameBlock(location, outputChestLocation)
                || isNamedContainerLocation(location);
    }

    public void syncLoadedContainers() {
        syncInputChestToBuffer();
        pushOutputBufferToChest();
    }

    private boolean addItemToBuffer(@NotNull List<ItemStack> buffer, @NotNull ItemStack source, int maxSlots) {
        if (source.getType() == Material.AIR || source.getAmount() <= 0) {
            return true;
        }
        ItemStack remaining = source.clone();
        for (ItemStack stack : buffer) {
            if (!stack.isSimilar(remaining) || stack.getAmount() >= stack.getMaxStackSize()) {
                continue;
            }
            int moved = Math.min(remaining.getAmount(), stack.getMaxStackSize() - stack.getAmount());
            stack.setAmount(stack.getAmount() + moved);
            remaining.setAmount(remaining.getAmount() - moved);
            if (remaining.getAmount() <= 0) {
                return true;
            }
        }
        while (remaining.getAmount() > 0 && buffer.size() < maxSlots) {
            ItemStack next = remaining.clone();
            int moved = Math.min(remaining.getAmount(), remaining.getMaxStackSize());
            next.setAmount(moved);
            buffer.add(next);
            remaining.setAmount(remaining.getAmount() - moved);
        }
        return remaining.getAmount() <= 0;
    }

    private int countItems(@NotNull List<ItemStack> buffer) {
        int count = 0;
        for (ItemStack stack : buffer) {
            count += stack.getAmount();
        }
        return count;
    }

    private boolean isSameBlock(@Nullable Location first, @Nullable Location second) {
        if (first == null || second == null || first.getWorld() == null || second.getWorld() == null) {
            return false;
        }
        return first.getWorld().equals(second.getWorld())
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }

    private boolean isNamedContainerLocation(@NotNull Location location) {
        for (BuildingContainerType type : BuildingContainerType.values()) {
            Position position = namedPositions.get(type.positionKey());
            if (position != null && isSameBlock(location, position.toLocation(getWorld()))) {
                return true;
            }
        }
        return false;
    }

    private boolean isLoaded(@Nullable Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        return location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    private @Nullable Chest getLoadedChest(@Nullable Location location) {
        if (!isLoaded(location)) {
            return null;
        }
        Material type = location.getBlock().getType();
        if (type != Material.CHEST && type != Material.TRAPPED_CHEST) {
            return null;
        }
        BlockState state = location.getBlock().getState();
        return state instanceof Chest chest ? chest : null;
    }

    private void ensureLoadedContainer(@Nullable Location location, @NotNull Material material, @NotNull Component name) {
        if (!isLoaded(location)) {
            return;
        }
        Material current = location.getBlock().getType();
        if (current != Material.CHEST && current != Material.TRAPPED_CHEST) {
            location.getBlock().setType(material);
        }
        Chest chest = getLoadedChest(location);
        if (chest != null) {
            chest.customName(name);
            chest.update();
        }
    }

    private void syncInputChestToBuffer() {
        Chest inputChest = getLoadedChest(inputChestLocation);
        if (inputChest == null) {
            return;
        }
        Inventory inv = inputChest.getInventory();
        boolean changed = false;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            if (!addItemToBuffer(inputBuffer, item, inputBufferSlots)) {
                continue;
            }
            inv.setItem(i, null);
            changed = true;
        }
        if (changed) {
            updateHolo();
        }
    }

    private void pushOutputBufferToChest() {
        Chest outputChest = getLoadedChest(outputChestLocation);
        if (outputChest == null || outputBuffer.isEmpty()) {
            return;
        }
        Inventory inv = outputChest.getInventory();
        boolean changed = false;
        for (int i = 0; i < outputBuffer.size(); i++) {
            ItemStack stack = outputBuffer.get(i);
            Map<Integer, ItemStack> leftover = inv.addItem(stack.clone());
            if (leftover.isEmpty()) {
                outputBuffer.remove(i--);
                changed = true;
                continue;
            }
            ItemStack remaining = leftover.values().iterator().next();
            if (remaining.getAmount() != stack.getAmount()) {
                stack.setAmount(remaining.getAmount());
                changed = true;
            }
            break;
        }
        if (changed) {
            updateHolo();
        }
    }

    public int getProgressPercent() {
        int required = 0;
        int placed = 0;
        for (BlockRequirement req : building.getBlockRequirements()) {
            required += req.getAmount();
            if (req.isTagRequirement()) {
                placed += Math.min(req.getAmount(), placedBlocksByTag.getOrDefault(req.getTag(), 0));
            } else if (req.isMaterialRequirement()) {
                placed += Math.min(req.getAmount(), placedBlocks.getOrDefault(req.getMaterial(), 0));
            }
        }
        return required == 0 ? 100 : Math.min(100, (int) Math.round((placed * 100.0) / required));
    }

    public int getUpgradeProgressPercent() {
        int required = 0;
        int placed = 0;
        for (BlockRequirement req : getUpgradeRequirements()) {
            required += req.getAmount();
            placed += Math.min(req.getAmount(), getPlacedAmount(req, true));
        }
        return required == 0 ? 100 : Math.min(100, (int) Math.round((placed * 100.0) / required));
    }

    public boolean isUpgradeComplete() {
        for (BlockRequirement req : getUpgradeRequirements()) {
            if (getPlacedAmount(req, true) < req.getAmount()) {
                return false;
            }
        }
        return getUpgradeConfig() != null;
    }

    public @NotNull List<BlockRequirement> getUpgradeRequirements() {
        BuildingUpgradeConfig config = getUpgradeConfig();
        return config == null ? List.of() : config.requiredBlocks();
    }

    public @Nullable BuildingUpgradeConfig getUpgradeConfig() {
        return building.getUpgradeConfig();
    }

    public @Nullable AddHousing getHousingEffect() {
        for (BuildingEffect effect : buildingEffects) {
            if (effect instanceof AddHousing housing) {
                return housing;
            }
        }
        for (BuildingEffectData data : building.getEffects()) {
            if (data.getId().equals("AddHousing")) {
                try {
                    return new AddHousing(data, this);
                } catch (Exception ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    public @Nullable AddHousing getConfiguredHousingEffect(@NotNull Building source) {
        for (BuildingEffectData data : source.getEffects()) {
            if (data.getId().equals("AddHousing")) {
                try {
                    return new AddHousing(data, this);
                } catch (Exception ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    public @NotNull Component getUpgradeTargetName() {
        String targetId = upgradeTargetBuilding;
        BuildingUpgradeConfig config = getUpgradeConfig();
        if ((targetId == null || targetId.isBlank()) && config != null) {
            targetId = config.targetBuildingId();
        }
        if (targetId == null || targetId.isBlank()) {
            return Component.text("-");
        }
        return Component.translatable("factions.building.buildings." + targetId + ".name");
    }

    public boolean canBeginUpgrade() {
        return active && finished && upgradeReady && !upgradeInProgress && getUpgradeConfig() != null;
    }

    public @NotNull List<Component> getUpgradeReadinessBlockerLines() {
        List<Component> blockers = new ArrayList<>();
        Faction owner = getFaction();
        BuildingUpgradeConfig config = getUpgradeConfig();
        if (owner == null || config == null) {
            return blockers;
        }
        Building target = buildingManager.getById(config.targetBuildingId());
        AddHousing targetHousing = target == null ? null : getConfiguredHousingEffect(target);
        if (target == null || targetHousing == null) {
            blockers.add(Component.translatable("factions.building.dialog.site.upgrade_blocker_target",
                    "factions.building.dialog.site.upgrade_blocker_target"));
            return blockers;
        }
        PopulationLevel targetLevel = targetHousing.getLevel();
        blockers.addAll(PopulationRequirementLines.missingTargetLevelRequirements(owner, targetLevel));
        return blockers;
    }

    public boolean beginUpgrade(@NotNull Player player) {
        if (!canBeginUpgrade()) {
            return false;
        }
        BuildingUpgradeConfig config = getUpgradeConfig();
        upgradeReady = false;
        upgradeInProgress = true;
        upgradeTargetBuilding = config.targetBuildingId();
        upgradePlacedBlocks.clear();
        upgradePlacedBlocksByTag.clear();
        scheduleProgressUpdate();
        updateHolo();
        player.sendMessage(Component.translatable("factions.building.upgrade.started", getUpgradeTargetName()));
        saveQuietly();
        return true;
    }

    public void markUpgradeSatisfiedPayday() {
        if (upgradeReady || upgradeInProgress || getUpgradeConfig() == null) {
            return;
        }
        upgradeSatisfiedPaydays++;
        updateHolo();
        saveQuietly();
    }

    public void resetUpgradeSatisfiedPaydays() {
        if (upgradeSatisfiedPaydays == 0 || upgradeReady || upgradeInProgress) {
            return;
        }
        upgradeSatisfiedPaydays = 0;
        updateHolo();
        saveQuietly();
    }

    public void markUpgradeReady() {
        if (upgradeInProgress || getUpgradeConfig() == null) {
            return;
        }
        upgradeReady = true;
        upgradeTargetBuilding = getUpgradeConfig().targetBuildingId();
        updateHolo();
        saveQuietly();
    }

    private void completeUpgrade() {
        BuildingUpgradeConfig upgradeConfig = getUpgradeConfig();
        if (upgradeConfig == null) {
            return;
        }
        Building oldBuilding = building;
        Building target = buildingManager.getById(upgradeConfig.targetBuildingId());
        if (target == null) {
            FLogger.ERROR.log("Cannot complete housing upgrade for " + uuid + ": target building " + upgradeConfig.targetBuildingId() + " not found.");
            return;
        }
        AddHousing oldHousing = getConfiguredHousingEffect(oldBuilding);
        AddHousing targetHousing = getConfiguredHousingEffect(target);
        removeEffects();
        buildingEffects.clear();
        building = target;
        upgradeReady = false;
        upgradeInProgress = false;
        upgradeTargetBuilding = null;
        upgradeSatisfiedPaydays = 0;
        upgradePlacedBlocks.clear();
        upgradePlacedBlocksByTag.clear();
        instantiateEffects();
        setActive(true);
        transferHousingPopulation(oldHousing, targetHousing);
        Faction owner = getFaction();
        if (owner != null) {
            owner.sendTranslatable("factions.building.upgrade.completed",
                    Component.translatable("factions.building.buildings." + oldBuilding.getId() + ".name"),
                    Component.translatable("factions.building.buildings." + target.getId() + ".name"));
            owner.saveData();
        }
        updateHolo();
        saveQuietly();
    }

    private void transferHousingPopulation(@Nullable AddHousing oldHousing, @Nullable AddHousing targetHousing) {
        Faction owner = getFaction();
        if (owner == null || oldHousing == null || targetHousing == null) {
            return;
        }
        PopulationLevel sourceLevel = oldHousing.getLevel();
        PopulationLevel targetLevel = targetHousing.getLevel();
        int amount = Math.min(Math.min(oldHousing.getAmount(), targetHousing.getAmount()), owner.getPopulation(sourceLevel));
        if (amount <= 0 || sourceLevel == targetLevel) {
            return;
        }
        owner.getPopulation().put(sourceLevel, Math.max(0, owner.getPopulation(sourceLevel) - amount));
        owner.addPopulation(targetLevel, amount);
    }

    private void instantiateEffects() {
        if (building == null || building.getEffects() == null) {
            return;
        }
        for (BuildingEffectData effectData : building.getEffects()) {
            BuildingEffect newEffectInstance = effectData.newEffect(this);
            if (newEffectInstance == null) {
                continue;
            }
            buildingEffects.add(newEffectInstance);
            for (BuildingContainerType type : newEffectInstance.getRequiredContainers()) {
                requireContainer(type);
            }
        }
    }

    private void saveQuietly() {
        try {
            save();
        } catch (IOException e) {
            FLogger.ERROR.log("Failed to save build site " + uuid + ": " + e.getMessage());
        }
    }

    public int getInputBufferItemCount() {
        return countItems(inputBuffer);
    }

    public int getOutputBufferItemCount() {
        return countItems(outputBuffer);
    }

    public int getInputBufferCapacityItems() {
        return inputBufferSlots * 64;
    }

    public int getOutputBufferCapacityItems() {
        return outputBufferSlots * 64;
    }

    public boolean requiresInputChest() {
        return requiresInputChest;
    }

    public boolean requiresOutputChest() {
        return requiresOutputChest;
    }

    public @Nullable String getProblemMessage() {
        return problemMessage;
    }

    private NamedTextColor getStateColor() {
        return switch (state) {
            case ACTIVE -> NamedTextColor.GREEN;
            case READY_FOR_REVIEW -> NamedTextColor.YELLOW;
            case DENIED, DAMAGED -> NamedTextColor.RED;
            case PLACED -> NamedTextColor.GRAY;
        };
    }

    private BuildSiteState parseState(@Nullable String value, boolean finished, boolean hasTicket, @Nullable String problemMessage) {
        if (value != null) {
            try {
                return BuildSiteState.valueOf(value);
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (finished) {
            return BuildSiteState.ACTIVE;
        }
        if (problemMessage != null && hasTicket) {
            return BuildSiteState.DENIED;
        }
        if (hasTicket) {
            return BuildSiteState.READY_FOR_REVIEW;
        }
        return BuildSiteState.PLACED;
    }

    //
    // Event handlers
    //

    @EventHandler
    private void onInteract(PlayerInteractEvent event) {
        handleContainerInteract(event);
    }

    public void handleContainerInteract(@NotNull PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || !isContainerBlock(event.getClickedBlock())) {
            return;
        }
        if (event.getClickedBlock().getLocation().equals(inputChestLocation)) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            syncInputChestToBuffer();
            return;
        }
        if (event.getClickedBlock().getLocation().equals(outputChestLocation)) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            pushOutputBufferToChest();
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
            if (event.getInventory().getHolder(false) instanceof Chest chest && isContainerBlock(chest.getBlock())) {
                if (isSameBlock(chest.getLocation(), inputChestLocation)) {
                    syncInputChestToBuffer();
                }
                HandlerList.unregisterAll(this);
            }
            return;
        }
        buildingStorage.clear();
        outputBuffer.clear();
        for (ItemStack item : inventory.getContents()) {
            if (item == null) {
                continue;
            }
            outputBuffer.add(item);
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

    public @NotNull ClaimableRegion getRegion() {
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

    public Location getCenter() {
        return new Location(corner.getWorld(),
                (corner.getX() + otherCorner.getX()) / 2,
                (corner.getY() + otherCorner.getY()) / 2,
                (corner.getZ() + otherCorner.getZ()) / 2);
    }

    public @NotNull Set<BuildSiteSection> getSections() {
        return sections;
    }

    public @NotNull Map<String, Position> getNamedPositions() {
        return namedPositions;
    }

    public int getBlockCount(Material material) {
        Set<BuildSiteCoordinate> coordinates = blocksOfInterest.get(material);
        return coordinates != null ? coordinates.size() : placedBlocks.getOrDefault(material, 0);
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
        return state == BuildSiteState.ACTIVE || (finished && state != BuildSiteState.DAMAGED);
    }

    public void setProblemMessage(@NotNull String msg) {
        problemMessage = msg;
        state = BuildSiteState.DENIED;
        hasTicket = true;
        updateHolo();
    }

    public void setRequiresInputChest(boolean requiresInputChest) {
        this.requiresInputChest = requiresInputChest;
        if (requiresInputChest) {
            requireContainer(BuildingContainerType.INPUT);
        }
    }

    public void setRequiresOutputChest(boolean requiresOutputChest) {
        this.requiresOutputChest = requiresOutputChest;
        if (requiresOutputChest) {
            requireContainer(BuildingContainerType.OUTPUT);
        }
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
        return new HashSet<>(outputBuffer);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public Set<ItemStack> getInputItems() {
        inputItems.clear();
        inputItems.addAll(inputBuffer);
        return inputItems;
    }

    public Set<ItemStack> getOutputItems() {
        outputItems.clear();
        outputItems.addAll(outputBuffer);
        return outputItems;
    }

    public Set<BuildingEffect> getEffects() {
        return buildingEffects;
    }

    public Set<Position> getChestLocations() {
        chestPositions.clear();
        if (inputChestLocation != null) {
            chestPositions.add(inputChestLocation);
        }
        if (outputChestLocation != null) {
            chestPositions.add(outputChestLocation);
        }
        return chestPositions;
    }

    public @NotNull BuildSiteState getState() {
        return state;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isUpgradeReady() {
        return upgradeReady;
    }

    public boolean isUpgradeInProgress() {
        return upgradeInProgress;
    }

    public int getUpgradeSatisfiedPaydays() {
        return upgradeSatisfiedPaydays;
    }

    public int getUpgradeRequiredSatisfiedPaydays() {
        BuildingUpgradeConfig config = getUpgradeConfig();
        return config == null ? 0 : config.requiredSatisfiedPaydays();
    }

    public @Nullable String getUpgradeTargetBuilding() {
        return upgradeTargetBuilding != null ? upgradeTargetBuilding : getUpgradeConfig() == null ? null : getUpgradeConfig().targetBuildingId();
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            if (state == BuildSiteState.ACTIVE) {
                state = BuildSiteState.DAMAGED;
            }
            for (BuildingEffect effect : buildingEffects) {
                effect.remove();
            }
        } else {
            state = BuildSiteState.ACTIVE;
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
        resourceInputsAvailable = true;
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

    public void setResourceInputsAvailable(boolean resourceInputsAvailable) {
        this.resourceInputsAvailable = resourceInputsAvailable;
    }

    public boolean hasResourceInputsAvailable() {
        return resourceInputsAvailable;
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
                getNamedPositions().put("input_chest", inputChestLocation);
            }
            if (getNamedPositions().containsKey("output_chest")) {
                outputChestLocation = getNamedPositions().get("output_chest").toLocation(getWorld());
            } else if (requiresOutputChest) {
                outputChestLocation = interactiveLocation.clone().add(0, 0, -1);
                getNamedPositions().put("output_chest", outputChestLocation);
            }
        }
        ensureLoadedContainer(inputChestLocation, Material.TRAPPED_CHEST, Component.translatable("factions.building.common.input_chest"));
        ensureLoadedContainer(outputChestLocation, Material.CHEST, Component.translatable("factions.building.common.output_chest"));
        syncLoadedContainers();
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
        hologramInteractionUUID = UUID.fromString(getString("hologramInteractionUUID", "00000000-0000-0000-0000-000000000000"));
        building = buildingManager.getById(getString("building"));
        finished = getBoolean("finished");
        hasTicket = getBoolean("hasTicket");
        problemMessage = getString("problemMessage");
        state = parseState(getString("state", null), finished, hasTicket, problemMessage);
        upgradeReady = getBoolean("upgradeReady", false);
        upgradeInProgress = getBoolean("upgradeInProgress", false);
        upgradeTargetBuilding = getString("upgradeTargetBuilding", null);
        upgradeSatisfiedPaydays = getInt("upgradeSatisfiedPaydays", 0);
        region = (ClaimableRegion) plugin.getRegionManager().getRegionById(getInt("region"));
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
                outputBuffer.add(ItemStack.deserializeBytes(Base64.getDecoder().decode(id)));
            }
        }
        if (contains("inputBuffer")) {
            for (String id : getStringList("inputBuffer")) {
                inputBuffer.add(ItemStack.deserializeBytes(Base64.getDecoder().decode(id)));
            }
        }
        if (contains("outputBuffer")) {
            outputBuffer.clear();
            for (String id : getStringList("outputBuffer")) {
                outputBuffer.add(ItemStack.deserializeBytes(Base64.getDecoder().decode(id)));
            }
        }
        if (contains("outputItems")) {
            for (String id : getStringList("outputItems")) {
                outputBuffer.add(ItemStack.deserializeBytes(Base64.getDecoder().decode(id)));
            }
        }
        if (contains("placedBlocks")) {
            for (String id : getConfigurationSection("placedBlocks").getKeys(false)) {
                placedBlocks.put(Material.valueOf(id), getInt("placedBlocks." + id));
            }
        }
        if (contains("placedBlocksByTag")) {
            for (String id : getConfigurationSection("placedBlocksByTag").getKeys(false)) {
                FSetTag tag = buildingManager.getTagManager().getTag(id.toUpperCase());
                if (tag != null) {
                    placedBlocksByTag.put(tag, getInt("placedBlocksByTag." + id));
                }
            }
        }
        if (contains("upgradePlacedBlocks")) {
            for (String id : getConfigurationSection("upgradePlacedBlocks").getKeys(false)) {
                upgradePlacedBlocks.put(Material.valueOf(id), getInt("upgradePlacedBlocks." + id));
            }
        }
        if (contains("upgradePlacedBlocksByTag")) {
            for (String id : getConfigurationSection("upgradePlacedBlocksByTag").getKeys(false)) {
                FSetTag tag = buildingManager.getTagManager().getTag(id.toUpperCase());
                if (tag != null) {
                    upgradePlacedBlocksByTag.put(tag, getInt("upgradePlacedBlocksByTag." + id));
                }
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
        if ((state == BuildSiteState.READY_FOR_REVIEW || state == BuildSiteState.DENIED) && !buildingManager.getBuildingTickets().contains(this)) {
            buildingManager.getBuildingTickets().add(this);
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
                        for (BuildingContainerType type : effect.getRequiredContainers()) {
                            requireContainer(type);
                        }
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
            updateHolo();
            if (hasBlockDependentEffects()) {
                scheduleProgressUpdate();
            }
        } else {
            FLogger.BUILDING.log("Build site " + uuid + " is not active (finished=" + finished + ", destroyed=" + isDestroyed() + "). Effects not activated by load().");
            scheduleProgressUpdate();
        }
        FLogger.BUILDING.log("Loaded build site " + uuid + " for " + building.getId() + " in " + region.getName());
    }

    private boolean hasBlockDependentEffects() {
        return building.getEffects().stream().anyMatch(effect -> effect.getId().equals("BlockDependentResourceProduction"));
    }

    public void save() throws IOException {
        File file = new File(Factions.BUILD_SITES, uuid + ".yml");
        set("progressHoloUUID", progressHoloUUID == null ? null : progressHoloUUID.toString());
        set("hologramInteractionUUID", hologramInteractionUUID == null ? null : hologramInteractionUUID.toString());
        set("building", building.getId());
        set("region", region.getId());
        set("location.corner", corner.serialize());
        set("location.otherCorner", otherCorner.serialize());
        set("location.interactable", interactive.serialize());
        set("finished", finished);
        set("state", state.name());
        set("hasTicket", hasTicket);
        set("problemMessage", problemMessage);
        set("upgradeReady", upgradeReady);
        set("upgradeInProgress", upgradeInProgress);
        set("upgradeTargetBuilding", upgradeTargetBuilding);
        set("upgradeSatisfiedPaydays", upgradeSatisfiedPaydays);
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
        for (ItemStack stack : outputBuffer) {
            items.add(java.util.Base64.getEncoder().encodeToString(stack.serializeAsBytes()));
        }
        set("buildingStorage", null);
        set("outputBuffer", items);
        List<String> input = new ArrayList<>();
        for (ItemStack stack : inputBuffer) {
            input.add(java.util.Base64.getEncoder().encodeToString(stack.serializeAsBytes()));
        }
        set("inputBuffer", input);
        for (Map.Entry<Material, Integer> entry : placedBlocks.entrySet()) {
            set("placedBlocks." + entry.getKey().name(), entry.getValue());
        }

        for (Map.Entry<FSetTag, Integer> entry : placedBlocksByTag.entrySet()) {
            set("placedBlocksByTag." + entry.getKey().getName(), entry.getValue());
        }
        set("upgradePlacedBlocks", null);
        for (Map.Entry<Material, Integer> entry : upgradePlacedBlocks.entrySet()) {
            set("upgradePlacedBlocks." + entry.getKey().name(), entry.getValue());
        }
        set("upgradePlacedBlocksByTag", null);
        for (Map.Entry<FSetTag, Integer> entry : upgradePlacedBlocksByTag.entrySet()) {
            set("upgradePlacedBlocksByTag." + entry.getKey().getName(), entry.getValue());
        }

        for (Material type : blocksOfInterest.keySet()) {
            List<String> coords = new ArrayList<>();
            for (BuildSiteCoordinate coordinate : blocksOfInterest.get(type)) {
                coords.add(coordinate.toString());
            }
            set("blocksOfInterest." + type.name(), coords);
        }
        for (Map.Entry<String, String> entry : additionalData.entrySet()) {
            set("additionalData." + entry.getKey(), entry.getValue());
        }
        set("outputItems", null);
        super.save(file);
    }
}


