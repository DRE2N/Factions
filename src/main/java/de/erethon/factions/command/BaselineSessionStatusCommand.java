package de.erethon.factions.command;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import de.erethon.factions.blocklog.mapupdate.MapUpdateSession;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.region.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

/**
 * Displays the current state of a map update session for a region.
 * <p>
 * Usage: {@code /f baseline session status <region>}
 *
 * @author Malfrador
 */
public class BaselineSessionStatusCommand extends FCommand {

    public BaselineSessionStatusCommand() {
        setCommand("status");
        setAliases("info", "i");
        setMinMaxArgs(1, 1);
        setConsoleCommand(true);
        setPermission("factions.admin.baseline.session");
        setFUsage(BaselineCommand.LABEL + " session status <region>");
        setDescription("Shows the current state of a map update session");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Region region = getRegion(args[1]);

        assure(plugin.getMapUpdateSessionManager() != null, "Map update session manager is not initialized.");
        assure(plugin.getMapUpdateSessionManager().hasSession(region),
                "No active map update session for region " + region.getName() + ".");

        MapUpdateSession session = plugin.getMapUpdateSessionManager().getSession(region);

        sender.sendMessage(Component.text("=== Map Update Session: " + region.getName() + " ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("New Baseline Version: v" + session.getNewBaselineVersion(), NamedTextColor.YELLOW));

        if (session.hasIncludeOnlyZones()) {
            sender.sendMessage(Component.text("Mode: INCLUDE-ONLY (only listed zones will be updated)", NamedTextColor.AQUA));
        } else {
            sender.sendMessage(Component.text("Mode: FULL REGION (entire region will be updated)", NamedTextColor.AQUA));
        }

        // Protected zones
        Map<String, MapUpdateSession.ZoneEntry> protectedZones = session.getProtectedZones();
        if (protectedZones.isEmpty()) {
            sender.sendMessage(Component.text("Protected Zones: none", NamedTextColor.GRAY));
        } else {
            sender.sendMessage(Component.text("Protected Zones (" + protectedZones.size() + "):", NamedTextColor.RED));
            for (Map.Entry<String, MapUpdateSession.ZoneEntry> entry : protectedZones.entrySet()) {
                sender.sendMessage(Component.text(
                        "  - " + entry.getKey() + ": " + formatZone(entry.getValue()),
                        NamedTextColor.RED));
            }
        }

        // Include-only zones
        Map<String, MapUpdateSession.ZoneEntry> includeZones = session.getIncludeOnlyZones();
        if (!includeZones.isEmpty()) {
            sender.sendMessage(Component.text("Include-Only Zones (" + includeZones.size() + "):", NamedTextColor.GREEN));
            for (Map.Entry<String, MapUpdateSession.ZoneEntry> entry : includeZones.entrySet()) {
                sender.sendMessage(Component.text(
                        "  - " + entry.getKey() + ": " + formatZone(entry.getValue()),
                        NamedTextColor.GREEN));
            }
        }

        sender.sendMessage(Component.text("Run '/f baseline session apply " + region.getName()
                + "' to execute the update.", NamedTextColor.GRAY));
    }

    private String formatZone(MapUpdateSession.ZoneEntry entry) {
        CuboidRegion zone = entry.region();
        BlockVector3 min = zone.getMinimumPoint();
        BlockVector3 max = zone.getMaximumPoint();
        String coords = min.x() + "," + min.y() + "," + min.z() + " → " + max.x() + "," + max.y() + "," + max.z();
        if (entry.yShift() != 0) {
            coords += " [Y-shift: " + (entry.yShift() > 0 ? "+" : "") + entry.yShift() + "]";
        }
        return coords;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) return getTabRegions(args[1]);
        return null;
    }
}
