package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class Pos1Command extends FCommand {

    public Pos1Command() {
        setCommand("pos1");
        setPermissionFromName();
        setFUsage(getCommand());
        setDescription("Setzt die erste Position der Markierung");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        fPlayer.setPos1(fPlayer.getPlayer().getLocation());
        sender.sendMessage(FMessage.CMD_POS1_SUCCESS.message(fPlayer.getPos1().blockX() + "," + fPlayer.getPos1().blockY() + "," + fPlayer.getPos1().blockZ()));
    }
}
