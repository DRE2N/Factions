package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.display.RegionMapDisplay;
import de.erethon.factions.util.display.RegionMapDisplay.ZoomLevel;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Command to display a map of nearby regions with alliance-based coloring.
 *
 * @author Malfrador
 */
public class RegionMapCommand extends FCommand {

    public RegionMapCommand() {
        setCommand("map");
        setAliases("m");
        setMinMaxArgs(0, 2);
        setConsoleCommand(false);
        setPermissionFromName();
        setFUsage(getCommand() + " [close|zoom <level>|+|-|<region>]");
        setDescription("Zeigt eine Karte der umliegenden Regionen");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(player);

        // Handle close argument
        if (args.length >= 2 && args[1].equalsIgnoreCase("close")) {
            if (RegionMapDisplay.hasActiveDisplay(player.getUniqueId())) {
                RegionMapDisplay.removeExisting(player.getUniqueId());
                MessageUtil.sendMessage(sender, "&7Map closed.");
            } else {
                MessageUtil.sendMessage(sender, "&cNo map is currently open.");
            }
            return;
        }

        // Handle zoom commands on existing map
        if (args.length >= 2 && RegionMapDisplay.hasActiveDisplay(player.getUniqueId())) {
            RegionMapDisplay activeDisplay = RegionMapDisplay.getActiveDisplay(player.getUniqueId());

            if (args[1].equalsIgnoreCase("+") || args[1].equalsIgnoreCase("in")) {
                activeDisplay.zoomIn();
                MessageUtil.sendMessage(sender, "&aZoomed in to: &e" + activeDisplay.getZoomLevel().getDisplayName());
                return;
            }

            if (args[1].equalsIgnoreCase("-") || args[1].equalsIgnoreCase("out")) {
                activeDisplay.zoomOut();
                MessageUtil.sendMessage(sender, "&aZoomed out to: &e" + activeDisplay.getZoomLevel().getDisplayName());
                return;
            }

            if (args[1].equalsIgnoreCase("zoom") && args.length >= 3) {
                ZoomLevel level = parseZoomLevel(args[2]);
                if (level == null) {
                    MessageUtil.sendMessage(sender, "&cInvalid zoom level. Use: close, medium, far, world");
                    return;
                }
                activeDisplay.setZoomLevel(level);
                MessageUtil.sendMessage(sender, "&aZoom set to: &e" + level.getDisplayName());
                return;
            }
        }

        // Toggle if map already open and no region specified
        if (args.length == 1 && RegionMapDisplay.hasActiveDisplay(player.getUniqueId())) {
            RegionMapDisplay.removeExisting(player.getUniqueId());
            MessageUtil.sendMessage(sender, "&7Map closed.");
            return;
        }

        // Determine zoom level
        ZoomLevel zoomLevel = ZoomLevel.CLOSE;
        Region targetRegion = null;

        if (args.length >= 2) {
            // Check if it's a zoom level
            ZoomLevel parsedZoom = parseZoomLevel(args[1]);
            if (parsedZoom != null) {
                zoomLevel = parsedZoom;
            } else {
                // Try to parse as region
                targetRegion = getRegionByIdOrName(args[1]);
                if (targetRegion == null) {
                    MessageUtil.sendMessage(sender, "&cRegion not found: " + args[1]);
                    return;
                }
            }
        }

        RegionMapDisplay mapDisplay;
        if (targetRegion != null) {
            mapDisplay = new RegionMapDisplay(plugin, player, fPlayer, targetRegion);
        } else {
            mapDisplay = new RegionMapDisplay(plugin, player, fPlayer, zoomLevel);
        }

        mapDisplay.show();
        MessageUtil.sendMessage(sender, "&aMap opened. Use &e/f map + &aor &e/f map - &ato zoom, &e/f map &ato close.");
    }

    private ZoomLevel parseZoomLevel(String input) {
        return switch (input.toLowerCase()) {
            case "detail", "0", "d" -> ZoomLevel.DETAIL;
            case "very_close", "1", "vc" -> ZoomLevel.VERY_CLOSE;
            case "close", "2", "c" -> ZoomLevel.CLOSE;
            case "medium", "3", "m" -> ZoomLevel.MEDIUM;
            case "far", "4", "f" -> ZoomLevel.FAR;
            case "world", "5", "w" -> ZoomLevel.WORLD;
            default -> null;
        };
    }

    private Region getRegionByIdOrName(String target) {
        try {
            int regionId = Integer.parseInt(target);
            return plugin.getRegionManager().getRegionById(regionId);
        } catch (NumberFormatException ignored) {
            return plugin.getRegionManager().getRegionByName(target);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> completions = new ArrayList<>();
            completions.add("close");
            completions.add("+");
            completions.add("-");
            completions.add("zoom");
            completions.addAll(Arrays.asList("detail", "very_close", "close", "medium", "far", "world"));
            completions.addAll(getTabRegions(args[1]));
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("zoom")) {
            return Arrays.asList("detail", "very_close", "close", "medium", "far", "world").stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .toList();
        }
        return null;
    }
}
