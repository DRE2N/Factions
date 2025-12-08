package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.marker.Marker;
import de.erethon.factions.player.FPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MarkerCreateCommand extends FCommand {

    public MarkerCreateCommand() {
        setCommand("create");
        setAliases("c");
        setMinMaxArgs(0, 1);
        setPermissionFromName(MarkerCommand.LABEL);
        setFUsage(MarkerCommand.LABEL + " " + getCommand() + " [iconPath]");
        setDescription("Creates a marker at your current position");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        Player player = fPlayer.getPlayer();

        String iconPath = args.length == 2 ? args[1] : "lectern.png";
        int x = player.getLocation().getBlockX();
        int z = player.getLocation().getBlockZ();

        Marker marker = plugin.getMarkerCache().createMarker(iconPath, x, z);
        MessageUtil.sendMessage(sender, "<gray>Created marker <green>#" + marker.getId() +
                "<gray> at your current position with icon <green>" + iconPath);
    }
}

