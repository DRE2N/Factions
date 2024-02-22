package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.player.FPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

public class SetHomeCommand extends FCommand {

    public SetHomeCommand() {
        setCommand("sethome");
        setAliases("sh");
        setMinMaxArgs(0, 0);
        setFUsage(getCommand());
        setPermissionFromName();
        setDescription("sethome"); // TODO: We need translatables for usage and description
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayer(sender);
        assurePlayerHasFaction(fPlayer);
        assureSenderHasModPerms(sender, fPlayer.getFaction());
        fPlayer.getFaction().setFHome(fPlayer.getPlayer().getLocation());
        fPlayer.sendMessage(Component.translatable("factions.cmd.sethome.success"));
    }
}
