package de.erethon.factions.command;

import de.erethon.factions.blocklog.BaselineManager;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.region.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

/**
 * Captures a new baseline for a region from the ErethonStaging world and starts a
 * map update session that can then be configured and applied via {@code /f baseline session}.
 * <p>
 * Usage: {@code /f baseline update <region> [notes...]}
 *
 * @author Malfrador
 */
public class BaselineUpdateCommand extends FCommand {

    public BaselineUpdateCommand() {
        setCommand("update");
        setAliases("u");
        setMinMaxArgs(1, 99);
        setConsoleCommand(true);
        setPermission("factions.admin.baseline.update");
        setFUsage(BaselineCommand.LABEL + " update <region> [notes]");
        setDescription("Captures a new baseline from the staging world and opens a map update session");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Region region = getRegion(args[1]);
        String notes = args.length > 2
                ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : "Map update from ErethonStaging";

        assure(plugin.getBaselineManager() != null, "Block logging system is not initialized.");
        assure(plugin.getMapUpdateSessionManager() != null, "Map update session manager is not initialized.");

        sender.sendMessage(Component.text(
                "Capturing baseline for region " + region.getName() + " from staging world '"
                        + BaselineManager.STAGING_WORLD_NAME + "'...", NamedTextColor.YELLOW));

        plugin.getBaselineManager().captureBaselineFromStagingWorld(region, notes).thenAccept(version -> {
            if (version < 0) {
                sender.sendMessage(Component.text(
                        "Failed to capture staging baseline for region " + region.getName()
                                + ". Check console for details.", NamedTextColor.RED));
                return;
            }

            // Open a map update session for this region at the new version
            plugin.getMapUpdateSessionManager().createSession(region, version);

            sender.sendMessage(Component.text(
                    "Captured staging baseline v" + version + " for region " + region.getName() + ".",
                    NamedTextColor.GREEN));
            sender.sendMessage(Component.text(
                    "Map update session opened. Use '/f baseline session status " + region.getName()
                            + "' to see the session, or '/f baseline session apply " + region.getName()
                            + "' to apply it immediately.",
                    NamedTextColor.YELLOW));
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

