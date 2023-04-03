package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.region.Region;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class RegionAddNeighbourCommand extends FCommand {

    public RegionAddNeighbourCommand() {
        setCommand("addneighbour");
        setAliases("an");
        setMinMaxArgs(1, 2);
        setPlayerCommand(true);
        setPermissionFromName(RegionCommand.LABEL);
        setFUsage(RegionCommand.LABEL + " " + getCommand() + " [region]");
        setDescription("Markiert zwei Regionen als benachbart");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Region region;
        Region other;
        if (args.length == 2) {
            region = getRegion(getFPlayer(sender));
            other = getRegion(args[1]);
        } else {
            region = getRegion(args[1]);
            other = getRegion(args[2]);
        }
        boolean added = region.addAdjacentRegion(other);
        sender.sendMessage((added ? FMessage.ERROR_REGIONS_ALREADY_NEIGHBOURS : FMessage.CMD_REGION_ADD_NEIGHBOUR_SUCCESS).message(region.getName(), other.getName()));
    }
}
