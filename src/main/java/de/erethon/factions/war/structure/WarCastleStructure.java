package de.erethon.factions.war.structure;

import de.erethon.factions.event.WarPhaseChangeEvent;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionStructure;
import de.erethon.factions.region.schematic.SchematicSavable;
import de.erethon.factions.util.FLogger;
import io.papermc.paper.math.Position;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
public class WarCastleStructure extends RegionStructure implements Listener, SchematicSavable {


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
    public String getSchematicID() {
        return getName() + "_" + getRegion().getId();
    }

    @Override
    public Location getOrigin() {
        return getCenterPosition().toLocation(getRegion().getWorld());
    }
}
