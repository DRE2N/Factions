package de.erethon.factions.region.structure;

import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import io.papermc.paper.math.Position;
import net.kyori.adventure.util.TriState;
import org.jetbrains.annotations.NotNull;

/**
 * An area that can only be modified, when warzones are closed.
 *
 * @author Fyreum
 */
@SuppressWarnings("UnstableApiUsage")
public class WarCastleStructure extends RegionStructure {

    public WarCastleStructure(@NotNull Position a, @NotNull Position b) {
        super(a, b);
    }

    @Override
    public @NotNull TriState canBuild(@NotNull FPlayer fPlayer, @NotNull Region region) {
        if (plugin.getWarPhaseManager().getCurrentWarPhase().isOpenWarZones()) {
            return TriState.NOT_SET;
        }
        return fPlayer.hasAlliance() && fPlayer.getAlliance() == region.getAlliance() ? TriState.TRUE : TriState.FALSE;
    }
}
