package de.erethon.factions.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import de.erethon.factions.Factions;
import de.erethon.factions.marker.Marker;
import de.erethon.factions.region.LazyChunk;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionBorderCalculator;
import de.erethon.factions.region.RegionCache;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Fyreum
 */
public class RegionServerCache {

    final Factions plugin = Factions.get();
    final UUID worldId = Bukkit.getWorlds().getFirst().getUID();

    protected String jsonString = "";
    protected Map<String, String> regionChunkStrings = Map.of();
    protected Map<String, String> regionBorderStrings = Map.of();
    protected String markerJsonString = "";

    public RegionServerCache() {
        updateCache();
    }

    public void updateCache() {
        RegionCache cache = plugin.getRegionManager().getCache(worldId);
        if (cache == null) {
            return;
        }
        RegionBorderCalculator borderCalculator = plugin.getRegionBorderCalculator();
        RegionBorderCalculator.WorldBorderData worldBorderData = borderCalculator.getWorldBorderData(worldId);

        JsonObject json = new JsonObject();
        Map<String, String> regionMap = new HashMap<>(cache.getSize(), 1f);
        Map<String, String> borderMap = new HashMap<>(cache.getSize(), 1f);

        for (Region region : cache) {
            JsonObject regionJson = region.toJson();

            // Add border data if available
            if (worldBorderData != null) {
                RegionBorderCalculator.RegionBorderData borderData = worldBorderData.regionData.get(region.getId());
                if (borderData != null) {
                    regionJson.add("border", borderData.toJson());
                }
            }

            json.add(String.valueOf(region.getId()), regionJson);

            // Chunks endpoint
            JsonArray chunkArray = new JsonArray();
            for (LazyChunk chunk : region.getChunks()) {
                chunkArray.add(chunk.toString());
            }
            regionMap.put(String.valueOf(region.getId()), toJsonString(chunkArray));

            // Border endpoint
            if (worldBorderData != null) {
                RegionBorderCalculator.RegionBorderData borderData = worldBorderData.regionData.get(region.getId());
                if (borderData != null) {
                    borderMap.put(String.valueOf(region.getId()), toJsonString(borderData.toJson()));
                }
            }
        }
        this.jsonString = toJsonString(json);
        this.regionChunkStrings = Collections.unmodifiableMap(regionMap);
        this.regionBorderStrings = Collections.unmodifiableMap(borderMap);

        JsonArray markerArray = new JsonArray();
        for (Marker marker : plugin.getMarkerCache()) {
            markerArray.add(marker.toJson());
        }
        this.markerJsonString = toJsonString(markerArray);
    }

    public String toJsonString(JsonElement json) {
        try {
            StringWriter stringWriter = new StringWriter();
            JsonWriter jsonWriter = new JsonWriter(stringWriter);
            jsonWriter.setLenient(true);
            jsonWriter.setIndent("  "); // for more readability
            Streams.write(json, jsonWriter);
            return stringWriter.toString();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    /* Getters */

    public boolean containsRegion(@NotNull String regionId) {
        return regionChunkStrings.containsKey(regionId);
    }

    public @NotNull String getJsonString() {
        return jsonString;
    }

    public @Nullable String getRegionChunkString(@NotNull String regionId) {
        return regionChunkStrings.get(regionId);
    }

    public @NotNull Map<String, String> getRegionChunkStrings() {
        return regionChunkStrings;
    }

    public @Nullable String getRegionBorderString(@NotNull String regionId) {
        return regionBorderStrings.get(regionId);
    }

    public @NotNull Map<String, String> getRegionBorderStrings() {
        return regionBorderStrings;
    }

    public @NotNull String getMarkerJsonString() {
        return markerJsonString;
    }

}
