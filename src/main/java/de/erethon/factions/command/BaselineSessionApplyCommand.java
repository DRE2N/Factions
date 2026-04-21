package de.erethon.factions.command;

import de.erethon.factions.blocklog.mapupdate.MapUpdateSession;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.region.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Executes the map update session for a region: pastes the staged baseline and replays
 * all player block changes. The session is removed after a successful apply.
 * <p>
 * Usage: {@code /f baseline session apply <region>}
 *
 * @author Malfrador
 */
public class BaselineSessionApplyCommand extends FCommand {

    public BaselineSessionApplyCommand() {
        setCommand("apply");
        setAliases("execute", "run");
        setMinMaxArgs(1, 1);
        setConsoleCommand(true);
        setPermission("factions.admin.baseline.session");
        setFUsage(BaselineCommand.LABEL + " session apply <region>");
        setDescription("Applies the staged map update for a region (pastes baseline + replays player changes)");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Region region = getRegion(args[1]);

        assure(plugin.getBaselineManager() != null, "Block logging system is not initialized.");
        assure(plugin.getMapUpdateSessionManager() != null, "Map update session manager is not initialized.");
        assure(plugin.getMapUpdateSessionManager().hasSession(region),
                "No active map update session for region " + region.getName()
                        + ". Run '/f baseline update " + region.getName() + "' first.");

        MapUpdateSession session = plugin.getMapUpdateSessionManager().getSession(region);

        sender.sendMessage(Component.text(
                "Applying map update for region " + region.getName()
                        + " (baseline v" + session.getNewBaselineVersion() + ")...",
                NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(
                "Protected zones: " + session.getProtectedZones().size()
                        + " | Include-only zones: " + session.getIncludeOnlyZones().size(),
                NamedTextColor.GRAY));

        plugin.getBaselineManager().applyMapUpdate(session, sender).thenRun(() -> {
            // Remove the session once it has been fully applied
            plugin.getMapUpdateSessionManager().removeSession(region);
        }).exceptionally(e -> {
            sender.sendMessage(Component.text(
                    "[MapUpdate] Unexpected error during apply: " + e.getMessage(), NamedTextColor.RED));
            return null;
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabRegions(args[1]);
        }
        return null;
    }
}

