package de.erethon.factions.marker;

import de.erethon.factions.util.FLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MarkerCache implements Iterable<Marker> {

    private final File folder;
    private final Map<Integer, Marker> cache = new HashMap<>();
    private int nextId = 0;

    public MarkerCache(@NotNull File folder) {
        this.folder = folder;
    }

    public void loadAll() {
        FLogger.INFO.log("Loading markers...");
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }
        int count = 0;
        for (File file : files) {
            if (!file.getName().endsWith(".yml")) {
                continue;
            }
            Marker marker = load(file);
            if (marker != null) {
                cache.put(marker.getId(), marker);
                if (marker.getId() >= nextId) {
                    nextId = marker.getId() + 1;
                }
                count++;
            }
        }
        FLogger.INFO.log("Loaded " + count + " markers");
    }

    protected @Nullable Marker load(@NotNull File file) {
        try {
            Marker marker = new Marker(this, file);
            marker.load();
            return marker;
        } catch (Exception e) {
            FLogger.ERROR.log("Failed to load marker from file '" + file.getName() + "': " + e.getMessage());
            return null;
        }
    }

    public @NotNull Marker createMarker(@NotNull String iconPath, int x, int z) {
        int id = nextId++;
        FLogger.INFO.log("Creating marker " + id + " at " + x + "," + z);
        Marker marker = new Marker(this, new File(folder, id + ".yml"), id, iconPath, x, z);
        cache.put(id, marker);
        marker.save();
        return marker;
    }

    protected void removeMarker(@NotNull Marker marker) {
        cache.remove(marker.getId());
    }

    public void saveAll() {
        for (Marker marker : cache.values()) {
            marker.save();
        }
    }

    /* Getters */

    public @Nullable Marker get(int id) {
        return cache.get(id);
    }

    public @Nullable Marker get(@NotNull String id) {
        try {
            return cache.get(Integer.parseInt(id));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public @NotNull Map<Integer, Marker> getCache() {
        return cache;
    }

    public int getSize() {
        return cache.size();
    }

    @Override
    public @NotNull Iterator<Marker> iterator() {
        return cache.values().iterator();
    }
}

