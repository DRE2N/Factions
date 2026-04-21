package de.erethon.factions.command;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import de.erethon.factions.blocklog.mapupdate.MapUpdateSession;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.region.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Adds or removes a named protected zone from a map update session.
 * Blocks inside protected zones are not overwritten by the baseline paste, but player
 * changes are still replayed into them (with an optional Y-shift).
 * <p>
 * The zone volume is defined by the executor's current WorldEdit selection.
 * <p>
 * Usage:
 * <ul>
 *   <li>{@code /f baseline session protect add <region> <zone-name> [yShift]}</li>
 *   <li>{@code /f baseline session protect remove <region> <zone-name>}</li>
 * </ul>
 *
 * @author Malfrador
 */
public class BaselineSessionProtectCommand extends FCommand {

    public BaselineSessionProtectCommand() {
        setCommand("protect");
        setMinMaxArgs(3, 4);
        setConsoleCommand(false);
        setPermission("factions.admin.baseline.session");
        setFUsage(BaselineCommand.LABEL + " session protect <add|remove> <region> <zone-name> [yShift]");
        setDescription("Adds or removes a protected zone (WE selection) in a map update session");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        String action = args[1].toLowerCase();
        Region region = getRegion(args[2]);
        String zoneName = args[3];

        assure(plugin.getMapUpdateSessionManager() != null, "Map update session manager is not initialized.");
        assure(plugin.getMapUpdateSessionManager().hasSession(region),
                "No active map update session for region " + region.getName()
                        + ". Run '/f baseline update " + region.getName() + "' first.");

        MapUpdateSession session = plugin.getMapUpdateSessionManager().getSession(region);

        switch (action) {
            case "add" -> {
                assure(sender instanceof Player, "You must be a player to use a WorldEdit selection.");
                Player player = (Player) sender;
                int yShift = args.length >= 5 ? parseInt(args[4]) : 0;
                CuboidRegion zone = getWeSelection(player);
                if (zone == null) return;
                session.addProtectedZone(zoneName, zone, yShift);
                plugin.getMapUpdateSessionManager().visualizeProtectedZone(region, zoneName, zone, player.getWorld());
                String yShiftStr = yShift != 0 ? ", Y-shift: " + (yShift > 0 ? "+" : "") + yShift : "";
                sender.sendMessage(Component.text(
                        "Protected zone '" + zoneName + "' added (" + formatZone(zone) + yShiftStr + ").",
                        NamedTextColor.GREEN));
            }
            case "remove" -> {
                boolean removed = session.removeProtectedZone(zoneName);
                if (removed) {
                    plugin.getMapUpdateSessionManager().removeProtectedZoneVisualization(region, zoneName);
                    sender.sendMessage(Component.text(
                            "Protected zone '" + zoneName + "' removed.", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text(
                            "No protected zone named '" + zoneName + "' found in the session.",
                            NamedTextColor.RED));
                }
            }
            default -> sender.sendMessage(Component.text(
                    "Unknown action '" + action + "'. Use 'add' or 'remove'.", NamedTextColor.RED));
        }
    }

    /** Reads the player's current WorldEdit selection as a CuboidRegion. Returns null and sends an error on failure. */
    private CuboidRegion getWeSelection(Player player) {
        try {
            com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(player);
            com.sk89q.worldedit.LocalSession weSession =
                    WorldEdit.getInstance().getSessionManager().get(wePlayer);
            com.sk89q.worldedit.regions.Region sel = weSession.getSelection(wePlayer.getWorld());
            BlockVector3 min = sel.getMinimumPoint();
            BlockVector3 max = sel.getMaximumPoint();
            return new CuboidRegion(min, max);
        } catch (IncompleteRegionException e) {
            player.sendMessage(Component.text(
                    "You don't have a complete WorldEdit selection. Select an area with WorldEdit first.",
                    NamedTextColor.RED));
            return null;
        }
    }

    private String formatZone(CuboidRegion zone) {
        BlockVector3 min = zone.getMinimumPoint();
        BlockVector3 max = zone.getMaximumPoint();
        return min.x() + "," + min.y() + "," + min.z() + " → " + max.x() + "," + max.y() + "," + max.z();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabList(List.of("add", "remove"), args[1]);
        }
        if (args.length == 3) {
            return getTabRegions(args[2]);
        }
        return null;
    }
}
