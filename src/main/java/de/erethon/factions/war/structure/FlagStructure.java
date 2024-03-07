package de.erethon.factions.war.structure;

import com.destroystokyo.paper.MaterialSetTag;
import com.destroystokyo.paper.MaterialTags;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionStructure;
import io.papermc.paper.math.Position;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Fyreum
 */
@SuppressWarnings("UnstableApiUsage")
public class FlagStructure extends RegionStructure {

    private static final Map<NamedTextColor, Material> COLOR_TO_WOOL = new HashMap<>();
    private static final Map<NamedTextColor, Material> COLOR_TO_CONCRETE = new HashMap<>();

    static {
        COLOR_TO_WOOL.put(NamedTextColor.BLACK, Material.BLACK_WOOL);
        COLOR_TO_WOOL.put(NamedTextColor.DARK_BLUE, Material.BLUE_WOOL);
        COLOR_TO_WOOL.put(NamedTextColor.DARK_GREEN, Material.GREEN_WOOL);
        COLOR_TO_WOOL.put(NamedTextColor.DARK_AQUA, Material.CYAN_WOOL);
        COLOR_TO_WOOL.put(NamedTextColor.DARK_RED, Material.RED_WOOL);
        COLOR_TO_WOOL.put(NamedTextColor.DARK_PURPLE, Material.PURPLE_WOOL);
        COLOR_TO_WOOL.put(NamedTextColor.GOLD, Material.ORANGE_WOOL);
        COLOR_TO_WOOL.put(NamedTextColor.GRAY, Material.LIGHT_GRAY_WOOL);
        COLOR_TO_WOOL.put(NamedTextColor.DARK_GRAY, Material.GRAY_WOOL);
        COLOR_TO_WOOL.put(NamedTextColor.BLUE, Material.BLUE_WOOL);
        COLOR_TO_WOOL.put(NamedTextColor.GREEN, Material.LIME_WOOL);
        COLOR_TO_WOOL.put(NamedTextColor.AQUA, Material.LIGHT_BLUE_WOOL);
        COLOR_TO_WOOL.put(NamedTextColor.RED, Material.RED_WOOL);
        COLOR_TO_WOOL.put(NamedTextColor.LIGHT_PURPLE, Material.MAGENTA_WOOL);
        COLOR_TO_WOOL.put(NamedTextColor.YELLOW, Material.YELLOW_WOOL);
        COLOR_TO_WOOL.put(NamedTextColor.WHITE, Material.WHITE_WOOL);

        COLOR_TO_CONCRETE.put(NamedTextColor.BLACK, Material.BLACK_CONCRETE);
        COLOR_TO_CONCRETE.put(NamedTextColor.DARK_BLUE, Material.BLUE_CONCRETE);
        COLOR_TO_CONCRETE.put(NamedTextColor.DARK_GREEN, Material.GREEN_CONCRETE);
        COLOR_TO_CONCRETE.put(NamedTextColor.DARK_AQUA, Material.CYAN_CONCRETE);
        COLOR_TO_CONCRETE.put(NamedTextColor.DARK_RED, Material.RED_CONCRETE);
        COLOR_TO_CONCRETE.put(NamedTextColor.DARK_PURPLE, Material.PURPLE_CONCRETE);
        COLOR_TO_CONCRETE.put(NamedTextColor.GOLD, Material.ORANGE_CONCRETE);
        COLOR_TO_CONCRETE.put(NamedTextColor.GRAY, Material.LIGHT_GRAY_CONCRETE);
        COLOR_TO_CONCRETE.put(NamedTextColor.DARK_GRAY, Material.GRAY_CONCRETE);
        COLOR_TO_CONCRETE.put(NamedTextColor.BLUE, Material.BLUE_CONCRETE);
        COLOR_TO_CONCRETE.put(NamedTextColor.GREEN, Material.LIME_CONCRETE);
        COLOR_TO_CONCRETE.put(NamedTextColor.AQUA, Material.LIGHT_BLUE_CONCRETE);
        COLOR_TO_CONCRETE.put(NamedTextColor.RED, Material.RED_CONCRETE);
        COLOR_TO_CONCRETE.put(NamedTextColor.LIGHT_PURPLE, Material.MAGENTA_CONCRETE);
        COLOR_TO_CONCRETE.put(NamedTextColor.YELLOW, Material.YELLOW_CONCRETE);
        COLOR_TO_CONCRETE.put(NamedTextColor.WHITE, Material.WHITE_CONCRETE);
    }

    public FlagStructure(@NotNull Region region, @NotNull ConfigurationSection config) {
        super(region, config);
    }

    public FlagStructure(@NotNull Region region, @NotNull ConfigurationSection config, @NotNull Position a, @NotNull Position b) {
        super(region, config, a, b);
    }

    @Override
    public @NotNull TriState canBuild(@NotNull FPlayer fPlayer, @Nullable Block block) {
        if (block != null && block.getType().name().contains("WOOL")) {
            return TriState.FALSE;
        }
        return super.canBuild(fPlayer, block);
    }

    public void displayColor(@NotNull World world, @NotNull NamedTextColor color) {
        final Material wool = COLOR_TO_WOOL.getOrDefault(color, Material.WHITE_WOOL);
        final Material concrete = COLOR_TO_CONCRETE.getOrDefault(color, Material.WHITE_CONCRETE);
        final Position minPosition = getMinPosition();
        final Position maxPosition = getMaxPosition();
        final int maxX = maxPosition.blockX(), maxY = maxPosition.blockY(), maxZ = maxPosition.blockZ();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (int x = minPosition.blockX(); x < maxX; x++) {
                for (int y = minPosition.blockY(); y < maxY; y++) {
                    for (int z = minPosition.blockZ(); z < maxZ; z++) {
                        if (MaterialSetTag.WOOL.isTagged(world.getType(x, y, z))) {
                            world.setType(x, y, z, wool);
                        } else if (MaterialTags.CONCRETES.isTagged(world.getType(x, y, z))) {
                            world.setType(x, y, z, concrete);
                        }
                    }
                }
            }
        });
    }

}
