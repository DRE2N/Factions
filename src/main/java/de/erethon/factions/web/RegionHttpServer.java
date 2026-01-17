package de.erethon.factions.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.erethon.factions.Factions;
import de.erethon.factions.util.FLogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Fyreum
 */
public class RegionHttpServer extends Thread {

    public static final String REGIONS_PATH = "/v1/regions"; // Specific region data
    public static final String MARKERS_PATH = "/v1/markers"; // Get all markers
    public static final String PLAYER_POSITION_PATH = "/v1/player-position"; // Get player position based on IP address
    public static final String MAPDATA_PATH = "/v1/mapdata"; // Combined map data for all claimed or war regions, for easy map rendering

    private final RegionServerCache cache;
    private HttpServer server;

    public RegionHttpServer() {
        this.cache = new RegionServerCache();
    }

    @Override
    public void run() {
        try {
            server = HttpServer.create(new InetSocketAddress(Factions.get().getFConfig().getWebPort()), 0);
            server.createContext(REGIONS_PATH, this::handleRegions);
            server.createContext(MARKERS_PATH, this::handleMarkers);
            server.createContext(PLAYER_POSITION_PATH, this::handlePlayerPosition);
            server.createContext(MAPDATA_PATH, this::handleMapData);
            server.setExecutor(null);
            server.start();
        } catch (Exception e) {
            FLogger.ERROR.log("Failed to create HttpServer: " + e.getMessage());
        }
    }

    public void stopServer() {
        server.stop(0);
    }

    public void handleRegions(HttpExchange exchange) throws IOException {
        if (exchange == null) {
            throw new NullPointerException("HttpExchange is null");
        }
        FLogger.WEB.log("Handling web request from: " + exchange.getRemoteAddress() + " for: " + exchange.getRequestURI().getPath());

        Map<String, String> queryParams = decodeQueryParams(exchange);

        if (!authenticateRequest(queryParams)) {
            respondNotAuthenticated(exchange);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String cleanPath = path.replaceFirst("/v1/regions/?", "");

        if (cleanPath.isEmpty()) {
            respondRegions(exchange);
            return;
        }
        String regionId = cleanPath.substring(0, cleanPath.indexOf('/'));

        if (cache.containsRegion(regionId)) {
            handleRegionRequest(exchange, regionId, cleanPath.substring(Math.min(cleanPath.length() - 1, regionId.length() + 1)));
            return;
        }
        respondNotFound(exchange);
    }

    void handleRegionRequest(HttpExchange exchange, String regionId, String subPath) throws IOException {
        if (subPath.isEmpty()) {
            respondNotFound(exchange);
            return;
        }
        // Switch, because we might want to add more subPaths in the future
        switch (subPath) {
            case "chunks", "chunks/" -> respondRegionChunks(exchange, regionId);
            case "border", "border/" -> respondRegionBorder(exchange, regionId);
            default -> respondNotFound(exchange);
        }
    }

    void respondRegions(HttpExchange exchange) throws IOException {
        String response = cache.getJsonString();
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    void respondRegionChunks(HttpExchange exchange, String regionId) throws IOException {
        String response = cache.getRegionChunkString(regionId);
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    void respondRegionBorder(HttpExchange exchange, String regionId) throws IOException {
        String response = cache.getRegionBorderString(regionId);
        if (response == null) {
            respondNotFound(exchange);
            return;
        }
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    void respondStatusCode(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, 0);
        exchange.getResponseBody().close();
    }

    void respondNotFound(HttpExchange exchange) throws IOException {
        respondStatusCode(exchange, 404);
    }

    void respondNotAuthenticated(HttpExchange exchange) throws IOException {
        FLogger.WEB.log("Refusing web request from " + exchange.getRemoteAddress() + ": Unauthenticated access");
        exchange.sendResponseHeaders(401, 0);
        exchange.getResponseBody().close();
    }

    void handleMarkers(HttpExchange exchange) throws IOException {
        FLogger.WEB.log("Handling markers request from: " + exchange.getRemoteAddress());

        Map<String, String> queryParams = decodeQueryParams(exchange);

        if (!authenticateRequest(queryParams)) {
            respondNotAuthenticated(exchange);
            return;
        }

        String response = cache.getMarkerJsonString();
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    void handlePlayerPosition(HttpExchange exchange) throws IOException {
        FLogger.WEB.log("Handling player position request from: " + exchange.getRemoteAddress());

        Map<String, String> queryParams = decodeQueryParams(exchange);

        if (!authenticateRequest(queryParams)) {
            respondNotAuthenticated(exchange);
            return;
        }

        String ipAddress = queryParams.get("ip");

        if (ipAddress == null || ipAddress.isEmpty()) {
            respondStatusCode(exchange, 400);
            return;
        }

        Player foundPlayer = null;
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (player.getAddress() != null && player.getAddress().getAddress().getHostAddress().equals(ipAddress)) {
                foundPlayer = player;
                break;
            }
        }

        if (foundPlayer == null) {
            respondNotFound(exchange);
            return;
        }

        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("name", foundPlayer.getName());
        json.addProperty("uuid", foundPlayer.getUniqueId().toString());
        json.addProperty("x", foundPlayer.getLocation().getBlockX());
        json.addProperty("y", foundPlayer.getLocation().getBlockY());
        json.addProperty("z", foundPlayer.getLocation().getBlockZ());
        json.addProperty("world", foundPlayer.getWorld().getName());

        String response = cache.toJsonString(json);
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    void handleMapData(HttpExchange exchange) throws IOException {
        FLogger.WEB.log("Handling mapdata request from: " + exchange.getRemoteAddress());

        Map<String, String> queryParams = decodeQueryParams(exchange);

        if (!authenticateRequest(queryParams)) {
            respondNotAuthenticated(exchange);
            return;
        }

        String response = cache.getMapDataJsonString();
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private Map<String, String> decodeQueryParams(HttpExchange exchange) {
        Map<String, String> queryParams = new HashMap<>();
        String[] pairs = exchange.getRequestURI().getQuery().split("&");

        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            queryParams.put(
                    URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8)
            );
        }
        return queryParams;
    }

    private boolean authenticateRequest(Map<String, String> queryParams) {
        String authToken = queryParams.get("auth_token");
        return authToken != null && authToken.equals(Factions.get().getFConfig().getWebAuthToken());
    }

    /* Getters */

    public @NotNull RegionServerCache getCache() {
        return cache;
    }

    public @NotNull HttpServer getServer() {
        return server;
    }
}
