package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * @author Fyreum
 */
public class RegionAddCommand extends FCommand {

    public RegionAddCommand() {
        setCommand("add");
        setAliases("a");
        setMinMaxArgs(1, 1);
        setPermissionFromName(RegionCommand.LABEL);
        setFUsage(getCommand() + " [region]");
        setDescription("Fügt den Chunk der Region hinzu");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        Player player = fPlayer.getPlayer();
        Chunk chunk = player.getChunk();
        Region region = getRegion(args[1]);
        assureSameWorld(region, player);
        assureChunkIsNoRegion(chunk.getWorld(), chunk);
        region.addChunk(chunk);
        player.sendMessage(FMessage.CMD_REGION_ADD_CHUNK_SUCCESS.message(region.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabRegions((Player) sender, args[1]);
        }
        return null;
    }
}
