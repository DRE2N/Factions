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
 * Command to get information about a region's baseline.
 *
 * @author Malfrador
 */
public class BaselineInfoCommand extends FCommand {

    public BaselineInfoCommand() {
        setCommand("info");
        setAliases("i");
        setMinMaxArgs(1, 99);
        setConsoleCommand(true);
        setPermission("factions.admin.baseline.info");
        setFUsage("/" + BaselineCommand.LABEL + " " + getCommand() + " <region>");
        setDescription("Shows baseline information for a region");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Region region = getRegion(args[1]);

        boolean hasBaseline = Factions.getInstance().getBaselineManager().hasBaseline(region);

        if (!hasBaseline) {
            sender.sendMessage(Component.text("Region " + region.getName() + " has no baseline captured", NamedTextColor.RED));
            return;
        }

        int currentVersion = Factions.getInstance().getBaselineManager().getCurrentVersion(region);

        sender.sendMessage(Component.text("=== Baseline Info: " + region.getName() + " ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Current Version: v" + currentVersion, NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Has Baseline: Yes", NamedTextColor.GREEN));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabRegions(args[1]);
        }
        return null;
    }
}

