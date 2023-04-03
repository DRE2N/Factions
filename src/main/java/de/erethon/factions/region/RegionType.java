package de.erethon.factions.region;

import de.erethon.factions.data.FMessage;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Fyreum
 */
public enum RegionType {

    BARREN(FMessage.REGION_BARREN),
    CITY(FMessage.REGION_CITY),
    DESERT(FMessage.REGION_DESERT),
    FARMLAND(FMessage.REGION_FARMLAND),
    FOREST(FMessage.REGION_FOREST),
    MAGIC(FMessage.REGION_MAGIC),
    MOUNTAINOUS(FMessage.REGION_MOUNTAINOUS),
    SEA(FMessage.REGION_SEA),
    WAR_ZONE(FMessage.REGION_WAR_ZONE);

    private final FMessage name;

    RegionType(FMessage name) {
        this.name = name;
    }

    /**
     * @return the name of the region type
     */
    public @NotNull String getName() {
        return name.getMessage();
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
