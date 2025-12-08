package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.marker.Marker;
import org.bukkit.command.CommandSender;

public class MarkerListCommand extends FCommand {

    public MarkerListCommand() {
        setCommand("list");
        setAliases("l");
        setMinMaxArgs(0, 0);
        setPermissionFromName(MarkerCommand.LABEL);
        setFUsage(MarkerCommand.LABEL + " " + getCommand());
        setDescription("Lists all markers");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        int count = plugin.getMarkerCache().getSize();
        if (count == 0) {
            MessageUtil.sendMessage(sender, "<gray>No markers found.");
            return;
        }

        MessageUtil.sendMessage(sender, "<gold>=== Markers (" + count + ") ===");
        for (Marker marker : plugin.getMarkerCache()) {
            String name = marker.getName("en");
            if (name == null || name.isEmpty()) {
                name = marker.getName("de");
            }
            MessageUtil.sendMessage(sender, "<green>#" + marker.getId() + " <gray>- " + name + " <dark_gray>(" + marker.getX() + ", " + marker.getZ() + ")");
        }
    }
}

