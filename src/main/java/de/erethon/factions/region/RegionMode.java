package de.erethon.factions.region;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum RegionMode {
    PVP(Component.translatable("factions.region.mode.pvp"), TextColor.color(0xFF0000)), // Red color for PVP
    PVE(Component.translatable("factions.region.mode.pve"), TextColor.color(0x00FF00)), // Green color for PVE
    PVPVE(Component.translatable("factions.region.mode.pvpve"), TextColor.color(0xFFFF00)), // Yellow color for PVPVE
    SAFE_ZONE(Component.translatable("factions.region.mode.safe_zone"), TextColor.color(0x0000FF)); // Blue color for Safe Zone;

    private final Component name;
    private final TextColor color;

    RegionMode(Component name, TextColor color) {
        this.name = name;
        this.color = color;
    }

    public @NotNull Component getName() {
        return name;
    }

    public @NotNull TextColor getColor() {
        return color;
    }

    public static @Nullable RegionMode getByName(@NotNull String name) {
        return getByName(name, null);
    }

    public boolean isSafe() {
        return this == SAFE_ZONE || this == PVE;
    }

    @Contract("_, !null -> !null")
    public static @Nullable RegionMode getByName(@NotNull String name, @Nullable RegionMode def) {
        for (RegionMode mode : values()) {
            if (mode.name().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return def;
    }
}
