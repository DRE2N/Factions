package de.erethon.factions.war.structure;

import de.erethon.factions.event.WarPhaseChangeEvent;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionStructure;
import de.erethon.factions.region.WarRegion;
import de.erethon.factions.region.schematic.FAWESchematicUtils;
import de.erethon.factions.region.schematic.SchematicSavable;
import de.erethon.factions.util.FLogger;
import io.papermc.paper.math.Position;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.List;

/**
 * An area that can only be modified, when warzones are closed.
 *
 * @author Fyreum
 */
@SuppressWarnings("UnstableApiUsage")
public class WarCastleStructure extends RegionStructure implements Listener, SchematicSavable {

    private static final Particle.DustOptions BUILD_PREVIEW_DUST = new Particle.DustOptions(org.bukkit.Color.LIME, 0.45f);
    private static final List<BlockFace> BUILD_PREVIEW_FACES = List.of(BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);

    private CrystalWarStructure crystalObjective;
    private BukkitRunnable repairTask;
    private BukkitRunnable placementPreviewTask;
    private int nextRepairSlice;
    private int nextRepairBlockInSlice;
    private BossBar repairBossBar;

    public WarCastleStructure(@NotNull WarRegion region, @NotNull ConfigurationSection config) {
        super(region, config);
    }

    public WarCastleStructure(@NotNull WarRegion region, @NotNull ConfigurationSection config, @NotNull Position a, @NotNull Position b) {
        super(region, config, a, b);
    }

    @Override
    public @NotNull TriState canBuild(@NotNull FPlayer fPlayer, @Nullable Block block) {
        if (plugin.getCurrentWarPhase() == de.erethon.factions.war.WarPhase.PEACE) {
            return canModifyDuringPeace(fPlayer, block);
        }
        if (plugin.getCurrentWarPhase().isAllowRuinBuilding()) {
            if (block == null || !fPlayer.hasFaction() || region.getRegionalWarTracker().getOperatingFaction() != fPlayer.getFaction()) {
                return TriState.FALSE;
            }
            if (block.getType().isSolid() && block.getRelative(0, -1, 0).getType().isAir()) {
                return TriState.FALSE;
            }
            return TriState.TRUE;
        }
        if (!plugin.getCurrentWarPhase().isAllowPvP()) {
            return TriState.FALSE;
        }
        return super.canBuild(fPlayer, block);
    }

    @Override
    public @NotNull TriState canPlace(@NotNull FPlayer fPlayer, @Nullable Block block) {
        if (plugin.getCurrentWarPhase() == de.erethon.factions.war.WarPhase.PEACE) {
            TriState modify = canModifyDuringPeace(fPlayer, block);
            if (modify != TriState.TRUE || block == null) {
                return modify;
            }
            if (requiresObsidianSupport(block) && !hasObsidianBelow(block)) {
                return TriState.FALSE;
            }
            return TriState.TRUE;
        }
        return canBuild(fPlayer, block);
    }

    private @NotNull TriState canModifyDuringPeace(@NotNull FPlayer fPlayer, @Nullable Block block) {
        if (block == null || !fPlayer.hasFaction() || region.getRegionalWarTracker().getOperatingFaction() != fPlayer.getFaction()) {
            return TriState.FALSE;
        }
        if (requiresObsidianSupport(block) && !hasObsidianBelow(block)) {
            return TriState.FALSE;
        }
        return TriState.TRUE;
    }

    private boolean requiresObsidianSupport(@NotNull Block block) {
        return block.getType().isSolid() && block.getType().getHardness() >= Material.STONE.getHardness();
    }

    private boolean hasObsidianBelow(@NotNull Block block) {
        for (int y = block.getY() - 1; y >= block.getWorld().getMinHeight(); y--) {
            if (block.getWorld().getBlockAt(block.getX(), y, block.getZ()).getType() == Material.OBSIDIAN) {
                return true;
            }
        }
        return false;
    }

