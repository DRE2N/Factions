package de.erethon.factions.command;

import de.erethon.bedrock.command.CommandFailedException;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.RegionType;
import de.erethon.factions.war.WarObjectiveManager;
import de.erethon.factions.war.objective.CrystalWarObjective;
import de.erethon.factions.war.objective.OccupyWarObjective;
import de.erethon.factions.war.objective.WarObjective;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * @author Fyreum
 */
public class ObjectiveCreateCommand extends FCommand {

    public static final List<String> OBJECTIVE_TYPES = List.of("crystal", "occupy");

    public ObjectiveCreateCommand() {
        setCommand("create");
        setAliases("c");
        setMinMaxArgs(2, 2);
        setPermissionFromName(ObjectiveCommand.LABLE);
        setFUsage(ObjectiveCommand.LABLE + " " + getCommand() + " [type] [name]");
        setDescription("Erstellt ein weiteres Kriegsziel");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        assure(getRegion(fPlayer).getType() == RegionType.WAR_ZONE, FMessage.ERROR_REGION_IS_NOT_A_WARZONE);
        WarObjectiveManager objectiveManager = plugin.getWarObjectiveManager();
        WarObjective objective = switch (args[1].toLowerCase()) {
            case "crystal" -> objectiveManager.instantiateObjective(args[2], CrystalWarObjective::new).setAlliance(fPlayer.getAlliance());
            case "occupy" -> objectiveManager.instantiateObjective(args[2], OccupyWarObjective::new);
            default -> throw new CommandFailedException(FMessage.ERROR_WAR_OBJECTIVE_TYPE_NOT_FOUND, args[1]);
        };
        objective.setLocation(fPlayer.getPlayer().getLocation());
        if (plugin.getWarPhaseManager().getCurrentWarPhase().isOpenWarZones()) {
            objective.activate();
        }
        sender.sendMessage(FMessage.CMD_OBJECTIVE_CREATE_SUCCESS.message());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabList(OBJECTIVE_TYPES, args[1]);
        }
        return null;
    }
}
