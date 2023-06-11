package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionType;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * @author Fyreum
 */
public class RegionTypeCommand extends FCommand {

    public RegionTypeCommand() {
        setCommand("type");
        setAliases("t");
        setMinMaxArgs(1, 1);
        setPermissionFromName(RegionCommand.LABEL);
        setFUsage(RegionCommand.LABEL + " " + getCommand() + "[type]");
        setDescription("Setzt den Typ der Region");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        Region region = getRegion(fPlayer);
        RegionType type = RegionType.getByName(args[1]);
        assure(type != null, FMessage.ERROR_REGION_TYPE_NOT_FOUND, args[1]);
        region.setType(type);
        sender.sendMessage(FMessage.CMD_REGION_TYPE_SUCCESS.message(region.getName(), type.name()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabList(RegionType.values(), args[1]);
        }
        return null;
    }
}