    /* Listeners */

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getCurrentWarPhase().isAllowPvP()) {
            return;
        }
        if (!containsPosition(event.getBlock().getLocation())) {
            return;
        }
        event.setDropItems(false);
        event.setExpToDrop(0);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!plugin.getCurrentWarPhase().isAllowPvP()) {
            return;
        }
        if (!containsPosition(event.getBlock().getLocation())) {
            return;
        }
    }

    @EventHandler
    public void onWarPhaseChange(WarPhaseChangeEvent event) {
        if (event.getOldPhase().isAllowPvP() != event.getNewPhase().isAllowPvP()) {
            if (event.getOldPhase().isAllowPvP()) {
                // PvP: true -> false
                crystalObjective.deactivate();
            } else {
                // PvP: false -> true
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> FAWESchematicUtils.saveWarStructureToSchematic(this));
                crystalObjective.activate();
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!plugin.getCurrentWarPhase().isAllowPvP()) {
            return;
        }
    }

    /* Serialization */

    @Override
    protected void load(@NotNull ConfigurationSection config) {
        FLogger.DEBUG.log("Loading war castle '" + name + "' for region: " + region.getId() + "...");

        ConfigurationSection section = config.getConfigurationSection("crystalObjective");

        if (section == null) {
            section = config.createSection("crystalObjective");
        }
        if (!section.contains("minPosition") || !section.contains("maxPosition")) {
            // Initialize default crystal position
            int radius = 3;
            int centerX = xRange.getMinimumInteger() + xRange.getMaximumInteger() / 2,
                    centerZ = zRange.getMinimumInteger() + zRange.getMaximumInteger() / 2;

            section.set("minPosition", Map.of("x", centerX - radius, "y", yRange.getMinimumInteger(), "z", centerZ - radius));
            section.set("maxPosition", Map.of("x", centerX + radius, "y", yRange.getMinimumInteger() + radius, "z", centerZ + radius));
        }
        this.crystalObjective = new CrystalWarStructure(region, section);
        this.crystalObjective.setDefenderCrystal(true);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startPlacementPreviewTask();
    }

    private void startPlacementPreviewTask() {
        if (placementPreviewTask != null) {
            return;
        }
        placementPreviewTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getCurrentWarPhase() != de.erethon.factions.war.WarPhase.PEACE
                        || region.getRegionalWarTracker().getOperatingFaction() == null) {
                    return;
                }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    FPlayer fPlayer = plugin.getFPlayerCache().getByPlayerIfCached(player);
                    if (fPlayer == null || !fPlayer.hasFaction()
                            || fPlayer.getFaction() != region.getRegionalWarTracker().getOperatingFaction()
                            || !isHoldingBlock(player)
                            || player.getWorld() != region.getWorld()
                            || !containsPosition(player.getLocation())) {
                        continue;
                    }
                    showPlacementPreview(player);
                }
            }
        };
        placementPreviewTask.runTaskTimer(plugin, 20L, 20L);
    }

    private boolean isHoldingBlock(Player player) {
        return player.getInventory().getItemInMainHand().getType().isBlock()
                || player.getInventory().getItemInOffHand().getType().isBlock();
    }

    private void showPlacementPreview(Player player) {
        Location center = player.getLocation();
        int radius = 4;
        for (int x = center.getBlockX() - radius; x <= center.getBlockX() + radius; x++) {
            for (int z = center.getBlockZ() - radius; z <= center.getBlockZ() + radius; z++) {
                for (int y = center.getBlockY() - 1; y <= center.getBlockY() + 2; y++) {
                    showPlacementPreviewFaces(player, player.getWorld().getBlockAt(x, y, z));
                }
            }
        }
    }

    private void showPlacementPreviewFaces(Player player, Block support) {
        if (!support.getType().isSolid()) {
            return;
        }
        for (BlockFace face : BUILD_PREVIEW_FACES) {
            Block target = support.getRelative(face);
            if (!containsPosition(target.getLocation()) || !target.isPassable() || !hasObsidianBelow(target)) {
                continue;
            }
            player.spawnParticle(Particle.DUST, faceCenter(support, face), 1, 0, 0, 0, 0, BUILD_PREVIEW_DUST);
        }
    }

    private Location faceCenter(Block block, BlockFace face) {
        double x = block.getX() + 0.5 + face.getModX() * 0.51;
        double y = block.getY() + 0.5 + face.getModY() * 0.51;
        double z = block.getZ() + 0.5 + face.getModZ() * 0.51;
        return new Location(block.getWorld(), x, y, z);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> serialized = super.serialize();
        if (crystalObjective != null) {
            serialized.put("crystalObjective", crystalObjective.serialize());
        }
        return serialized;
    }

    /* Getters and setters */

    public @NotNull CrystalWarStructure getCrystalObjective() {
        return crystalObjective;
    }

    @Override
    public void onTemporaryOccupy(@NotNull Alliance alliance) {
        crystalObjective.onTemporaryOccupy(alliance);
    }

    @Override
    public String getSchematicID() {
        return getName() + "_" + getRegion().getId();
    }

    @Override
    public Location getOrigin() {
        return getCenterPosition().toLocation(getRegion().getWorld());
    }

    public void startRepair() {
        if (repairTask != null) {
            return;
        }
        nextRepairSlice = 0;
        nextRepairBlockInSlice = 0;
        repairBossBar = BossBar.bossBar(repairProgressMessage(), getRepairProgress(), BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
        Bukkit.getScheduler().runTask(plugin, this::updateRepairBossBar);
        repairTask = new BukkitRunnable() {
            @Override
            public void run() {
                int availableSupplies = region.getRegionalWarTracker().getRepairSupplies();
                if (availableSupplies <= 0) {
                    repairTask = null;
                    Bukkit.getScheduler().runTask(plugin, WarCastleStructure.this::hideRepairBossBar);
                    cancel();
                    return;
                }
                if (!hasFriendlyPlayerInCastle()) {
                    Bukkit.getScheduler().runTask(plugin, WarCastleStructure.this::updateRepairBossBar);
                    return;
                }
                FAWESchematicUtils.BlockPasteResult result = FAWESchematicUtils.pasteNextBlockInSlice(getSchematicID(), getOrigin(), nextRepairSlice, nextRepairBlockInSlice);
                nextRepairBlockInSlice = result.nextBlockIndex();
                if (result.changedBlock()) {
                    region.getRegionalWarTracker().consumeRepairSupplies(1);
                }
                if (result.completedSlice()) {
                    nextRepairSlice++;
                    nextRepairBlockInSlice = 0;
                }
                Bukkit.getScheduler().runTask(plugin, WarCastleStructure.this::updateRepairBossBar);
                if (nextRepairSlice >= getRepairSliceCount() || region.getRegionalWarTracker().getRepairSupplies() <= 0) {
                    nextRepairSlice = 0;
                    nextRepairBlockInSlice = 0;
                    repairTask = null;
                    Bukkit.getScheduler().runTask(plugin, WarCastleStructure.this::hideRepairBossBar);
                    cancel();
                }
            }
        };
        repairTask.runTaskTimerAsynchronously(plugin, 0, 1L);
    }

    public boolean isRepairing() {
        return repairTask != null;
    }

    public int getRepairSliceCount() {
        return getYRange().getMaximumInteger() - getYRange().getMinimumInteger() + 1;
    }

    public int getCurrentRepairY() {
        return Math.min(getYRange().getMinimumInteger() + nextRepairSlice, getYRange().getMaximumInteger());
    }

    public float getRepairProgress() {
        double progress = getRepairBlockCursor() / (double) getTotalRepairBlocks();
        return Math.min(1.0f, Math.max(0.0f, (float) progress));
    }

    private int getBlocksPerRepairSlice() {
        return (getXRange().getMaximumInteger() - getXRange().getMinimumInteger() + 1)
                * (getZRange().getMaximumInteger() - getZRange().getMinimumInteger() + 1);
    }

    private int getTotalRepairBlocks() {
        return Math.max(1, getBlocksPerRepairSlice() * getRepairSliceCount());
    }

    private int getRepairBlockCursor() {
        return Math.min(getTotalRepairBlocks(), nextRepairSlice * getBlocksPerRepairSlice() + nextRepairBlockInSlice);
    }

    private void updateRepairBossBar() {
        if (repairBossBar == null) {
            return;
        }
        repairBossBar.name(repairProgressMessage());
        repairBossBar.progress(getRepairProgress());
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (shouldSeeRepairBossBar(player)) {
                player.showBossBar(repairBossBar);
            } else {
                player.hideBossBar(repairBossBar);
            }
        }
    }

    private void hideRepairBossBar() {
        if (repairBossBar == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(repairBossBar);
        }
        repairBossBar = null;
    }

    private boolean shouldSeeRepairBossBar(Player player) {
        Region currentRegion = plugin.getRegionManager().getRegionByLocation(player.getLocation());
        if (currentRegion != region) {
            return false;
        }
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayerIfCached(player);
        return fPlayer != null && fPlayer.hasAlliance() && fPlayer.getAlliance() == region.getAlliance();
    }

    private boolean hasFriendlyPlayerInCastle() {
        if (region.getAlliance() == null) {
            return false;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() != region.getWorld() || !containsPosition(player.getLocation())) {
                continue;
            }
            FPlayer fPlayer = plugin.getFPlayerCache().getByPlayerIfCached(player);
            if (fPlayer != null && fPlayer.hasAlliance() && fPlayer.getAlliance() == region.getAlliance()) {
                return true;
            }
        }
        return false;
    }

    private net.kyori.adventure.text.Component repairProgressMessage() {
        return FMessage.WAR_OBJECTIVE_REPAIR_PROGRESS.message(
                region.getName(),
                String.format(java.util.Locale.ROOT, "%.2f", getRepairProgress() * 100.0),
                String.valueOf(getCurrentRepairY()),
                String.valueOf(region.getRegionalWarTracker().getRepairSupplies())
        );
    }
}
