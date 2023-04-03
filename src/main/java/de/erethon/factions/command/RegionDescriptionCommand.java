package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * @author Fyreum
 */
public class RegionDescriptionCommand extends FCommand {

    public RegionDescriptionCommand() {
        setCommand("description");
        setAliases("desc", "d");
        setMinMaxArgs(1, Integer.MAX_VALUE);
        setPermissionFromName(RegionCommand.LABEL);
        setFUsage(RegionCommand.LABEL + " " + getCommand() + " [...]");
        setDescription("Setzt die Beschreibung der Region");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        Region region = getRegion(fPlayer);
        region.setDescription(getFinalArg(args, 1));
        fPlayer.sendMessage(FMessage.CMD_REGION_DESCRIPTION_SUCCESS.message(region.getName(), region.getDescription()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabRegions(args[1]);
        }
        return null;
    }
}
