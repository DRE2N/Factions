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
import de.erethon.factions.region.RegionType;
import de.erethon.factions.util.FLogger;
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
    protected String mapdataJsonString = "";

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

        // Generate combined regions with borders data. Mostly useful for map display purposes.
        JsonArray regionsWithBordersArray = new JsonArray();
        for (Region region : cache) {
            // Include only claimed regions, or CAPITAL and WAR_ZONE types
            if (!region.isOwned()
                    && region.getType() != RegionType.CAPITAL
                    && region.getType() != RegionType.WAR_ZONE
                    && region.getType() != RegionType.ALLIANCE_CITY
            ) {
                continue;
            }

            JsonObject regionData = new JsonObject();
            regionData.addProperty("id", region.getId());
            regionData.addProperty("name", region.getName());
            regionData.addProperty("type", region.getType().name());
            regionData.addProperty("alliance", region.hasAlliance() ? region.getAlliance().getId() : null);

            String color = switch (region.getType()) {
                case CAPITAL -> Factions.get().getFConfig().getWebCapitalColor();
                case WAR_ZONE -> Factions.get().getFConfig().getWebWarZoneColor();
                // Highlight alliance cities in pink if no alliance
                case ALLIANCE_CITY -> region.hasAlliance() ? region.getAlliance().getColor().asHexString() : "FFC0CB";
                default -> region.hasAlliance()
                        ? region.getAlliance().getColor().asHexString()
                        : Factions.get().getFConfig().getWebDefaultColor();
            };

            regionData.addProperty("color", color);

            // Add owner faction info
            if (region.getOwner() != null) {
                regionData.addProperty("ownerName", region.getOwner().getName(true));
                regionData.addProperty("ownerId", region.getOwner().getId());
            } else {
                regionData.addProperty("ownerName", (String) null);
                regionData.addProperty("ownerId", -1);
            }

            // Add border polygon if available
            if (worldBorderData != null) {
                RegionBorderCalculator.RegionBorderData borderData = worldBorderData.regionData.get(region.getId());
                if (borderData != null) {
                    regionData.add("border", borderData.toJson());
                }
            }

            regionsWithBordersArray.add(regionData);
        }
        this.mapdataJsonString = toJsonString(regionsWithBordersArray);
        FLogger.INFO.log("Updated region server cache for world " + worldId);
    }

    public String toJsonString(JsonElement json) {
        try {
            StringWriter stringWriter = new StringWriter();
            JsonWriter jsonWriter = new JsonWriter(stringWriter);
            jsonWriter.setLenient(true);
            jsonWriter.setIndent(""); // not human readable, but saves space
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

    public @NotNull String getMapDataJsonString() {
        return mapdataJsonString;
    }

}
