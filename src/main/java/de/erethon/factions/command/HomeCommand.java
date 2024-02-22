package de.erethon.factions.command;

import de.erethon.aergia.util.TeleportUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.player.FPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

public class HomeCommand extends FCommand {

    public HomeCommand() {
        setCommand("home");
        setAliases("h");
        setMinMaxArgs(0, 0);
        setFUsage(getCommand());
        setPermissionFromName();
        setDescription("Teleportiert den Spieler zum Fraktions-Home"); // TODO: We need translatables for usage and description
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayer(sender);
        assurePlayerHasFaction(fPlayer);
        if (fPlayer.getFaction().getFHome() == null) {
            fPlayer.sendMessage(Component.translatable("factions.error.noFHome"));
            return;
        }
        TeleportUtil.teleport(fPlayer, fPlayer.getEPlayer(), fPlayer.getFaction().getFHome());
    }
}
