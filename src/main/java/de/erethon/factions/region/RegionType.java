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

    ALLIANCE_CITY(FMessage.REGION_ALLIANCE_CITY, Material.PLAYER_HEAD, NamedTextColor.LIGHT_PURPLE, false),
    BARREN(FMessage.REGION_BARREN, Material.DEAD_BUSH, NamedTextColor.GOLD),
    CAPITAL(FMessage.REGION_CAPITAL, Material.BRICK, NamedTextColor.DARK_RED, false),
    DESERT(FMessage.REGION_DESERT, Material.SAND, NamedTextColor.YELLOW),
    FARMLAND(FMessage.REGION_FARMLAND, Material.SHORT_GRASS, NamedTextColor.GREEN),
    FOREST(FMessage.REGION_FOREST, Material.OAK_LOG, NamedTextColor.DARK_GREEN),
    MAGIC(FMessage.REGION_MAGIC, Material.PURPLE_DYE, NamedTextColor.DARK_PURPLE),
    MOUNTAINOUS(FMessage.REGION_MOUNTAINOUS, Material.STONE, NamedTextColor.GRAY),
    SEA(FMessage.REGION_SEA, Material.WATER_BUCKET, NamedTextColor.BLUE),
    WAR_ZONE(FMessage.REGION_WAR_ZONE, Material.IRON_SWORD, NamedTextColor.RED),
    PVE(FMessage.REGION_PVE, Material.DIAMOND_SWORD, NamedTextColor.AQUA);

    private final FMessage name;
    private final Material icon;
    private final TextColor color;
    private final boolean allowsBuilding;

    RegionType(FMessage name, Material icon, TextColor color) {
        this(name, icon, color, true);
    }

    RegionType(FMessage name, Material icon, TextColor color, boolean allowsBuilding) {
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.allowsBuilding = allowsBuilding;
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

    /**
     * @return whether allied players are allowed to build on the region's ground
     */
    public boolean isAllowsBuilding() {
        return allowsBuilding;
    }

    public boolean isWarGround() {
        return this == WAR_ZONE || this == CAPITAL;
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
