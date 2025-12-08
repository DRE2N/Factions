package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.marker.Marker;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class MarkerNameCommand extends FCommand {

    public MarkerNameCommand() {
        setCommand("name");
        setMinMaxArgs(3, Integer.MAX_VALUE);
        setPermissionFromName(MarkerCommand.LABEL);
        setFUsage(MarkerCommand.LABEL + " " + getCommand() + " <id> <language> <name...>");
        setDescription("Sets the name for a marker in a specific language");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Marker marker = plugin.getMarkerCache().get(args[1]);
        if (marker == null) {
            MessageUtil.sendMessage(sender, "<red>Marker not found: " + args[1]);
            return;
        }

        String language = args[2];
        StringBuilder name = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            if (i > 3) {
                name.append(" ");
            }
            name.append(args[i]);
        }

        marker.setName(language, name.toString());
        marker.save();
        MessageUtil.sendMessage(sender, "<gray>Set name (<green>" + language + "<gray>) to: <green>" + name);
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
        if (args.length == 3) {
            return List.of("en", "de");
        }
        return List.of();
    }
}

