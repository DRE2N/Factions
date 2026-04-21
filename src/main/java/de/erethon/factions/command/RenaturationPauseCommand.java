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
 * Command to pause renaturation for a region.
 *
 * @author Malfrador
 */
public class RenaturationPauseCommand extends FCommand {

    public RenaturationPauseCommand() {
        setCommand("pause");
        setAliases("stop");
        setMinMaxArgs(1, 1);
        setConsoleCommand(true);
        setPermission("factions.admin.renaturation.pause");
        setFUsage("/" + FCommandCache.LABEL + " " + RenaturationCommand.LABEL + " " + getCommand() + " <region>");
        setDescription("Pauses/stops renaturation for a region");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Region region = getRegion(args[1]);

        Factions.getInstance().getRenaturationService().pauseRenaturation(region);
        sender.sendMessage(Component.text("Paused renaturation for region " + region.getName(), NamedTextColor.GREEN));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabRegions(args[1]);
        }
        return null;
    }
}

