package de.erethon.factions.command;

import de.erethon.factions.Factions;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.command.logic.FCommandCache;
import de.erethon.factions.region.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Command to capture a baseline for a region.
 *
 * @author Malfrador
 */
public class BaselineCaptureCommand extends FCommand {

    public BaselineCaptureCommand() {
        setCommand("capture");
        setAliases("c", "save");
        setMinMaxArgs(1, 99);
        setConsoleCommand(true);
        setPermission("factions.admin.baseline.capture");
        setFUsage("/" + BaselineCommand.LABEL + " " + getCommand() + " <region> [notes]");
        setDescription("Captures the current state of a region as a baseline");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Region region = getRegion(args[1]);
        String notes = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "Manual capture";

        sender.sendMessage(Component.text("Capturing baseline for region " + region.getName() + "...", NamedTextColor.YELLOW));

        Factions.getInstance().getBaselineManager().captureBaseline(region, notes).thenAccept(version -> {
            if (version > 0) {
                sender.sendMessage(Component.text("Successfully captured baseline v" + version + " for region " + region.getName(), NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("Failed to capture baseline for region " + region.getName(), NamedTextColor.RED));
            }
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

