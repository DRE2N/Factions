package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.region.LazyChunk;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionBorderCalculator;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Debug command to visualize region borders and polygon data.
 *
 * @author Malfrador
 */
public class RegionBorderDebugCommand extends FCommand {

    public RegionBorderDebugCommand() {
        setCommand("borderdebug");
        setAliases("bd");
        setMinMaxArgs(2, 2);
        setConsoleCommand(true);
        setPermissionFromName();
        setFUsage(getCommand() + " <polygon|map|chunks> <region>");
        setDescription("Zeigt Debug-Informationen zu Regionsgrenzen");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        String mode = args[1].toLowerCase();
        String target = args[2];

        // Find the region
        Region region = null;
        try {
            int regionId = Integer.parseInt(target);
            region = plugin.getRegionManager().getRegionById(regionId);
        } catch (NumberFormatException ignored) {
            region = plugin.getRegionManager().getRegionByName(target);
        }

        if (region == null) {
            MessageUtil.sendMessage(sender, "&cRegion not found: " + target);
            return;
        }

        UUID worldId = region.getWorldId();
        RegionBorderCalculator.WorldBorderData worldData = plugin.getRegionBorderCalculator().getWorldBorderData(worldId);

        if (worldData == null) {
            MessageUtil.sendMessage(sender, "&cNo border data found for this world. Run /f recalculateborders first.");
            return;
        }

        RegionBorderCalculator.RegionBorderData borderData = worldData.regionData.get(region.getId());
        if (borderData == null) {
            MessageUtil.sendMessage(sender, "&cNo border data found for region " + region.getName() + ". Run /f recalculateborders " + region.getName());
            return;
        }

        switch (mode) {
            case "polygon", "poly", "p" -> showPolygonCoordinates(sender, region, borderData);
            case "map", "m" -> showAsciiMap(sender, region, borderData);
            case "chunks", "c" -> showChunkInfo(sender, region, borderData);
            default -> MessageUtil.sendMessage(sender, "&cUnknown mode: " + mode + ". Use: polygon, map, or chunks");
        }
    }

    private void showPolygonCoordinates(CommandSender sender, Region region, RegionBorderCalculator.RegionBorderData borderData) {
        MessageUtil.sendMessage(sender, "&6=== Polygon for " + region.getName() + " (ID: " + region.getId() + ") ===");
        MessageUtil.sendMessage(sender, "&7Vertices: " + borderData.polygon.size());
        MessageUtil.sendMessage(sender, "&7Contiguous: " + (borderData.isContiguous ? "&aYes" : "&cNo"));
        MessageUtil.sendMessage(sender, "&7Neighbors: &f" + borderData.neighborIds);

        if (borderData.polygon.isEmpty()) {
            MessageUtil.sendMessage(sender, "&cNo polygon data available.");
            return;
        }

        MessageUtil.sendMessage(sender, "&7Polygon coordinates (block coords):");
        StringBuilder sb = new StringBuilder("&f");
        for (int i = 0; i < borderData.polygon.size(); i++) {
            int[] vertex = borderData.polygon.get(i);
            sb.append("(").append(vertex[0]).append(",").append(vertex[1]).append(")");
            if (i < borderData.polygon.size() - 1) {
                sb.append(" → ");
            }
            // Line break every 4 vertices to avoid chat overflow
            if ((i + 1) % 4 == 0 && i < borderData.polygon.size() - 1) {
                MessageUtil.sendMessage(sender, sb.toString());
                sb = new StringBuilder("&f  ");
            }
        }
        if (!sb.toString().equals("&f  ")) {
            MessageUtil.sendMessage(sender, sb.toString());
        }

        // Show holes if any
        if (!borderData.holes.isEmpty()) {
            MessageUtil.sendMessage(sender, "&7Holes (enclosed regions): " + borderData.holes.size());
            for (int h = 0; h < borderData.holes.size(); h++) {
                List<int[]> hole = borderData.holes.get(h);
                MessageUtil.sendMessage(sender, "&7  Hole " + (h + 1) + ": " + hole.size() + " vertices");
            }
        }
    }

