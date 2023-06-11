package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class RegionRemoveCommand extends FCommand {

    public RegionRemoveCommand() {
        setCommand("remove");
        setAliases("r");
        setPermissionFromName(RegionCommand.LABEL);
        setFUsage(RegionCommand.LABEL + " " + getCommand());
        setDescription("Entfernt den Chunk von der Region");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        Region region = getRegion(fPlayer);
        region.removeChunk(fPlayer.getPlayer().getChunk());
        sender.sendMessage(FMessage.CMD_REGION_REMOVE_CHUNK_SUCCESS.message(region.getName()));
    }
}
