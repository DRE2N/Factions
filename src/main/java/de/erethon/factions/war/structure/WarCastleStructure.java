package de.erethon.factions.war.structure;

import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionStructure;
import de.erethon.factions.region.schematic.RegionSchematic;
import de.erethon.factions.util.FLogger;
import io.papermc.paper.math.Position;
import net.kyori.adventure.util.TriState;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * An area that can only be modified, when warzones are closed.
 *
 * @author Fyreum
 */
@SuppressWarnings("UnstableApiUsage")
public class WarCastleStructure extends RegionStructure {

    protected RegionSchematic schematic;

    public WarCastleStructure(@NotNull Region region, @NotNull ConfigurationSection config) {
        super(region, config);
        load(config);
    }

    public WarCastleStructure(@NotNull Region region, @NotNull ConfigurationSection config, @NotNull Position a, @NotNull Position b) {
        super(region, config, a, b);
        load(config);
    }

    private void load(@NotNull ConfigurationSection config) {
        String schematicId = config.getString("schematicId");
        if (schematicId == null) {
            FLogger.ERROR.log("Illegal schematic id in war castle '" + name + "' found: null");
            return;
        }
        this.schematic = plugin.getRegionSchematicManager().getSchematic(schematicId);
    }

    @Override
    public @NotNull TriState canBuild(@NotNull FPlayer fPlayer, Block block) {
        if (!plugin.getWarPhaseManager().getCurrentWarPhase().isAllowPvP()) {
            return fPlayer.hasAlliance() && fPlayer.getAlliance() == region.getAlliance() ? TriState.TRUE : TriState.FALSE;
        }
        return super.canBuild(fPlayer, block);
    }

    public void startRestoring() {
        if (schematic == null || schematic.hasRestoreProcess()) {
            return;
        }
        schematic.restoreAt(region.getWorld(), getMinPosition(), plugin.getFConfig().getWarCastleRestoreInterval());
    }

    /* Serialization */

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
}
