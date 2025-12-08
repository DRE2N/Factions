package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.marker.Marker;
import de.erethon.factions.player.FPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MarkerPositionCommand extends FCommand {

    public MarkerPositionCommand() {
        setCommand("position");
        setAliases("pos", "move");
        setMinMaxArgs(1, 3);
        setPermissionFromName(MarkerCommand.LABEL);
        setFUsage(MarkerCommand.LABEL + " " + getCommand() + " <id> [x] [z]");
        setDescription("Sets the position of a marker (uses current position if x/z not provided)");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Marker marker = plugin.getMarkerCache().get(args[1]);
        if (marker == null) {
            MessageUtil.sendMessage(sender, "<red>Marker not found: " + args[1]);
            return;
        }

        int x, z;
        if (args.length >= 4) {
            try {
                x = Integer.parseInt(args[2]);
                z = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                MessageUtil.sendMessage(sender, "<red>Invalid coordinates");
                return;
            }
        } else {
            FPlayer fPlayer = getFPlayerRaw(sender);
            Player player = fPlayer.getPlayer();
            x = player.getLocation().getBlockX();
            z = player.getLocation().getBlockZ();
        }

        marker.setPosition(x, z);
        marker.save();
        MessageUtil.sendMessage(sender, "<gray>Set position to: <green>" + x + ", " + z);
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

