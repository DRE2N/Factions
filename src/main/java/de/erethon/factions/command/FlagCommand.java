package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class FlagCommand extends FCommand {

    public FlagCommand() {
        setCommand("flag");
        setAliases("f");
        setMinMaxArgs(0, 1);
        setPermissionFromName();
        setFUsage(getCommand() + " ([faction])");
        setDescription("Setzt die Fraktionsflagge");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        Faction faction = args.length == 2 ? getFaction(args[1]) : getFaction(fPlayer);
        assureSenderHasAdminPerms(sender, faction);
        faction.setFlag(fPlayer.getPlayer().getInventory().getItemInMainHand());
    }
}
