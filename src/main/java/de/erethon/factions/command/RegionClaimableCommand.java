package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.region.Region;
import org.bukkit.command.CommandSender;

import java.util.List;

public class RegionClaimableCommand extends FCommand {

    public RegionClaimableCommand() {
        setCommand("claimable");
        setMinMaxArgs(1, 2);
        setPermissionFromName(RegionCommand.LABEL);
        setFUsage(RegionCommand.LABEL + " " + getCommand() + " [true|false] ([region])");
        setDescription("Setzt, ob eine Region beanspruchbar ist");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Region region = args.length >= 3 ? getRegion(args[2]) : getRegion(getFPlayer(sender));
        boolean claimable = Boolean.parseBoolean(args[1]);
        region.setClaimable(claimable);
        sender.sendMessage("Region " + region.getName() + " claimable=" + claimable + ".");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabList(List.of("true", "false"), args[1]);
        }
        if (args.length == 3) {
            return getTabRegions(args[2]);
        }
        return null;
    }
}
