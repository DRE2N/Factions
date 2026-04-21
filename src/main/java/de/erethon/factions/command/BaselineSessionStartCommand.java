package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.region.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Manually opens (or re-opens) a map update session for a region at its current staged baseline.
 * Normally a session is opened automatically by {@code /f baseline update}, but this command
 * allows recovering a session if the server restarted after the schematic was already captured.
 * <p>
 * Usage: {@code /f baseline session start <region>}
 *
 * @author Malfrador
 */
public class BaselineSessionStartCommand extends FCommand {

    public BaselineSessionStartCommand() {
        setCommand("start");
        setMinMaxArgs(1, 1);
        setConsoleCommand(true);
        setPermission("factions.admin.baseline.session");
        setFUsage(BaselineCommand.LABEL + " session start <region>");
        setDescription("Opens a map update session for a region using its current baseline version");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Region region = getRegion(args[1]);

        assure(plugin.getBaselineManager() != null, "Block logging system is not initialized.");
        assure(plugin.getMapUpdateSessionManager() != null, "Map update session manager is not initialized.");
        int version = plugin.getBaselineManager().getLatestCapturedVersion(region);
        assure(version > 0,
                "Region " + region.getName() + " has no captured baseline. Run '/f baseline update' first.");

        plugin.getMapUpdateSessionManager().createSession(region, version);

        sender.sendMessage(Component.text(
                "Map update session opened for region " + region.getName()
                        + " at baseline v" + version + ".", NamedTextColor.GREEN));
        sender.sendMessage(Component.text(
                "Use '/f baseline session protect add <region> <name>' to mark protected zones, "
                        + "then '/f baseline session apply <region>' to apply.", NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabRegions(args[1]);
        }
        return null;
    }
}

