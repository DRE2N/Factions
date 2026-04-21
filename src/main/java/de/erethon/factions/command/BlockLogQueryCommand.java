package de.erethon.factions.command;

import de.erethon.factions.Factions;
import de.erethon.factions.blocklog.BlockPalette;
import de.erethon.factions.blocklog.filter.BlockLogFilter;
import de.erethon.factions.blocklog.filter.BlockLogFilterParser;
import de.erethon.factions.blocklog.filter.BlockLogQueryBuilder;
import de.erethon.factions.blocklog.model.BlockChange;
import de.erethon.factions.blocklog.model.ChangeType;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.region.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Command to query block log history with flexible filters.
 * <p>
 * Usage: /f blocklog query [filters...]
 * <p>
 * Consecutive changes of the same type, material and player are collapsed
 * into a single line (e.g. "placed terracotta ×18").
 *
 * @author Malfrador
 */
public class BlockLogQueryCommand extends FCommand {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public BlockLogQueryCommand() {
        setCommand("query");
        setAliases("q", "search", "lookup", "l");
        setMinMaxArgs(1, 99);
        setConsoleCommand(true);
        setPermission("factions.admin.blocklog.query");
        setFUsage(BlockLogCommand.LABEL + " " + "query" + " [filters...]");
        setDescription("Query block log - filters: player: area: sel since: before: block: region: type: limit: page: asc/desc");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        BlockLogFilter filter = BlockLogFilterParser.parse(sender, args, 1);

        sender.sendMessage(Component.text("Querying block log...", NamedTextColor.YELLOW));

        Bukkit.getScheduler().runTaskAsynchronously(Factions.getInstance(), () -> {
            try {
                Factions factions = Factions.getInstance();
                BlockPalette palette = factions.getBlockLogManager().getPalette();

                // Fetch all matching rows (no DB-level pagination) so we can
                // collapse them first, then paginate by display lines.
                BlockLogFilter fetchFilter = filter.withNoLimit();
                List<BlockChange> allChanges = palette.getJdbi()
                        .withHandle(new BlockLogQueryBuilder(fetchFilter)::execute);

                // Collapse on the async thread so the main thread only does chat I/O
                List<CollapsedEntry> allCollapsed = collapseEntries(allChanges, palette);

                Bukkit.getScheduler().runTask(factions, () ->
                        displayResults(sender, allCollapsed, filter));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(Factions.getInstance(), () ->
                        sender.sendMessage(Component.text("Error querying block log: " + e.getMessage(), NamedTextColor.RED)));
                e.printStackTrace();
            }
        });
    }

    // ---- Display ----

