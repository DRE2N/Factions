package de.erethon.factions.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import de.erethon.factions.Factions;
import de.erethon.factions.region.LazyChunk;
import de.erethon.factions.region.Region;
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

    public RegionServerCache() {
        updateCache();
    }

    public void updateCache() {
        RegionCache cache = plugin.getRegionManager().getCache(worldId);
        if (cache == null) {
            return;
        }
        JsonObject json = new JsonObject();
        Map<String, String> regionMap = new HashMap<>(cache.getSize(), 1f);

        for (Region region : cache) {
            json.add(String.valueOf(region.getId()), region.toJson());

            JsonArray chunkArray = new JsonArray();

            for (LazyChunk chunk : region.getChunks()) {
                chunkArray.add(chunk.toString());
            }
            regionMap.put(String.valueOf(region.getId()), toJsonString(chunkArray));
        }
        this.jsonString = toJsonString(json); //json.toString();
        this.regionChunkStrings = Collections.unmodifiableMap(regionMap);
    }

    String toJsonString(JsonElement json) {
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

}
