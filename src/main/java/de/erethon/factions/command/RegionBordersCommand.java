package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionBorderCalculator;
import de.erethon.factions.region.RegionCache;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.UUID;

/**
 * Command to recalculate region borders and neighbor relationships.
 *
 * @author Fyreum
 */
public class RegionBordersCommand extends FCommand {

    public RegionBordersCommand() {
        setCommand("recalculateborders");
        setAliases("borders", "rb");
        setMinMaxArgs(0, 1);
        setConsoleCommand(true);
        setPermissionFromName();
        setFUsage(getCommand() + " [world|region]");
        setDescription("Berechnet Regionsgrenzen und Nachbarn neu");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        RegionBorderCalculator calculator = plugin.getRegionBorderCalculator();

        if (args.length == 1) {
            // Recalculate all worlds
            MessageUtil.sendMessage(sender, "&aStarting border recalculation for all worlds...");
            for (RegionCache cache : plugin.getRegionManager()) {
                UUID worldId = cache.getWorldId();
                int totalRegions = cache.getSize();
                MessageUtil.sendMessage(sender, "&7Processing world " + worldId + " (" + totalRegions + " regions)...");

                // Create progress callback that reports every 10%
                final int[] lastReportedPercent = {0};
                RegionBorderCalculator.ProgressCallback progressCallback = (current, total, regionName) -> {
                    int percent = (int) ((current / (double) total) * 100);
                    // Report every 10% or on completion
                    if (percent >= lastReportedPercent[0] + 10 || current == total) {
                        lastReportedPercent[0] = (percent / 10) * 10; // Round down to nearest 10
                        MessageUtil.sendMessage(sender, "&7  Progress: " + current + "/" + total + " (" + percent + "%) - Last: " + regionName);
                    }
                };

                calculator.recalculateWorld(worldId, progressCallback).thenAccept(data -> {
                    if (data != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            MessageUtil.sendMessage(sender, "&aCompleted border calculation for world " + worldId +
                                    " (" + data.regionData.size() + " regions)");
                            if (plugin.getRegionHttpServer() != null) {
                                plugin.getRegionHttpServer().getCache().updateCache();
                            }
                        });
                    }
                });
            }
        } else {
            String target = args[1];

            // Try to parse as region ID first
            Region region = null;
            try {
                int regionId = Integer.parseInt(target);
                region = plugin.getRegionManager().getRegionById(regionId);
            } catch (NumberFormatException ignored) {
                // Not a number, try to find by name
                region = plugin.getRegionManager().getRegionByName(target);
            }

            if (region != null) {
                int regionId = region.getId();
                MessageUtil.sendMessage(sender, "&aRecalculating borders for region " + region.getName() + " (ID: " + regionId + ")...");
                calculator.recalculateRegion(region).thenAccept(data -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        MessageUtil.sendMessage(sender, "&aCompleted border calculation for region " + regionId);
                        MessageUtil.sendMessage(sender, "&7  Polygon vertices: " + data.polygon.size());
                        MessageUtil.sendMessage(sender, "&7  Neighbors: " + data.neighborIds);
                        MessageUtil.sendMessage(sender, "&7  Contiguous: " + data.isContiguous);
                        if (plugin.getRegionHttpServer() != null) {
                            plugin.getRegionHttpServer().getCache().updateCache();
                        }
                    });
                });
                return;
            }

            // Try to parse as world UUID
            try {
                UUID worldId = UUID.fromString(target);
                RegionCache cache = plugin.getRegionManager().getCache(worldId);
                if (cache == null) {
                    MessageUtil.sendMessage(sender, "&cWorld not found: " + worldId);
                    return;
                }

                int totalRegions = cache.getSize();
                MessageUtil.sendMessage(sender, "&aRecalculating borders for world " + worldId + " (" + totalRegions + " regions)...");

                // Create progress callback
                final int[] lastReportedPercent = {0};
                RegionBorderCalculator.ProgressCallback progressCallback = (current, total, regionName) -> {
                    int percent = (int) ((current / (double) total) * 100);
                    if (percent >= lastReportedPercent[0] + 10 || current == total) {
                        lastReportedPercent[0] = (percent / 10) * 10;
                        MessageUtil.sendMessage(sender, "&7  Progress: " + current + "/" + total + " (" + percent + "%) - Last: " + regionName);
                    }
                };

                calculator.recalculateWorld(worldId, progressCallback).thenAccept(data -> {
                    if (data != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            MessageUtil.sendMessage(sender, "&aCompleted border calculation for world " + worldId +
                                    " (" + data.regionData.size() + " regions)");
                            if (plugin.getRegionHttpServer() != null) {
                                plugin.getRegionHttpServer().getCache().updateCache();
                            }
                        });
                    }
                });
            } catch (IllegalArgumentException e) {
                MessageUtil.sendMessage(sender, "&cRegion or world not found: " + target);
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabRegions(args[1]);
        }
        return null;
    }
}

