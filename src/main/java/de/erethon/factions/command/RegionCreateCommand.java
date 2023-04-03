package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Fyreum
 */
public class RegionCreateCommand extends FCommand {

    public RegionCreateCommand() {
        setCommand("create");
        setAliases("c");
        setMinMaxArgs(0, 1);
        setPermissionFromName(RegionCommand.LABEL);
        setFUsage(RegionCommand.LABEL + " " + getCommand() + " ([name])");
        setDescription("Erstellt eine Region");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        Player player = fPlayer.getPlayer();
        RegionManager regionManager = plugin.getRegionManager();
        Region region = args.length == 2 ? regionManager.createRegion(player.getChunk(), args[1]) : regionManager.createRegion(player.getChunk());
        player.sendMessage(FMessage.CMD_REGION_CREATE_SUCCESS.message(region.getName()));
    }

}
