package de.erethon.factions.blocklog.filter;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import de.erethon.factions.Factions;
import de.erethon.factions.blocklog.model.ChangeType;
import de.erethon.factions.region.Region;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;

/**
 * Parses command arguments into a {@link BlockLogFilter}.
 *
 * @author Malfrador
 */
public class BlockLogFilterParser {

    /**
     * Parses args (starting at startIndex) into a filter.
     */
    public static @NotNull BlockLogFilter parse(@NotNull CommandSender sender, @NotNull String[] args, int startIndex) {
        BlockLogFilter.Builder builder = BlockLogFilter.builder();
        int page = 1;

        // Preserve raw args (excluding page:) for pagination links
        StringBuilder rawArgsBuilder = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (args[i].toLowerCase().startsWith("page:")) continue;
            if (!rawArgsBuilder.isEmpty()) rawArgsBuilder.append(" ");
            rawArgsBuilder.append(args[i]);
        }
        builder.rawArgs(rawArgsBuilder.toString());

        for (int i = startIndex; i < args.length; i++) {
            String arg = args[i];

            // Handle standalone keywords
            if (arg.equalsIgnoreCase("asc")) {
                builder.ascending(true);
                continue;
            }
            if (arg.equalsIgnoreCase("desc")) {
                builder.ascending(false);
                continue;
            }
            if (arg.equalsIgnoreCase("sel") || arg.equalsIgnoreCase("selection")) {
                applyWorldEditSelection(sender, builder);
                continue;
            }

            // Handle key:value pairs
            int colonIdx = arg.indexOf(':');
            if (colonIdx == -1) {
                // Unknown standalone arg, skip
                continue;
            }

            String key = arg.substring(0, colonIdx).toLowerCase();
            String value = arg.substring(colonIdx + 1);

            if (value.isEmpty()) {
                continue;
            }

            switch (key) {
                case "player", "p" -> {
                    for (String name : value.split(",")) {
                        if (!name.isEmpty()) builder.player(name);
                    }
                }
                case "area", "radius" -> {
                    if (sender instanceof Player p) {
                        int radius = parseIntSafe(value, 20);
                        builder.area(radius,
                                p.getLocation().getBlockX(),
                                p.getLocation().getBlockY(),
                                p.getLocation().getBlockZ(),
                                p.getWorld().getName());
                    }
                }
                case "since", "time" -> {
                    Duration d = parseTimeframe(value);
                    if (d != null) builder.since(Instant.now().minus(d));
                }
                case "before" -> {
                    Duration d = parseTimeframe(value);
                    if (d != null) builder.before(Instant.now().minus(d));
                }
                case "block", "b" -> {
                    for (String mat : value.split(",")) {
                        if (!mat.isEmpty()) builder.block(mat);
                    }
                }
                case "world", "w" -> builder.world(value);
                case "region", "r" -> {
                    Region region = Factions.getInstance().getRegionManager().getRegionByName(value);
                    if (region != null) {
                        builder.region(region.getName());
                        builder.regionId(region.getId());
                    }
                }
                case "type" -> {
                    for (String t : value.split(",")) {
                        try {
                            builder.changeType(ChangeType.valueOf(t.toUpperCase()));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                case "limit" -> builder.limit(parseIntSafe(value, 100));
                case "page" -> page = Math.max(1, parseIntSafe(value, 1));
            }
        }

        // Default: if no time filter at all, default to last 24h
        BlockLogFilter preliminary = builder.build();
        if (preliminary.since() == null && preliminary.before() == null) {
            builder.since(Instant.now().minus(Duration.ofHours(24)));
        }

        // Apply pagination: offset = (page - 1) * limit
        if (page > 1) {
            builder.offset((page - 1) * preliminary.limit());
        }

        return builder.build();
    }

    /**
     * Applies the player's current WorldEdit selection as a spatial filter.
     */
    private static void applyWorldEditSelection(@NotNull CommandSender sender, @NotNull BlockLogFilter.Builder builder) {
        if (!(sender instanceof Player player)) {
            return;
        }
        try {
            com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(player);
            com.sk89q.worldedit.session.SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
            com.sk89q.worldedit.LocalSession session = sessionManager.get(wePlayer);
            com.sk89q.worldedit.regions.Region selection = session.getSelection(wePlayer.getWorld());

            BlockVector3 min = selection.getMinimumPoint();
            BlockVector3 max = selection.getMaximumPoint();

            builder.selection(
                    min.x(), min.y(), min.z(),
                    max.x(), max.y(), max.z(),
                    player.getWorld().getName()
            );
        } catch (IncompleteRegionException e) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(
                    "No WorldEdit selection found. Select a region first.", net.kyori.adventure.text.format.NamedTextColor.RED));
        }
    }

    /**
     * Parses a timeframe string like "2h", "7d", "30m", "2w".
     */
    public static Duration parseTimeframe(String timeframe) {
        try {
            if (timeframe == null || timeframe.length() < 2) return null;
            String unit = timeframe.substring(timeframe.length() - 1).toLowerCase();
            long value = Long.parseLong(timeframe.substring(0, timeframe.length() - 1));
            return switch (unit) {
                case "m" -> Duration.ofMinutes(value);
                case "h" -> Duration.ofHours(value);
                case "d" -> Duration.ofDays(value);
                case "w" -> Duration.ofDays(value * 7);
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private static int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}

