package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class RegionNameCommand extends FCommand {

    public RegionNameCommand() {
        setCommand("name");
        setAliases("n");
        setMinMaxArgs(1, Integer.MAX_VALUE);
        setPermissionFromName();
        setFUsage(getCommand() + " [name]");
        setDescription("Benennt die aktuelle Region um");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayer(sender);
        Region region = getRegion(fPlayer);
        region.setName(getFinalArg(args, 2));
        sender.sendMessage(FMessage.CMD_REGION_NAME_SUCCESS.message(String.valueOf(region.getId()), region.getName()));
    }
}
