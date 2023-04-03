package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.region.Region;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * @author Fyreum
 */
public class RegionDeleteCommand extends FCommand {

    public RegionDeleteCommand() {
        setCommand("delete");
        setAliases("remove");
        setMinMaxArgs(1, 2);
        setConsoleCommand(true);
        setPermissionFromName(RegionCommand.LABEL);
        setFUsage(RegionCommand.LABEL + " " + getCommand() + " [region]");
        setDescription("Löscht die Region");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Region region = getRegion(args[1]);
        if (region.isOwned()) {
            assure(args.length == 3 && args[2].equalsIgnoreCase("confirm"), FMessage.CMD_REGION_DELETE_CONFIRMATION_REQUIRED, getUsage().replace("[region]", args[1]) + " confirm");
        }
        if (region.delete()) {
            sender.sendMessage(FMessage.CMD_REGION_DELETE_SUCCESS.message());
        } else {
            sender.sendMessage(FMessage.CMD_REGION_DELETE_FAILED.message());
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
