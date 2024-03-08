package de.erethon.factions.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.erethon.factions.Factions;
import de.erethon.factions.util.FLogger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * @author Fyreum
 */
public class RegionHttpServer implements HttpHandler {

    public static final String CONTEXT_PATH = "/v1/regions";

    private final RegionServerCache cache;
    private HttpServer server;

    public RegionHttpServer() {
        this.cache = new RegionServerCache();
    }

    public void runServer() {
        try {
            server = HttpServer.create(new InetSocketAddress(Factions.get().getFConfig().getWebPort()), 0);
            server.createContext(CONTEXT_PATH, this);
            server.setExecutor(null);
            server.start();
        } catch (Exception e) {
            FLogger.ERROR.log("Failed to create HttpServer: " + e.getMessage());
        }
    }

    public void stopServer() {
        server.stop(0);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (exchange == null) {
            throw new NullPointerException("HttpExchange is null");
        }
        FLogger.WEB.log("Handling web request from: " + exchange.getRemoteAddress() + " for: " + exchange.getRequestURI().getPath());

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

    void respondNotFound(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(404, 0);
        exchange.getResponseBody().close();
    }

    /* Getters */

    public @NotNull RegionServerCache getCache() {
        return cache;
    }

    public @NotNull HttpServer getServer() {
        return server;
    }
}
