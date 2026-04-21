package de.erethon.factions.blocklog.mapupdate;

import com.sk89q.worldedit.regions.CuboidRegion;
import de.erethon.factions.region.Region;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Malfrador
 */
public class MapUpdateSessionManager {

    private final Map<Integer, MapUpdateSession> sessions = new ConcurrentHashMap<>();
    private final Map<Integer, VisualState> visualsByRegion = new ConcurrentHashMap<>();
    private final MapUpdateSelectionVisualizer visualizer = new MapUpdateSelectionVisualizer();

    public void createSession(@NotNull Region region, int newBaselineVersion) {
        clearVisuals(region);
        MapUpdateSession session = new MapUpdateSession(region, newBaselineVersion);
        sessions.put(region.getId(), session);
    }

    public @Nullable MapUpdateSession getSession(@NotNull Region region) {
        return sessions.get(region.getId());
    }

    public void removeSession(@NotNull Region region) {
        clearVisuals(region);
        sessions.remove(region.getId());
    }

    public boolean hasSession(@NotNull Region region) {
        return sessions.containsKey(region.getId());
    }

    public void visualizeProtectedZone(@NotNull Region region,
                                       @NotNull String zoneName,
                                       @NotNull CuboidRegion zone,
                                       @NotNull World world) {
        visualizeZone(region, zoneName, zone, world, MapUpdateSelectionVisualizer.ZoneType.PROTECT);
    }

    public void visualizeIncludeZone(@NotNull Region region,
                                     @NotNull String zoneName,
                                     @NotNull CuboidRegion zone,
                                     @NotNull World world) {
        visualizeZone(region, zoneName, zone, world, MapUpdateSelectionVisualizer.ZoneType.INCLUDE);
    }

    public void removeProtectedZoneVisualization(@NotNull Region region, @NotNull String zoneName) {
        removeZoneVisualization(region, zoneName, true);
    }

    public void removeIncludeZoneVisualization(@NotNull Region region, @NotNull String zoneName) {
        removeZoneVisualization(region, zoneName, false);
    }

    public void clearAllVisuals() {
        for (VisualState state : visualsByRegion.values()) {
            for (List<UUID> ids : state.protectedZoneDisplays.values()) {
                removeEntities(ids);
            }
            for (List<UUID> ids : state.includeZoneDisplays.values()) {
                removeEntities(ids);
            }
        }
        visualsByRegion.clear();
    }

    private void visualizeZone(@NotNull Region region,
                               @NotNull String zoneName,
                               @NotNull CuboidRegion zone,
                               @NotNull World world,
                               @NotNull MapUpdateSelectionVisualizer.ZoneType zoneType) {
        VisualState state = visualsByRegion.computeIfAbsent(region.getId(), key -> new VisualState());
        Map<String, List<UUID>> target = zoneType == MapUpdateSelectionVisualizer.ZoneType.PROTECT
                ? state.protectedZoneDisplays
                : state.includeZoneDisplays;

        List<UUID> previous = target.remove(zoneName);
        if (previous != null) {
            removeEntities(previous);
        }
        target.put(zoneName, visualizer.spawnOutline(world, zone, zoneType));
    }

    private void removeZoneVisualization(@NotNull Region region, @NotNull String zoneName, boolean protect) {
        VisualState state = visualsByRegion.get(region.getId());
        if (state == null) {
            return;
        }

        Map<String, List<UUID>> target = protect ? state.protectedZoneDisplays : state.includeZoneDisplays;
        List<UUID> ids = target.remove(zoneName);
        if (ids != null) {
            removeEntities(ids);
        }

        if (state.isEmpty()) {
            visualsByRegion.remove(region.getId());
        }
    }

    private void clearVisuals(@NotNull Region region) {
        VisualState removed = visualsByRegion.remove(region.getId());
        if (removed == null) {
            return;
        }
        for (List<UUID> ids : removed.protectedZoneDisplays.values()) {
            removeEntities(ids);
        }
        for (List<UUID> ids : removed.includeZoneDisplays.values()) {
            removeEntities(ids);
        }
    }

    private void removeEntities(@NotNull List<UUID> ids) {
        for (UUID id : ids) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
    }

    private static final class VisualState {
        private final Map<String, List<UUID>> protectedZoneDisplays = new HashMap<>();
        private final Map<String, List<UUID>> includeZoneDisplays = new HashMap<>();

        private boolean isEmpty() {
            return protectedZoneDisplays.isEmpty() && includeZoneDisplays.isEmpty();
        }
    }
}

