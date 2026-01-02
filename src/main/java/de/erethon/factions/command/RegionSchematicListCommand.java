package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

/**
 * Command to list all available schematic states for a region.
 *
 * @author Malfrador
 */
public class RegionSchematicListCommand extends FCommand {

    public RegionSchematicListCommand() {
        setCommand("list");
        setAliases("ls");
        setMinMaxArgs(0, 1);
        setConsoleCommand(true);
        setPermissionFromName(RegionCommand.LABEL + "." + RegionSchematicCommand.LABEL);
        setFUsage(RegionCommand.LABEL + " " + RegionSchematicCommand.LABEL + " " + getCommand() + " [region]");
        setDescription("Listet alle gespeicherten Schematic-Zustände einer Region");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Region region;

        if (args.length > 1) {
            region = getRegion(args[1]);
        } else {
            FPlayer fPlayer = getFPlayer(sender);
            region = getRegion(fPlayer);
        }

        Set<String> states = region.getAvailableSchematicStates();

        if (states.isEmpty()) {
            sender.sendMessage(FMessage.CMD_REGION_SCHEMATIC_LIST_EMPTY.message(region.getName()));
            return;
        }

        sender.sendMessage(FMessage.CMD_REGION_SCHEMATIC_LIST_HEADER.message(region.getName(), String.valueOf(states.size())));
        for (String state : states) {
            sender.sendMessage(FMessage.CMD_REGION_SCHEMATIC_LIST_ENTRY.message(state));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return sender instanceof Player player ? getTabRegions(player, args[1]) : getTabRegions(args[1]);
        }
        return null;
    }
}

