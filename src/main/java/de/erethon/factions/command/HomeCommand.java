package de.erethon.factions.command;

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
        setDescription("home"); // TODO: We need translatables for usage and description
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayer(sender);
        assurePlayerHasFaction(fPlayer);
        if (fPlayer.getFaction().getFHome() == null) {
            fPlayer.sendMessage(Component.translatable("factions.error.noFHome"));
            return;
        }
        fPlayer.getPlayer().teleportAsync(fPlayer.getFaction().getFHome());
        fPlayer.sendActionBar(Component.translatable("factions.cmd.home.success"));

    }
}
