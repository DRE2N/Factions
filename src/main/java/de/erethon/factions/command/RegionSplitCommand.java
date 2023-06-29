package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import org.bukkit.command.CommandSender;
import org.bukkit.util.Vector;

import java.util.concurrent.CompletableFuture;

/**
 * @author Fyreum
 */
public class RegionSplitCommand extends FCommand {

    public RegionSplitCommand() {
        setCommand("split");
        setPermissionFromName();
        setFUsage(RegionCommand.LABEL + " " + getCommand());
        setDescription("Trennt die aktuelle Region in zwei Teile");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        Region region = getRegion(fPlayer);
        Vector direction = fPlayer.getPlayer().getLocation().getDirection();
        CompletableFuture<Region> cf = plugin.getRegionManager().splitRegion(region, fPlayer.getCurrentChunk(), direction);
        cf.whenComplete((r, ex) -> sender.sendMessage(FMessage.CMD_REGION_SPLIT_SUCCESS.message(r.getName())));
    }
}
