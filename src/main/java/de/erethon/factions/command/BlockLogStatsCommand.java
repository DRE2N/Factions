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
 * Command to show block log statistics.
 *
 * @author Malfrador
 */
public class BlockLogStatsCommand extends FCommand {

    public BlockLogStatsCommand() {
        setCommand("stats");
        setAliases("statistics");
        setMinMaxArgs(1, 99);
        setConsoleCommand(true);
        setPermission("factions.admin.blocklog.stats");
        setFUsage("/" + BlockLogCommand.LABEL + " " + getCommand() + " <region>");
        setDescription("Show block log statistics for a region");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Region region = getRegion(args[1]);

        long count = Factions.getInstance().getBlockLogManager()
                .getBlockLogDAO()
                .countRegionChanges(region.getId());

        int paletteSize = Factions.getInstance().getBlockLogManager()
                .getPalette()
                .size();

        sender.sendMessage(Component.text("=== Block Log Stats: " + region.getName() + " ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Total logged changes: " + count, NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Unique block states (palette): " + paletteSize, NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabRegions(args[1]);
        }
        return null;
    }
}

