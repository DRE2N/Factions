package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.structure.FlagStructure;
import de.erethon.factions.war.objective.OccupyWarObjective;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * @author Fyreum
 */
public class CreateWarFlagCommand extends FCommand {

    public CreateWarFlagCommand() {
        setCommand("createwarflag");
        setMinMaxArgs(1, 1);
        setPermissionFromName();
        setFUsage(getCommand() + " [occupyObjective]");
        setDescription("Erstellt eine Kriegsflagge für das jeweilige Objective");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        assure(fPlayer.hasSelection(), FMessage.ERROR_NO_SELECTION);
        OccupyWarObjective objective = plugin.getWarObjectiveManager().getObjective(args[1], OccupyWarObjective.class);
        assure(objective != null, FMessage.ERROR_WAR_OBJECTIVE_NOT_FOUND, args[1]);
        objective.getFlagStructures().add(new FlagStructure(fPlayer.getPos1(), fPlayer.getPos2()));
        sender.sendMessage(FMessage.CMD_CREATE_WAR_FLAG_SUCCESS.message(objective.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabList(plugin.getWarObjectiveManager().getObjectives(OccupyWarObjective.class).keySet(), args[1]);
        }
        return null;
    }
}
