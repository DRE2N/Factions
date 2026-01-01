package de.erethon.factions.region;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * A region with PvE level bounds for mob scaling and similar mechanics.
 *
 * @author Malfrador
 */
public class PvERegion extends Region {

    private int lowerLevelBound = -1;
    private int upperLevelBound = -1;

    protected PvERegion(@NotNull RegionCache regionCache, @NotNull File file, int id, @NotNull String name, @Nullable String description) {
        super(regionCache, file, id, name, description);
    }

    protected PvERegion(@NotNull RegionCache regionCache, @NotNull File file) throws NumberFormatException {
        super(regionCache, file);
    }

    @Override
    public void load() {
        super.load();
        lowerLevelBound = config.getInt("lowerLevelBound", lowerLevelBound);
        upperLevelBound = config.getInt("upperLevelBound", upperLevelBound);
    }

    @Override
    protected void serializeData() {
        super.serializeData();
        config.set("lowerLevelBound", lowerLevelBound);
        config.set("upperLevelBound", upperLevelBound);
    }

    /* Level bounds */

    public void setMinMaxLevel(int lower, int upper) {
        this.lowerLevelBound = lower;
        this.upperLevelBound = upper;
    }

    public int getLowerLevelBound() {
        return lowerLevelBound;
    }

    public int getUpperLevelBound() {
        return upperLevelBound;
    }
}

