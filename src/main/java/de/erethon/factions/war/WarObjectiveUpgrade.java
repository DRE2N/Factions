package de.erethon.factions.war;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum WarObjectiveUpgrade {

    POINT_VALUE(1000, 3),
    CRYSTAL_CAPACITY(750, 3),
    CRYSTAL_RECHARGE(750, 3),
    REPAIR_RATE(600, 3),
    OBJECTIVE_GUARDS(500, 3),
    WAYPOINT(1500, 1);

    private final double baseCost;
    private final int maxLevel;

    WarObjectiveUpgrade(double baseCost, int maxLevel) {
        this.baseCost = baseCost;
        this.maxLevel = maxLevel;
    }

    public double getCost(int nextLevel) {
        return baseCost * Math.max(1, nextLevel);
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public static @Nullable WarObjectiveUpgrade getByName(@NotNull String name) {
        for (WarObjectiveUpgrade upgrade : values()) {
            if (upgrade.name().equalsIgnoreCase(name)) {
                return upgrade;
            }
        }
        return null;
    }
}
