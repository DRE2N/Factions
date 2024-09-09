package de.erethon.factions.war.structure;

import de.erethon.factions.event.WarPhaseChangeEvent;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionStructure;
import de.erethon.factions.region.schematic.RegionSchematic;
import de.erethon.factions.region.schematic.RestoreProcess;
import de.erethon.factions.region.schematic.RestoreProcessImpl;
import de.erethon.factions.util.FLogger;
import io.papermc.paper.math.Position;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * An area that can only be modified, when warzones are closed.
 *
 * @author Fyreum
 */
@SuppressWarnings("UnstableApiUsage")
public class WarCastleStructure extends RegionStructure implements Listener {

    protected RegionSchematic schematic;
    private RestoreProcess restoreProcess;
    private CrystalWarStructure crystalObjective;

    public WarCastleStructure(@NotNull Region region, @NotNull ConfigurationSection config) {
        super(region, config);
    }

    public WarCastleStructure(@NotNull Region region, @NotNull ConfigurationSection config, @NotNull Position a, @NotNull Position b) {
        super(region, config, a, b);
    }

    @Override
    public @NotNull TriState canBuild(@NotNull FPlayer fPlayer, @Nullable Block block) {
        if (!plugin.getCurrentWarPhase().isAllowPvP()) {
            return fPlayer.hasAlliance() && fPlayer.getAlliance() == region.getAlliance() ? TriState.TRUE : TriState.FALSE;
        }
        return super.canBuild(fPlayer, block);
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
        restoreProcess.addRemainingPosition(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!plugin.getCurrentWarPhase().isAllowPvP()) {
            return;
        }
        if (!containsPosition(event.getBlock().getLocation())) {
            return;
        }
        for (Block block : event.blockList()) {
            restoreProcess.addRemainingPosition(block);
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
                crystalObjective.activate();
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!plugin.getCurrentWarPhase().isAllowPvP()) {
            return;
        }
        restoreProcess.onChunkLoad(event.getChunk());
    }

    /* Serialization */

    @Override
    protected void load(@NotNull ConfigurationSection config) {
        String schematicId = config.getString("schematicId");
        if (schematicId != null) {
            this.schematic = plugin.getRegionSchematicManager().getSchematic(schematicId);
        }
        if (schematic == null) {
            FLogger.DEBUG.log("No schematic for war castle '" + name + "' found: " + schematicId);
            FLogger.DEBUG.log("Creating new schematic for war castle '" + name + "'...");
            schematic = plugin.getRegionSchematicManager().initializeSchematic(name);
            schematic.scanAndInsertBlocks(region.getWorld(), getMinPosition(), getMaxPosition());
        } else if (schematic.getBlocks() == null) {
            FLogger.DEBUG.log("Initial scan for schematic: " + schematicId);
            schematic.scanAndInsertBlocks(region.getWorld(), getMinPosition(), getMaxPosition());
        }
        this.restoreProcess = new RestoreProcessImpl(region.getWorld(), getMinPosition(), schematic);
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
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> serialized = super.serialize();
        if (schematic != null) {
            serialized.put("schematic", schematic.getName());
        }
        if (crystalObjective != null) {
            serialized.put("crystalObjective", crystalObjective.serialize());
        }
        return serialized;
    }

    /* Getters and setters */

    public @Nullable RegionSchematic getSchematic() {
        return schematic;
    }

    public void setSchematic(@Nullable RegionSchematic schematic) {
        this.schematic = schematic;
    }

    public @NotNull RestoreProcess getRestoreProcess() {
        return restoreProcess;
    }

    public void setRestoreProcess(@NotNull RestoreProcess restoreProcess) {
        this.restoreProcess = restoreProcess;
    }

    public @NotNull CrystalWarStructure getCrystalObjective() {
        return crystalObjective;
    }
}