    private void showAsciiMap(CommandSender sender, Region region, RegionBorderCalculator.RegionBorderData borderData) {
        MessageUtil.sendMessage(sender, "&6=== ASCII Map for " + region.getName() + " ===");

        Set<LazyChunk> chunks = region.getChunks();
        if (chunks.isEmpty()) {
            MessageUtil.sendMessage(sender, "&cRegion has no chunks.");
            return;
        }

        // Find bounds
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (LazyChunk chunk : chunks) {
            minX = Math.min(minX, chunk.getX());
            maxX = Math.max(maxX, chunk.getX());
            minZ = Math.min(minZ, chunk.getZ());
            maxZ = Math.max(maxZ, chunk.getZ());
        }

        int width = maxX - minX + 1;

        if (width > 60) {
            MessageUtil.sendMessage(sender, "&cRegion too wide for ASCII map (" + width + " chunks wide, max 60)");
            MessageUtil.sendMessage(sender, "&7Showing bounds: X[" + minX + " to " + maxX + "] Z[" + minZ + " to " + maxZ + "]");
            MessageUtil.sendMessage(sender, "&7Use /f borderdebug polygon " + region.getName() + " for coordinates instead.");
            return;
        }

        Set<LazyChunk> edgeChunks = findEdgeChunks(chunks);

        MessageUtil.sendMessage(sender, "&7Chunk coordinates: X[" + minX + " to " + maxX + "] Z[" + minZ + " to " + maxZ + "]");
        MessageUtil.sendMessage(sender, "&7Legend: &a·&7=edge &2·&7=inner &8·&7=empty");

        for (int z = minZ; z <= maxZ; z++) {
            StringBuilder row = new StringBuilder("&f");
            for (int x = minX; x <= maxX; x++) {
                LazyChunk chunk = new LazyChunk(x, z);
                if (chunks.contains(chunk)) {
                    if (edgeChunks.contains(chunk)) {
                        row.append("&a·"); // Edge chunk - bright green
                    } else {
                        row.append("&2·"); // Inner chunk - dark green
                    }
                } else {
                    row.append("&8·"); // Empty
                }
            }
            MessageUtil.sendMessage(sender, row.toString());
        }
    }

    private void showChunkInfo(CommandSender sender, Region region, RegionBorderCalculator.RegionBorderData borderData) {
        MessageUtil.sendMessage(sender, "&6=== Chunk Info for " + region.getName() + " ===");

        Set<LazyChunk> chunks = region.getChunks();
        Set<LazyChunk> edgeChunks = findEdgeChunks(chunks);

        MessageUtil.sendMessage(sender, "&7Total chunks: &f" + chunks.size());
        MessageUtil.sendMessage(sender, "&7Edge chunks: &f" + edgeChunks.size());
        MessageUtil.sendMessage(sender, "&7Inner chunks: &f" + (chunks.size() - edgeChunks.size()));
        MessageUtil.sendMessage(sender, "&7Contiguous: " + (borderData.isContiguous ? "&aYes" : "&cNo"));
        MessageUtil.sendMessage(sender, "&7Neighbors: &f" + borderData.neighborIds);
        MessageUtil.sendMessage(sender, "&7Polygon vertices: &f" + borderData.polygon.size());
        MessageUtil.sendMessage(sender, "&7Holes: &f" + borderData.holes.size());

        // Find bounds
        if (!chunks.isEmpty()) {
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

            for (LazyChunk chunk : chunks) {
                minX = Math.min(minX, chunk.getX());
                maxX = Math.max(maxX, chunk.getX());
                minZ = Math.min(minZ, chunk.getZ());
                maxZ = Math.max(maxZ, chunk.getZ());
            }

            MessageUtil.sendMessage(sender, "&7Bounding box (chunks): &fX[" + minX + "," + maxX + "] Z[" + minZ + "," + maxZ + "]");
            MessageUtil.sendMessage(sender, "&7Bounding box (blocks): &fX[" + (minX * 16) + "," + ((maxX + 1) * 16) + "] Z[" + (minZ * 16) + "," + ((maxZ + 1) * 16) + "]");
        }
    }

    /**
     * Find edge chunks (chunks with at least one neighbor outside the region)
     */
    private Set<LazyChunk> findEdgeChunks(Set<LazyChunk> chunks) {
        Set<LazyChunk> edges = new java.util.HashSet<>();

        for (LazyChunk chunk : chunks) {
            int x = chunk.getX();
            int z = chunk.getZ();

            // Check 4 cardinal neighbors
            if (!chunks.contains(new LazyChunk(x + 1, z)) ||
                !chunks.contains(new LazyChunk(x - 1, z)) ||
                !chunks.contains(new LazyChunk(x, z + 1)) ||
                !chunks.contains(new LazyChunk(x, z - 1))) {
                edges.add(chunk);
            }
        }

        return edges;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> modes = new ArrayList<>();
            for (String mode : List.of("polygon", "map", "chunks")) {
                if (mode.startsWith(args[1].toLowerCase())) {
                    modes.add(mode);
                }
            }
            return modes;
        }
        if (args.length == 3) {
            return getTabRegions(args[2]);
        }
        return null;
    }
}

