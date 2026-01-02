package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Command to save the current state of a region as a schematic.
 *
 * @author Malfrador
 */
public class RegionSchematicSaveCommand extends FCommand {

    public RegionSchematicSaveCommand() {
        setCommand("save");
        setAliases("s");
        setMinMaxArgs(1, 2);
        setConsoleCommand(true);
        setPermissionFromName(RegionCommand.LABEL + "." + RegionSchematicCommand.LABEL);
        setFUsage(RegionCommand.LABEL + " " + RegionSchematicCommand.LABEL + " " + getCommand() + " <stateId> [region]");
        setDescription("Speichert den aktuellen Zustand einer Region als Schematic");
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

        sender.sendMessage(FMessage.CMD_REGION_SCHEMATIC_SAVE_STARTED.message(region.getName(), stateId));

        region.saveSchematicStateAsync(stateId).thenAccept(success -> {
            if (success) {
                sender.sendMessage(FMessage.CMD_REGION_SCHEMATIC_SAVE_SUCCESS.message(region.getName(), stateId));
            } else {
                sender.sendMessage(FMessage.CMD_REGION_SCHEMATIC_SAVE_FAILED.message(region.getName(), stateId));
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            // Suggest existing states plus "rollback"
            if (sender instanceof Player player) {
                FPlayer fPlayer = getFPlayerRaw(player);
                Region region = fPlayer.getCurrentRegion();
                if (region != null) {
                    List<String> states = new java.util.ArrayList<>(region.getAvailableSchematicStates());
                    states.add("rollback");
                    return getTabList(states, args[1]);
                }
            }
            return getTabList(List.of("rollback"), args[1]);
        }
        if (args.length == 3) {
            return sender instanceof Player player ? getTabRegions(player, args[2]) : getTabRegions(args[2]);
        }
        return null;
    }
}

