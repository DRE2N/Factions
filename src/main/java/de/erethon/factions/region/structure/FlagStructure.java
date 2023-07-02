package de.erethon.factions.region.structure;

import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import io.papermc.paper.math.Position;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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

    }

    public FlagStructure(@NotNull Position a, @NotNull Position b) {
        super(a, b);
    }

    @Override
    public @NotNull TriState canBuild(@NotNull FPlayer fPlayer, @NotNull Region region, @Nullable Block block) {
        if (block != null && block.getType().name().contains("WOOL")) {
            return TriState.FALSE;
        }
        return super.canBuild(fPlayer, region, block);
    }

    public void displayColor(@NotNull World world, @NotNull NamedTextColor color) {
        final Material wool = COLOR_TO_WOOL.getOrDefault(color, Material.WHITE_WOOL);
        final Position minPosition = getMinPosition();
        final Position maxPosition = getMaxPosition();
        final int maxX = maxPosition.blockX(), maxY = maxPosition.blockY(), maxZ = maxPosition.blockZ();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (int x = minPosition.blockX(); x < maxX; x++) {
                for (int y = minPosition.blockY(); y < maxY; y++) {
                    for (int z = minPosition.blockZ(); z < maxZ; z++) {
                        if (!world.getType(x, y, z).name().contains("WOOL")) {
                            continue;
                        }
                        world.setType(x, y, z, wool);
                    }
                }
            }
        });
    }

}
