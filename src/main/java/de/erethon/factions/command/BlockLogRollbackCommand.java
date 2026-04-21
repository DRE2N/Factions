package de.erethon.factions.command;

import de.erethon.factions.Factions;
import de.erethon.factions.blocklog.BlockPalette;
import de.erethon.factions.blocklog.filter.BlockLogFilter;
import de.erethon.factions.blocklog.filter.BlockLogFilterParser;
import de.erethon.factions.blocklog.filter.BlockLogQueryBuilder;
import de.erethon.factions.blocklog.model.BlockChange;
import de.erethon.factions.blocklog.model.ChangeType;
import de.erethon.factions.blocklog.util.BlockDataSerializer;
import de.erethon.factions.command.logic.FCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Command to rollback block changes with flexible filters.
 * <p>
 * Usage: /f blocklog rollback [filters...]
 * <p>
 * Shows a visual preview using {@link Player#sendBlockChange} before confirming.
 *
 * @author Malfrador
 */
public class BlockLogRollbackCommand extends FCommand {

    private static final Map<UUID, RollbackSession> pendingSessions = new HashMap<>();

    public BlockLogRollbackCommand() {
        setCommand("rollback");
        setAliases("rb");
        setMinMaxArgs(1, 99);
        setConsoleCommand(false);
        setPermission("factions.admin.blocklog.rollback");
        setFUsage(BlockLogCommand.LABEL + " " + "rollback" + " [filters...] OR confirm/cancel");
        setDescription("Rollback block changes. Uses same filters as query.");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return;
        }

        // Handle confirm / cancel
        if (args.length >= 2) {
            String sub = args[1].toLowerCase();
            if (sub.equals("confirm")) {
                confirmRollback(player);
                return;
            }
            if (sub.equals("cancel")) {
                cancelPreview(player);
                return;
            }
        }

        // Parse filters and show preview
        // Override limit to a high number for rollback — we want all matching changes
        BlockLogFilter filter = BlockLogFilterParser.parse(sender, args, 1);
        // For rollback, increase limit significantly (we need all results)
        filter = new BlockLogFilter(
                filter.players(), filter.excludedPlayers(),
                filter.radius(), filter.centerX(), filter.centerY(), filter.centerZ(), filter.centerWorld(),
                filter.selMinX(), filter.selMinY(), filter.selMinZ(),
                filter.selMaxX(), filter.selMaxY(), filter.selMaxZ(), filter.selWorld(),
                filter.world(), filter.since(), filter.before(),
                filter.blocks(), filter.excludedBlocks(),
                filter.regionName(), filter.regionId(),
                filter.changeTypes(), 50000, 0, filter.ascending(), filter.rawArgs()
        );

        player.sendMessage(Component.text("Calculating rollback preview...", NamedTextColor.YELLOW));

        final BlockLogFilter finalFilter = filter;
        Bukkit.getScheduler().runTaskAsynchronously(Factions.getInstance(), () -> {
            try {
                BlockLogQueryBuilder queryBuilder = new BlockLogQueryBuilder(finalFilter);
                List<BlockChange> changes = Factions.getInstance().getBlockLogManager()
                        .getPalette().getJdbi()
                        .withHandle(queryBuilder::execute);

                Bukkit.getScheduler().runTask(Factions.getInstance(), () ->
                        showPreview(player, changes, finalFilter));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(Factions.getInstance(), () ->
                        player.sendMessage(Component.text("Error: " + e.getMessage(), NamedTextColor.RED)));
                e.printStackTrace();
            }
        });
    }

    private void showPreview(Player player, List<BlockChange> changes, BlockLogFilter filter) {
        if (changes.isEmpty()) {
            player.sendMessage(Component.text("No block changes found matching your filters.", NamedTextColor.RED));
            return;
        }

        BlockPalette palette = Factions.getInstance().getBlockLogManager().getPalette();

        // Count by type
        Map<ChangeType, Integer> typeCounts = new HashMap<>();
        for (BlockChange change : changes) {
            typeCounts.merge(change.changeType(), 1, Integer::sum);
        }

        // Send visual preview using sendBlockChange — show what blocks would be restored to
        int previewed = 0;
        List<Location> previewLocations = new ArrayList<>();
        for (BlockChange change : changes) {
            org.bukkit.World world = Bukkit.getWorld(change.worldName());
            if (world == null) continue;

            Location loc = new Location(world, change.x(), change.y(), change.z());

            // Only send block changes for blocks reasonably close to the player (within render distance)
            if (loc.getWorld().equals(player.getWorld()) && loc.distanceSquared(player.getLocation()) < 160 * 160) {
                byte[] oldData = palette.getData(change.oldBlockId());
                BlockData blockData = getBlockDataFromPalette(oldData);
                if (blockData != null) {
                    player.sendBlockChange(loc, blockData);
                    previewLocations.add(loc);
                    previewed++;
                }
            }
        }

        // Store session
        RollbackSession session = new RollbackSession(changes, filter, previewLocations);
        pendingSessions.put(player.getUniqueId(), session);

        // Show summary
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("=== Rollback Preview ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("Total changes to rollback: " + changes.size(), NamedTextColor.YELLOW));
        if (previewed > 0) {
            player.sendMessage(Component.text("Visual preview: " + previewed + " blocks shown nearby", NamedTextColor.AQUA));
        }
        typeCounts.forEach((type, count) ->
                player.sendMessage(Component.text("  " + type + ": " + count, NamedTextColor.GRAY)));

        Component buttons = Component.text()
                .append(Component.text("[Confirm]", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/f " + BlockLogCommand.LABEL + " rollback confirm")))
                .append(Component.text("  "))
                .append(Component.text("[Cancel]", NamedTextColor.RED, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/f " + BlockLogCommand.LABEL + " rollback cancel")))
                .build();
        player.sendMessage(buttons);

        Bukkit.getScheduler().runTaskLater(Factions.getInstance(), () -> {
            RollbackSession expired = pendingSessions.remove(player.getUniqueId());
            if (expired != null) {
                restorePreviewBlocks(player, expired);
                player.sendMessage(Component.text("Rollback preview expired. Preview blocks restored.", NamedTextColor.YELLOW));
            }
        }, 20L * 60 * 5);
    }

    private void confirmRollback(Player player) {
        RollbackSession session = pendingSessions.remove(player.getUniqueId());
        if (session == null) {
            player.sendMessage(Component.text("No pending rollback. Run the command with filters first.", NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("Executing rollback of " + session.changes.size() + " changes...", NamedTextColor.YELLOW));

        // Sort newest first to properly reverse
        List<BlockChange> sorted = session.changes.stream()
                .sorted((a, b) -> b.timestamp().compareTo(a.timestamp()))
                .toList();

        List<Long> changeIds = sorted.stream()
                .map(BlockChange::id)
                .filter(id -> id > 0)
                .distinct()
                .collect(Collectors.toList());

        Bukkit.getScheduler().runTaskAsynchronously(Factions.getInstance(), () -> {
            try {
                int reverted = changeIds.isEmpty() ? 0 : Factions.getInstance().getBlockLogManager()
                        .getBlockLogDAO()
                        .markReverted(changeIds, player.getUniqueId(), player.getName());

                Bukkit.getScheduler().runTask(Factions.getInstance(), () -> {
                    player.sendMessage(Component.text("Marked " + reverted + " log entries as reverted.", NamedTextColor.GRAY));
                    executeRollbackBatches(player, sorted);
                });
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(Factions.getInstance(), () -> {
                    player.sendMessage(Component.text("Failed to persist rollback metadata: " + e.getMessage(), NamedTextColor.RED));
                    executeRollbackBatches(player, sorted);
                });
            }
        });
    }

    private void executeRollbackBatches(Player player, List<BlockChange> sorted) {
        BlockPalette palette = Factions.getInstance().getBlockLogManager().getPalette();
        Map<Integer, Integer> baselineVersionCache = new HashMap<>();

        final int batchSize = 50;
        final int[] state = {0, 0}; // [index, failed]

        Bukkit.getScheduler().runTaskTimer(Factions.getInstance(), task -> {
            int processed = 0;
            while (state[0] < sorted.size() && processed < batchSize) {
                BlockChange change = sorted.get(state[0]);
                try {
                    org.bukkit.World world = Bukkit.getWorld(change.worldName());
                    if (world == null) {
                        state[1]++;
                        state[0]++;
                        processed++;
                        continue;
                    }

                    Location location = new Location(world, change.x(), change.y(), change.z());
                    byte[] oldData = palette.getData(change.oldBlockId());
                    if (oldData != null && oldData.length > 0) {
                        BlockDataSerializer.applyBlockData(location, oldData);
                    } else {
                        location.getBlock().setType(org.bukkit.Material.AIR, false);
                    }

                    int baselineVersion = baselineVersionCache.computeIfAbsent(change.regionId(), regionId ->
                            Factions.getInstance().getBlockLogManager().getRegionBaselineDAO()
                                    .getCurrentVersion(regionId)
                                    .orElse(0));

                    Factions.getInstance().getBlockLogManager().logBlockChange(
                            new BlockChange(
                                    change.regionId(),
                                    change.x(), change.y(), change.z(),
                                    change.worldName(),
                                    change.newBlockId(),
                                    change.oldBlockId(),
                                    change.oldBlockId() == BlockPalette.AIR_ID
                                            ? ChangeType.PLAYER_BREAK
                                            : ChangeType.PLAYER_PLACE,
                                    player.getUniqueId(),
                                    player.getName(),
                                    new java.sql.Timestamp(System.currentTimeMillis()),
                                    baselineVersion
                            )
                    );
                } catch (Exception e) {
                    state[1]++;
                }
                state[0]++;
                processed++;
            }

            if (state[0] >= sorted.size()) {
                task.cancel();
                int rolledBack = state[0] - state[1];
                player.sendMessage(Component.text("Rollback complete!", NamedTextColor.GREEN, TextDecoration.BOLD));
                player.sendMessage(Component.text("Rolled back: " + rolledBack + " | Failed: " + state[1], NamedTextColor.YELLOW));
            } else if (state[0] % 500 == 0) {
                player.sendMessage(Component.text("Progress: " + state[0] + "/" + sorted.size(), NamedTextColor.GRAY));
            }
        }, 0L, 1L);
    }

    private void cancelPreview(Player player) {
        RollbackSession session = pendingSessions.remove(player.getUniqueId());
        if (session == null) {
            player.sendMessage(Component.text("No pending rollback to cancel.", NamedTextColor.RED));
            return;
        }
        restorePreviewBlocks(player, session);
        player.sendMessage(Component.text("Rollback cancelled. Preview blocks restored.", NamedTextColor.YELLOW));
    }

    /**
     * Restores the actual block states for all previewed locations by re-sending the real blocks.
     */
    private void restorePreviewBlocks(Player player, RollbackSession session) {
        for (Location loc : session.previewLocations) {
            if (loc.getWorld() != null && loc.isChunkLoaded()) {
                player.sendBlockChange(loc, loc.getBlock().getBlockData());
            }
        }
    }

    /**
     * Converts palette block data to a Bukkit BlockData for sendBlockChange preview.
     * Returns null if the data cannot be parsed.
     */
    private BlockData getBlockDataFromPalette(byte[] data) {
        if (data == null || data.length == 0) {
            return org.bukkit.Material.AIR.createBlockData();
        }
        try {
            String description = BlockDataSerializer.getBlockDescription(data);
            // NMS BlockState.toString() returns:
            //   "Block{minecraft:stone}" (no properties)
            //   "Block{minecraft:oak_stairs}[facing=east,half=bottom,shape=straight,waterlogged=false]" (with properties)
            // Bukkit.createBlockData() expects:
            //   "minecraft:stone"
            //   "minecraft:oak_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]"
            int braceStart = description.indexOf('{');
            int braceEnd = description.indexOf('}');
            if (braceStart == -1 || braceEnd == -1) {
                return null;
            }
            String blockId = description.substring(braceStart + 1, braceEnd);
            // Append properties if present (they come after the closing brace)
            String properties = description.substring(braceEnd + 1);
            return Bukkit.createBlockData(blockId + properties);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length < 2) return List.of();
        String current = args[args.length - 1].toLowerCase();

        List<String> suggestions = new ArrayList<>();

        if (current.isEmpty() || !current.contains(":")) {
            List<String> keys = List.of("player:", "area:", "sel", "since:", "before:", "block:", "region:", "type:", "world:", "limit:", "asc", "desc", "confirm", "cancel");
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
                    for (org.bukkit.Material m : org.bukkit.Material.values()) {
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
                case "area" -> {
                    for (String r : List.of("10", "20", "30", "50", "100")) {
                        if (r.startsWith(valuePart)) suggestions.add(key + ":" + r);
                    }
                }
            }
        }

        return suggestions;
    }

    private record RollbackSession(
            List<BlockChange> changes,
            BlockLogFilter filter,
            List<Location> previewLocations
    ) {}
}
