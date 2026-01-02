package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to delete a schematic state for a region.
 *
 * @author Malfrador
 */
public class RegionSchematicDeleteCommand extends FCommand {

    public RegionSchematicDeleteCommand() {
        setCommand("delete");
        setAliases("del", "remove", "rm");
        setMinMaxArgs(1, 2);
        setConsoleCommand(true);
        setPermissionFromName(RegionCommand.LABEL + "." + RegionSchematicCommand.LABEL);
        setFUsage(RegionCommand.LABEL + " " + RegionSchematicCommand.LABEL + " " + getCommand() + " <stateId> [region]");
        setDescription("Löscht einen gespeicherten Schematic-Zustand einer Region");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        String stateId = args[1];
        Region region;

        if (args.length > 2) {
            region = getRegion(args[2]);
        } else {
            FPlayer fPlayer = getFPlayer(sender);
            region = getRegion(fPlayer);
        }

        assure(region.hasSchematicState(stateId), FMessage.ERROR_REGION_SCHEMATIC_STATE_NOT_FOUND, stateId);

        boolean success = region.deleteSchematicState(stateId);
        if (success) {
            sender.sendMessage(FMessage.CMD_REGION_SCHEMATIC_DELETE_SUCCESS.message(region.getName(), stateId));
        } else {
            sender.sendMessage(FMessage.CMD_REGION_SCHEMATIC_LOAD_FAILED.message(region.getName(), stateId));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            if (sender instanceof Player player) {
                FPlayer fPlayer = getFPlayerRaw(player);
                Region region = fPlayer.getCurrentRegion();
                if (region != null) {
                    return getTabList(new ArrayList<>(region.getAvailableSchematicStates()), args[1]);
                }
            }
            return List.of();
        }
        if (args.length == 3) {
            return sender instanceof Player player ? getTabRegions(player, args[2]) : getTabRegions(args[2]);
        }
        return null;
    }
}

