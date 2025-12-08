package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.marker.Marker;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class MarkerInfoCommand extends FCommand {

    public MarkerInfoCommand() {
        setCommand("info");
        setAliases("i");
        setMinMaxArgs(1, 1);
        setPermissionFromName(MarkerCommand.LABEL);
        setFUsage(MarkerCommand.LABEL + " " + getCommand() + " <id>");
        setDescription("Shows information about a marker");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Marker marker = plugin.getMarkerCache().get(args[1]);
        if (marker == null) {
            MessageUtil.sendMessage(sender, "<red>Marker not found: " + args[1]);
            return;
        }

        MessageUtil.sendMessage(sender, "<gold>=== Marker <green>#" + marker.getId() + " <gold>===");
        MessageUtil.sendMessage(sender, "<green>Position: <gray>" + marker.getX() + ", " + marker.getZ());
        MessageUtil.sendMessage(sender, "<green>Icon: <gray>" + marker.getIconPath());
        MessageUtil.sendMessage(sender, "<green>Names:");
        for (String lang : marker.getNames().keySet()) {
            MessageUtil.sendMessage(sender, "  <green>" + lang + ": <gray>" + marker.getName(lang));
        }
        MessageUtil.sendMessage(sender, "<green>Descriptions:");
        for (String lang : marker.getDescriptions().keySet()) {
            String desc = marker.getDescription(lang);
            MessageUtil.sendMessage(sender, "  <green>" + lang + ": <gray>" + (desc.isEmpty() ? "(none)" : desc));
        }
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

