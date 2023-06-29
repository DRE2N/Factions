package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.AutomatedChunkManager;
import de.erethon.factions.region.ChunkOperation;
import de.erethon.factions.region.Region;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class RegionAutoTransferCommand extends FCommand {

    public RegionAutoTransferCommand() {
        setCommand("transfer");
        setAliases("t");
        setMinMaxArgs(0, 1);
        setPermissionFromName(RegionAutoCommand.CMD_PREFIX);
        setFUsage(RegionAutoCommand.CMD_PREFIX + " " + getCommand() + " [region]");
        setDescription("Übergibt den Chunk an eine andere Region");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        AutomatedChunkManager acm = fPlayer.getAutomatedChunkManager();
        if (args.length < 2) {
            acm.deactivate();
            return;
        }
        Region region = getRegion(args[1]);
        if (acm.getOperation() == ChunkOperation.TRANSFER && acm.getSelection() == region) {
            acm.deactivate(true);
            return;
        }
        acm.setOperation(ChunkOperation.TRANSFER);
        acm.setSelection(region);
    }
}
