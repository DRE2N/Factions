package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.region.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Discards the active map update session for a region without applying it.
 * <p>
 * Usage: {@code /f baseline session cancel <region>}
 *
 * @author Malfrador
 */
public class BaselineSessionCancelCommand extends FCommand {

    public BaselineSessionCancelCommand() {
        setCommand("cancel");
        setAliases("discard", "abort");
        setMinMaxArgs(1, 1);
        setConsoleCommand(true);
        setPermission("factions.admin.baseline.session");
        setFUsage(BaselineCommand.LABEL + " session cancel <region>");
        setDescription("Discards the active map update session for a region without applying it");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Region region = getRegion(args[1]);

        assure(plugin.getMapUpdateSessionManager() != null, "Map update session manager is not initialized.");
        assure(plugin.getMapUpdateSessionManager().hasSession(region),
                "No active map update session for region " + region.getName() + ".");

        plugin.getMapUpdateSessionManager().removeSession(region);

        sender.sendMessage(Component.text(
                "Map update session for region " + region.getName() + " has been cancelled.",
                NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(
                "The staged baseline schematic is still on disk. "
                        + "Run '/f baseline session start " + region.getName()
                        + "' to reopen a session.", NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabRegions(args[1]);
        }
        return null;
    }
}

