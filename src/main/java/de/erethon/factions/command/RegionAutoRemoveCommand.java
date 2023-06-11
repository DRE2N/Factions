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
public class RegionAutoRemoveCommand extends FCommand {

    public RegionAutoRemoveCommand() {
        setCommand("remove");
        setAliases("r");
        setMinMaxArgs(0, 1);
        setPermissionFromName(RegionAutoCommand.PERM_PREFIX);
        setFUsage(RegionCommand.LABEL + " " + RegionAutoCommand.LABEL + " " + getCommand() + " ([region])");
        setDescription("Entfernt automatisch den jeweiligen Chunk aus der Region");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        AutomatedChunkManager acm = fPlayer.getAutomatedChunkManager();
        Region region = args.length == 2 ? getRegion(args[1]) : null;
        if (acm.getOperation() == ChunkOperation.REMOVE && acm.getSelection() == region) {
            acm.deactivate(false);
            return;
        }
        acm.setOperation(ChunkOperation.REMOVE);
        acm.setSelection(region);
    }
}
