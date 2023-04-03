package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.AutomatedChunkManager;
import de.erethon.factions.region.ChunkOperation;
import de.erethon.factions.region.Region;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * @author Fyreum
 */
public class RegionAutoAddChunkCommand extends FCommand {

    public RegionAutoAddChunkCommand() {
        setCommand("autoaddchunk");
        setAliases("aa");
        setMinMaxArgs(0, 1);
        setPermissionFromName(RegionCommand.LABEL);
        setFUsage(getCommand() + " [region]");
        setDescription("Fügt automatisch den jeweiligen Chunk der Region hinzu");
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
        if (acm.getSelection() == region) {
            acm.deactivate();
            return;
        }
        assureSameWorld(region, fPlayer.getPlayer());
        acm.setOperation(ChunkOperation.ADD);
        acm.setSelection(region);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabRegions((Player) sender, args[1]);
        }
        return null;
    }
}
