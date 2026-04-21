package de.erethon.factions.blocklog.mapupdate;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import de.erethon.factions.region.Region;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Malfrador
 */
public class MapUpdateSession {

    public record ZoneEntry(@NotNull CuboidRegion region, int yShift) {}

    private final Region factionsRegion;
    private final int newBaselineVersion;
    private final Map<String, ZoneEntry> protectedZones = new LinkedHashMap<>();
    private final Map<String, ZoneEntry> includeOnlyZones = new LinkedHashMap<>();

    public MapUpdateSession(@NotNull Region factionsRegion, int newBaselineVersion) {
        this.factionsRegion = factionsRegion;
        this.newBaselineVersion = newBaselineVersion;
    }

    public @NotNull Region getRegion() {
        return factionsRegion;
    }

    public int getNewBaselineVersion() {
        return newBaselineVersion;
    }


    public void addProtectedZone(@NotNull String name, @NotNull CuboidRegion zone, int yShift) {
        protectedZones.put(name, new ZoneEntry(zone, yShift));
    }

    public boolean removeProtectedZone(@NotNull String name) {
        return protectedZones.remove(name) != null;
    }

    public @NotNull Map<String, ZoneEntry> getProtectedZones() {
        return Collections.unmodifiableMap(protectedZones);
    }

    public void addIncludeZone(@NotNull String name, @NotNull CuboidRegion zone, int yShift) {
        includeOnlyZones.put(name, new ZoneEntry(zone, yShift));
    }

    public boolean removeIncludeZone(@NotNull String name) {
        return includeOnlyZones.remove(name) != null;
    }

    public @NotNull Map<String, ZoneEntry> getIncludeOnlyZones() {
        return Collections.unmodifiableMap(includeOnlyZones);
    }

    public boolean hasIncludeOnlyZones() {
        return !includeOnlyZones.isEmpty();
    }

    public boolean isProtected(int x, int y, int z) {
        BlockVector3 vec = BlockVector3.at(x, y, z);
        for (ZoneEntry entry : protectedZones.values()) {
            if (entry.region().contains(vec)) {
                return true;
            }
        }
        return false;
    }

    public boolean isIncluded(int x, int y, int z) {
        if (includeOnlyZones.isEmpty()) {
            return true;
        }
        BlockVector3 vec = BlockVector3.at(x, y, z);
        for (ZoneEntry entry : includeOnlyZones.values()) {
            if (entry.region().contains(vec)) {
                return true;
            }
        }
        return false;
    }

    public int getYShiftAt(int x, int y, int z) {
        BlockVector3 vec = BlockVector3.at(x, y, z);
        for (ZoneEntry entry : protectedZones.values()) {
            if (entry.region().contains(vec)) {
                return entry.yShift();
            }
        }
        for (ZoneEntry entry : includeOnlyZones.values()) {
            if (entry.region().contains(vec)) {
                return entry.yShift();
            }
        }
        return 0;
    }

}
