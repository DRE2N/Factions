package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.player.FPlayer;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Fyreum
 */
public class RegionAutoRadiusCommand extends FCommand {

    public RegionAutoRadiusCommand() {
        setCommand("autoradius");
        setMinMaxArgs(1, 1);
        setPermissionFromName(RegionAutoCommand.CMD_PREFIX);
        setFUsage(RegionAutoCommand.CMD_PREFIX + " " + getCommand() + " [radius]");
        setDescription("Setzt den Radius für automatische Chunkaktionen");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        fPlayer.getAutomatedChunkManager().setRadius(parseInt(args[1]));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            int max = plugin.getFConfig().getMaximumAutomatedChunkManagerRadius() + 1;
            List<String> completes = new ArrayList<>(max);
            for (int i = 0; i < max; i++) {
                if (String.valueOf(i).startsWith(args[1])) {
                    completes.add(String.valueOf(i));
                }
            }
            return completes;
        }
        return null;
    }
}
