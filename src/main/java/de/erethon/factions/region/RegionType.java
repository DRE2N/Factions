package de.erethon.factions.region;

import de.erethon.factions.data.FMessage;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Fyreum
 */
public enum RegionType {

    BARREN(FMessage.REGION_BARREN, Material.DEAD_BUSH, NamedTextColor.GOLD),
    CAPITAL(FMessage.REGION_CAPITAL, Material.BRICK, NamedTextColor.DARK_RED),
    CITY(FMessage.REGION_CITY, Material.GLASS_PANE, NamedTextColor.WHITE),
    DESERT(FMessage.REGION_DESERT, Material.SAND, NamedTextColor.YELLOW),
    FARMLAND(FMessage.REGION_FARMLAND, Material.GRASS, NamedTextColor.GREEN),
    FOREST(FMessage.REGION_FOREST, Material.OAK_LOG, NamedTextColor.DARK_GREEN),
    MAGIC(FMessage.REGION_MAGIC, Material.PURPLE_DYE, NamedTextColor.DARK_PURPLE),
    MOUNTAINOUS(FMessage.REGION_MOUNTAINOUS, Material.STONE, NamedTextColor.GRAY),
    SEA(FMessage.REGION_SEA, Material.WATER_BUCKET, NamedTextColor.BLUE),
    WAR_ZONE(FMessage.REGION_WAR_ZONE, Material.IRON_SWORD, NamedTextColor.RED);

    private final FMessage name;
    private final Material icon;
    private final TextColor color;

    RegionType(FMessage name, Material icon, TextColor color) {
        this.name = name;
        this.icon = icon;
        this.color = color;
    }

    /**
     * @return the name of the region type
     */
    public @NotNull String getName() {
        return name.getMessage();
    }

    /**
     * @return the icon displayed in GUIs
     */
    public @NotNull Material getIcon() {
        return icon;
    }

    /**
     * @return the color of the region type name
     */
    public @NotNull TextColor getColor() {
        return color;
    }

    /* Statics */

    public static @Nullable RegionType getByName(@NotNull String name) {
        return getByName(name, null);
    }

    @Contract("_, !null -> !null")
    public static @Nullable RegionType getByName(@NotNull String name, @Nullable RegionType def) {
        for (RegionType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return def;
    }

}
