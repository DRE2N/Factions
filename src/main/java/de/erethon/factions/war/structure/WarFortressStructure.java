package de.erethon.factions.war.structure;

import de.erethon.factions.region.WarRegion;
import io.papermc.paper.math.Position;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public class WarFortressStructure extends WarCastleStructure {

    public WarFortressStructure(@NotNull WarRegion region, @NotNull ConfigurationSection config) {
        super(region, config);
    }

    public WarFortressStructure(@NotNull WarRegion region, @NotNull ConfigurationSection config, @NotNull Position a, @NotNull Position b) {
        super(region, config, a, b);
    }
}
