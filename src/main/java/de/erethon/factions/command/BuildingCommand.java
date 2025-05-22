package de.erethon.factions.command;

import de.erethon.factions.building.BuildingSelectionGUI;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BuildingCommand extends FCommand {

    public BuildingCommand() {
        setCommand("building");
        setAliases("b");
        setPermission("factions.building");
        setPlayerCommand(true);
        setMaxArgs(1);
        setFUsage("/f building");
        setDescription("...");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        Faction faction = fPlayer.getFaction();
        if (!faction.isPrivileged(fPlayer)) {
            fPlayer.sendMessage(FMessage.ERROR_NO_PERMISSION.message());
            return;
        }
        if (faction.getUnrestLevel() > 0 && !fPlayer.isBypass()) { // Can't build during unrest
            fPlayer.sendMessage(Component.translatable("factions.error.unrest"));
            return;
        }
        Player player = fPlayer.getPlayer();
        BuildingSelectionGUI gui = new BuildingSelectionGUI(player);
        gui.open(player);
    }
}
