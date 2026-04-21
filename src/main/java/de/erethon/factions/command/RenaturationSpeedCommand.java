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
 * Command to adjust renaturation speed for testing/debugging.
 *
 * @author Malfrador
 */
public class RenaturationSpeedCommand extends FCommand {

    public RenaturationSpeedCommand() {
        setCommand("speed");
        setMinMaxArgs(2, 2);
        setConsoleCommand(true);
        setPermission("factions.admin.renaturation.speed");
        setFUsage("/" + FCommandCache.LABEL + " " + RenaturationCommand.LABEL + " " + getCommand() + " <region> <multiplier>");
        setDescription("Set renaturation speed multiplier for a region (for testing)");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Region region = getRegion(args[1]);

        double multiplier;
        try {
            multiplier = Double.parseDouble(args[2]);
            if (multiplier <= 0) {
                sender.sendMessage(Component.text("Multiplier must be positive!", NamedTextColor.RED));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid multiplier: " + args[2], NamedTextColor.RED));
            return;
        }

        if (Factions.getInstance().getRenaturationService() == null) {
            sender.sendMessage(Component.text("Renaturation system is not available!", NamedTextColor.RED));
            return;
        }

        Factions.getInstance().getRenaturationService().setSpeedMultiplier(region.getId(), multiplier);

        sender.sendMessage(Component.text("Set renaturation speed multiplier for ", NamedTextColor.GREEN)
                .append(Component.text(region.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" to ", NamedTextColor.GREEN))
                .append(Component.text(String.format("%.2fx", multiplier), NamedTextColor.GOLD)));

        if (multiplier >= 1000) {
            sender.sendMessage(Component.text("Warning: Very high multiplier may cause lag!", NamedTextColor.RED));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabRegions(args[1]);
        }
        if (args.length == 3) {
            return List.of("1", "10", "100", "1000");
        }
        return null;
    }
}

