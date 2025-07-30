package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionMode;
import org.bukkit.command.CommandSender;

import java.util.List;

public class RegionModeCommand extends FCommand {

    public RegionModeCommand() {
        setCommand("mode");
        setAliases("m");
        setMinMaxArgs(1, 1);
        setPermissionFromName(RegionCommand.LABEL);
        setFUsage(RegionCommand.LABEL + " " + getCommand() + "[mode]");
        setDescription("Setzt den Modus der Region");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        Region region = getRegion(fPlayer);
        RegionMode mode = RegionMode.getByName(args[1]);
        assure(mode != null, FMessage.ERROR_REGION_TYPE_NOT_FOUND, args[1]);
        region.setMode(mode);
        sender.sendMessage(FMessage.CMD_REGION_TYPE_SUCCESS.message(region.getName(), mode.name()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabList(RegionMode.values(), args[1]);
        }
        return null;
    }
}
