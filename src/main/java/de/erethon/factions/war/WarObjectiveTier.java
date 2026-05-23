package de.erethon.factions.war;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum WarObjectiveTier {

    CAMP(1, 120.0, false),
    TOWER(3, 180.0, false),
    FORTRESS(6, 260.0, true),
    CAPITAL(10, 320.0, true);

    private final int defaultPointValue;
    private final double defaultCaptureCap;
    private final boolean supportsWaypoint;

    WarObjectiveTier(int defaultPointValue, double defaultCaptureCap, boolean supportsWaypoint) {
        this.defaultPointValue = defaultPointValue;
        this.defaultCaptureCap = defaultCaptureCap;
        this.supportsWaypoint = supportsWaypoint;
    }

    public int getDefaultPointValue() {
        return defaultPointValue;
    }

    public double getDefaultCaptureCap() {
        return defaultCaptureCap;
    }

    public boolean supportsWaypoint() {
        return supportsWaypoint;
    }

    public static @Nullable WarObjectiveTier getByName(@NotNull String name) {
        for (WarObjectiveTier tier : values()) {
            if (tier.name().equalsIgnoreCase(name)) {
                return tier;
            }
        }
        return null;
    }
}
