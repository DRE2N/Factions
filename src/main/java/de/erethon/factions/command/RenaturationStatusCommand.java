package de.erethon.factions.command;

import de.erethon.factions.Factions;
import de.erethon.factions.blocklog.model.RenaturationProgress;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.command.logic.FCommandCache;
import de.erethon.factions.region.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Optional;

/**
 * Command to check renaturation status for a region.
 *
 * @author Malfrador
 */
public class RenaturationStatusCommand extends FCommand {

    public RenaturationStatusCommand() {
        setCommand("status");
        setAliases("info");
        setMinMaxArgs(0, 1);
        setConsoleCommand(true);
        setPermission("factions.admin.renaturation.status");
        setFUsage("/" + FCommandCache.LABEL + " " + RenaturationCommand.LABEL + " " + getCommand() + " [region]");
        setDescription("Shows renaturation status for a region or all active processes");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        if (args.length > 1) {
            // Show status for specific region
            Region region = getRegion(args[1]);
            showRegionStatus(sender, region);
        } else {
            // Show all active renaturation processes
            showAllActive(sender);
        }
    }

    private void showRegionStatus(CommandSender sender, Region region) {
        Optional<RenaturationProgress> progressOpt = Factions.getInstance()
                .getBlockLogManager()
                .getRenaturationProgressDAO()
                .get(region.getId());

        if (progressOpt.isEmpty()) {
            sender.sendMessage(Component.text("No active renaturation for region " + region.getName(), NamedTextColor.YELLOW));
            return;
        }

        RenaturationProgress progress = progressOpt.get();
        sender.sendMessage(Component.text("=== Renaturation Status: " + region.getName() + " ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Phase: " + progress.getPhase(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Current Y Level: " + progress.getCurrentYLevel(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Target Y Level: " + progress.getMinYTarget(), NamedTextColor.YELLOW));
        double speed = Factions.getInstance().getRenaturationService() != null
                ? Factions.getInstance().getRenaturationService().getSpeedMultiplier(region.getId())
                : 1.0;
        sender.sendMessage(Component.text("Speed: " + String.format("%.1fx", speed), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Started: " + progress.getStartedAt(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Last Processed: " + progress.getLastProcessed(), NamedTextColor.GRAY));
    }

    private void showAllActive(CommandSender sender) {
        List<RenaturationProgress> activeProcesses = Factions.getInstance()
                .getBlockLogManager()
                .getRenaturationProgressDAO()
                .getAllActive();

        if (activeProcesses.isEmpty()) {
            sender.sendMessage(Component.text("No active renaturation processes", NamedTextColor.YELLOW));
            return;
        }

        sender.sendMessage(Component.text("=== Active Renaturation Processes ===", NamedTextColor.GOLD));
        for (RenaturationProgress progress : activeProcesses) {
            Region region = Factions.getInstance().getRegionManager().getRegionById(progress.getRegionId());
            String regionName = region != null ? region.getName() : "Unknown";
            sender.sendMessage(Component.text(
                    regionName + " - Y" + progress.getCurrentYLevel() + " - " + progress.getPhase(),
                    NamedTextColor.YELLOW
            ));
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

