package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.marker.Marker;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class MarkerDeleteCommand extends FCommand {

    public MarkerDeleteCommand() {
        setCommand("delete");
        setAliases("d", "remove");
        setMinMaxArgs(1, 1);
        setPermissionFromName(MarkerCommand.LABEL);
        setFUsage(MarkerCommand.LABEL + " " + getCommand() + " <id>");
        setDescription("Deletes a marker");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Marker marker = plugin.getMarkerCache().get(args[1]);
        if (marker == null) {
            MessageUtil.sendMessage(sender, "<red>Marker not found: " + args[1]);
            return;
        }

        int id = marker.getId();
        marker.delete();
        MessageUtil.sendMessage(sender, "<gray>Deleted marker <green>#" + id);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> ids = new ArrayList<>();
            for (Marker marker : plugin.getMarkerCache()) {
                ids.add(String.valueOf(marker.getId()));
            }
            return ids;
        }
        return List.of();
    }
}

