package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class Pos2Command extends FCommand {

    public Pos2Command() {
        setCommand("pos2");
        setPermissionFromName();
        setFUsage(getCommand());
        setDescription("Setzt die erste Position der Markierung");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        fPlayer.setPos2(fPlayer.getPlayer().getLocation());
        sender.sendMessage(FMessage.CMD_POS2_SUCCESS.message(fPlayer.getPos1().blockX() + "," + fPlayer.getPos1().blockY() + "," + fPlayer.getPos1().blockZ()));
    }
}
