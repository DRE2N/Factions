package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.player.FPlayer;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * @author Fyreum
 */
public class RegionAutoRadiusCommand extends FCommand {

    private final List<String> radiusOptions = List.of("0", "1", "2");

    public RegionAutoRadiusCommand() {
        setCommand("autoradius");
        setMinMaxArgs(1, 1);
        setPermissionFromName(RegionCommand.LABEL);
        setFUsage(getCommand() + " [radius]");
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
            return getTabList(radiusOptions, args[1]);
        }
        return null;
    }
}
