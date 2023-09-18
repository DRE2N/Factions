package de.erethon.factions.war.structure;

import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionStructure;
import de.erethon.factions.region.schematic.RegionSchematic;
import de.erethon.factions.region.schematic.RestoreProcess;
import de.erethon.factions.region.schematic.RestoreProcessImpl;
import de.erethon.factions.util.FLogger;
import io.papermc.paper.math.Position;
import net.kyori.adventure.util.TriState;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
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

    public WarCastleStructure(@NotNull Region region, @NotNull ConfigurationSection config) {
        super(region, config);
    }

    public WarCastleStructure(@NotNull Region region, @NotNull ConfigurationSection config, @NotNull Position a, @NotNull Position b) {
        super(region, config, a, b);
    }

    @Override
    public @NotNull TriState canBuild(@NotNull FPlayer fPlayer, @Nullable Block block) {
        if (!plugin.getWarPhaseManager().getCurrentWarPhase().isAllowPvP()) {
            return fPlayer.hasAlliance() && fPlayer.getAlliance() == region.getAlliance() ? TriState.TRUE : TriState.FALSE;
        }
        return super.canBuild(fPlayer, block);
    }

    /* Listeners */

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getWarPhaseManager().getCurrentWarPhase().isAllowPvP()) {
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
        if (!plugin.getWarPhaseManager().getCurrentWarPhase().isAllowPvP()) {
            return;
        }
        if (!containsPosition(event.getBlock().getLocation())) {
            return;
        }
        for (Block block : event.blockList()) {
            restoreProcess.addRemainingPosition(block);
        }
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
            return;
        }
        this.restoreProcess = new RestoreProcessImpl(region.getWorld(), getMinPosition(), schematic);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> serialized = super.serialize();
        if (schematic != null) {
            serialized.put("schematic", schematic.getName());
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

}
