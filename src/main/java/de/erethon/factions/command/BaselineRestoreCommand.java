package de.erethon.factions.command;

import de.erethon.factions.Factions;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.region.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Command to restore a region to its baseline state.
 *
 * @author Malfrador
 */
public class BaselineRestoreCommand extends FCommand {

    public BaselineRestoreCommand() {
        setCommand("restore");
        setAliases("r", "load");
        setMinMaxArgs(1, 99);
        setConsoleCommand(true);
        setPermission("factions.admin.baseline.restore");
        setFUsage("/" + BaselineCommand.LABEL + " " + getCommand() + " <region> [version]");
        setDescription("Restores a region to its baseline state");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Region region = getRegion(args[1]);

        sender.sendMessage(Component.text("Restoring baseline for region " + region.getName() + "...", NamedTextColor.YELLOW));

        if (args.length > 2) {
            try {
                int version = Integer.parseInt(args[2]);
                Factions.getInstance().getBaselineManager().restoreBaseline(region, version).thenAccept(success -> {
                    if (success) {
                        sender.sendMessage(Component.text("Successfully restored baseline v" + version + " for region " + region.getName(), NamedTextColor.GREEN));
                    } else {
                        sender.sendMessage(Component.text("Failed to restore baseline for region " + region.getName(), NamedTextColor.RED));
                    }
                });
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid version number: " + args[2], NamedTextColor.RED));
            }
        } else {
            Factions.getInstance().getBaselineManager().restoreCurrentBaseline(region).thenAccept(success -> {
                if (success) {
                    sender.sendMessage(Component.text("Successfully restored current baseline for region " + region.getName(), NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Failed to restore baseline for region " + region.getName(), NamedTextColor.RED));
                }
            });
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