    private void displayResults(CommandSender sender, List<CollapsedEntry> allCollapsed, BlockLogFilter filter) {
        int linesPerPage = filter.limit();
        int currentPage = (filter.offset() / linesPerPage) + 1;
        int totalLines = allCollapsed.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalLines / linesPerPage));

        // Clamp page
        if (currentPage > totalPages) currentPage = totalPages;

        // Slice for current page
        int fromIndex = (currentPage - 1) * linesPerPage;
        int toIndex = Math.min(fromIndex + linesPerPage, totalLines);
        List<CollapsedEntry> pageEntries = allCollapsed.subList(fromIndex, toIndex);

        // Header
        sender.sendMessage(Component.text("=== Block Log Query ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("Results: " + totalLines + " entries | Page " + currentPage + "/" + totalPages, NamedTextColor.YELLOW));
        String queryString = filter.rawArgs() != null ? filter.rawArgs() : "no filters";
        sender.sendMessage(Component.text("Filters: " + queryString, NamedTextColor.GRAY));

        if (pageEntries.isEmpty()) {
            sender.sendMessage(Component.text("No block changes found matching your filters.", NamedTextColor.GRAY));
            return;
        }

        for (CollapsedEntry entry : pageEntries) {
            String blockName = entry.material.startsWith("minecraft:") ? entry.material.substring(10) : entry.material;
            String action = getActionSymbol(entry.type);
            NamedTextColor actionColor = getActionColor(entry.type);
            String playerName = entry.playerName != null ? entry.playerName : "System";
            String time = TIME_FORMAT.format(entry.firstTimestamp.toInstant());

            var lineBuilder = Component.text()
                    .append(Component.text(time, NamedTextColor.DARK_GRAY))
                    .append(Component.text(" " + action + " ", actionColor, TextDecoration.BOLD))
                    .append(Component.text(blockName, NamedTextColor.WHITE));

            if (entry.count > 1) {
                lineBuilder.append(Component.text(" ×" + entry.count, NamedTextColor.GOLD));
            }

            // Show coords: single block or area
            if (entry.count == 1) {
                lineBuilder.append(Component.text(" (" + entry.firstX + "," + entry.firstY + "," + entry.firstZ + ") ", NamedTextColor.GRAY));
            } else {
                lineBuilder.append(Component.text(" ", NamedTextColor.GRAY));
            }

            lineBuilder.append(Component.text(playerName, NamedTextColor.AQUA));

            // Hover with details
            var hoverBuilder = Component.text()
                    .append(Component.text(entry.count + " change(s)", NamedTextColor.YELLOW));
            if (entry.count > 1) {
                hoverBuilder.append(Component.newline())
                        .append(Component.text("Area: (" + entry.minX + "," + entry.minY + "," + entry.minZ
                                + ") to (" + entry.maxX + "," + entry.maxY + "," + entry.maxZ + ")", NamedTextColor.GRAY));
            }
            Region region = Factions.getInstance().getRegionManager().getRegionById(entry.regionId);
            String regionName = "<no region>";
            if (region != null) {
                regionName = region.getName();
            }
            hoverBuilder.append(Component.newline())
                    .append(Component.text("Region: " + regionName, NamedTextColor.GRAY))
                    .append(Component.newline())
                    .append(Component.text("Click to teleport", NamedTextColor.YELLOW));

            lineBuilder.hoverEvent(HoverEvent.showText(hoverBuilder.build()));
            lineBuilder.clickEvent(ClickEvent.runCommand("/tp " + entry.firstX + " " + entry.firstY + " " + entry.firstZ));

            sender.sendMessage(lineBuilder.build());
        }

        // Pagination controls
        if (totalPages > 1) {
            String rawArgs = filter.rawArgs() != null ? filter.rawArgs() : "";

            var nav = Component.text();

            if (currentPage > 1) {
                nav.append(Component.text(" [← Prev] ", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/f " + BlockLogCommand.LABEL + " query " + rawArgs + " page:" + (currentPage - 1))));
            }

            nav.append(Component.text(" Page " + currentPage + "/" + totalPages + " ", NamedTextColor.YELLOW));

            if (currentPage < totalPages) {
                nav.append(Component.text(" [Next →] ", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/f " + BlockLogCommand.LABEL + " query " + rawArgs + " page:" + (currentPage + 1))));
            }

            sender.sendMessage(nav.build());
        }
    }

    // ---- Collapsing ----

    /**
     * Collapses consecutive block changes with the same type, material and player
     * into single entries with a count.
     */
    private List<CollapsedEntry> collapseEntries(List<BlockChange> changes, BlockPalette palette) {
        List<CollapsedEntry> result = new ArrayList<>();

        CollapsedEntry current = null;

        for (BlockChange change : changes) {
            boolean showNew = change.changeType() == ChangeType.PLAYER_PLACE;
            int paletteId = showNew ? change.newBlockId() : change.oldBlockId();
            String material = palette.getMaterial(paletteId);
            String playerName = change.causeName();

            if (current != null
                    && current.type == change.changeType()
                    && current.material.equals(material)
                    && java.util.Objects.equals(current.playerName, playerName)) {
                // Same group — merge
                current.count++;
                current.minX = Math.min(current.minX, change.x());
                current.minY = Math.min(current.minY, change.y());
                current.minZ = Math.min(current.minZ, change.z());
                current.maxX = Math.max(current.maxX, change.x());
                current.maxY = Math.max(current.maxY, change.y());
                current.maxZ = Math.max(current.maxZ, change.z());
            } else {
                // New group
                if (current != null) result.add(current);
                current = new CollapsedEntry();
                current.type = change.changeType();
                current.material = material;
                current.playerName = playerName;
                current.regionId = change.regionId();
                current.firstTimestamp = change.timestamp();
                current.firstX = change.x();
                current.firstY = change.y();
                current.firstZ = change.z();
                current.minX = change.x();
                current.minY = change.y();
                current.minZ = change.z();
                current.maxX = change.x();
                current.maxY = change.y();
                current.maxZ = change.z();
                current.count = 1;
            }
        }
        if (current != null) result.add(current);

        return result;
    }

    private static class CollapsedEntry {
        ChangeType type;
        String material;
        String playerName;
        int regionId;
        java.sql.Timestamp firstTimestamp;
        int firstX, firstY, firstZ;
        int minX, minY, minZ;
        int maxX, maxY, maxZ;
        int count;
    }

    // ---- Helpers ----

    private String getActionSymbol(ChangeType type) {
        return switch (type) {
            case PLAYER_PLACE -> "+";
            case PLAYER_BREAK -> "-";
            case EXPLOSION -> "💥";
            case RENATURATION -> "~";
            default -> "?";
        };
    }

    private NamedTextColor getActionColor(ChangeType type) {
        return switch (type) {
            case PLAYER_PLACE -> NamedTextColor.GREEN;
            case PLAYER_BREAK -> NamedTextColor.RED;
            case EXPLOSION -> NamedTextColor.DARK_RED;
            case RENATURATION -> NamedTextColor.YELLOW;
            default -> NamedTextColor.GRAY;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length < 2) return List.of();
        String current = args[args.length - 1].toLowerCase();

        List<String> suggestions = new ArrayList<>();

        if (!current.contains(":")) {
            List<String> keys = List.of("player:", "area:", "sel", "since:", "before:", "block:", "region:", "type:", "world:", "limit:", "page:", "asc", "desc");
            suggestions.addAll(keys.stream().filter(k -> k.startsWith(current)).toList());
        } else {
            int colonIdx = current.indexOf(':');
            String key = current.substring(0, colonIdx);
            String valuePart = current.substring(colonIdx + 1);

            switch (key) {
                case "player", "p" -> suggestions.addAll(getTabPlayers(valuePart).stream()
                        .map(n -> key + ":" + n).toList());
                case "since", "before", "time" -> {
                    for (String t : List.of("5m", "30m", "1h", "6h", "12h", "24h", "3d", "7d", "14d", "30d")) {
                        if (t.startsWith(valuePart)) suggestions.add(key + ":" + t);
                    }
                }
                case "block", "b" -> {
                    for (Material m : Material.values()) {
                        if (m.isBlock() && m.name().toLowerCase().startsWith(valuePart.toLowerCase())) {
                            suggestions.add(key + ":" + m.name());
                            if (suggestions.size() > 20) break;
                        }
                    }
                }
                case "region", "r" -> suggestions.addAll(getTabRegions(valuePart).stream()
                        .map(n -> key + ":" + n).toList());
                case "type" -> {
                    for (ChangeType ct : ChangeType.values()) {
                        if (ct.name().toLowerCase().startsWith(valuePart.toLowerCase())) {
                            suggestions.add(key + ":" + ct.name());
                        }
                    }
                }
                case "area", "radius" -> {
                    for (String r : List.of("10", "20", "30", "50", "100")) {
                        if (r.startsWith(valuePart)) suggestions.add(key + ":" + r);
                    }
                }
                case "limit" -> {
                    for (String l : List.of("10", "15", "20", "50", "100")) {
                        if (l.startsWith(valuePart)) suggestions.add(key + ":" + l);
                    }
                }
            }
        }

        return suggestions;
    }
}
