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
 * Command to start renaturation for a region.
 *
 * @author Malfrador
 */
public class RenaturationStartCommand extends FCommand {

    public RenaturationStartCommand() {
        setCommand("start");
        setAliases("begin");
        setMinMaxArgs(1, 1);
        setConsoleCommand(true);
        setPermission("factions.admin.renaturation.start");
        setFUsage("/" + FCommandCache.LABEL + " " + RenaturationCommand.LABEL + " " + getCommand() + " <region>");
        setDescription("Starts renaturation (decay) for an abandoned region");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Region region = getRegion(args[1]);

        Factions.getInstance().getRenaturationService().startRenaturation(region);
        sender.sendMessage(Component.text("Started renaturation for region " + region.getName(), NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Structures will gradually decay and disappear from top to bottom", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabRegions(args[1]);
        }
        return null;
    }
}

