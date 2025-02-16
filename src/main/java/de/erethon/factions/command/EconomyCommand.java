package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.economy.gui.EconomyGUI;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EconomyCommand extends FCommand {

    public EconomyCommand() {
        setCommand("economy");
        setAliases("eco");
        setPermission("factions.economy");
        setPlayerCommand(true);
        setFUsage("/f economy");
        setDescription("...");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        FPlayer fPlayer = getFPlayer(player);
        Faction faction = getFaction(fPlayer);
        EconomyGUI gui = new EconomyGUI(player, faction);
        gui.open();
    }
}
