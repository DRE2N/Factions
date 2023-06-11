package de.erethon.factions.command;

import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.region.Region;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * @author Fyreum
 */
public class RegionAllianceCommand extends FCommand {

    public RegionAllianceCommand() {
        setCommand("alliance");
        setMinMaxArgs(1, 2);
        setConsoleCommand(true);
        setPermissionFromName(RegionCommand.LABEL);
        setFUsage(RegionCommand.LABEL + " " + getCommand() + " [alliance] ([region])");
        setDescription("Setzt die Allianz der Region");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Alliance alliance = args[1].equalsIgnoreCase("none") ? null : getAlliance(args[1]);
        Region region = args.length == 2 ? getRegion(getFPlayer(sender)) : getRegion(args[2]);
        region.setAlliance(alliance);
        sender.sendMessage(FMessage.CMD_REGION_ALLIANCE_SUCCESS.message(region.getName(), alliance == null ? FMessage.GENERAL_NONE.getMessage() : alliance.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> completes = getTabAlliances(args[1]);
            if ("none".startsWith(args[1].toLowerCase())) {
                completes.add("none");
            }
            return completes;
        }
        if (args.length == 3) {
            return getTabRegions(args[2]);
        }
        return null;
    }
}
