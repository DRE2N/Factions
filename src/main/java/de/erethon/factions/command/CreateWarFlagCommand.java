package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.war.structure.FlagStructure;
import de.erethon.factions.war.structure.OccupyWarStructure;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;

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
        Region region = getRegion(fPlayer);
        assure(fPlayer.hasSelection(), FMessage.ERROR_NO_SELECTION);
        OccupyWarStructure objective = region.getStructure(args[1], OccupyWarStructure.class);
        assure(objective != null, FMessage.ERROR_WAR_OBJECTIVE_NOT_FOUND, args[1]);
        objective.getFlagStructures().add(new FlagStructure(region, new MemoryConfiguration(), fPlayer.getPos1(), fPlayer.getPos2()));
        sender.sendMessage(FMessage.CMD_CREATE_WAR_FLAG_SUCCESS.message(objective.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            if (!(sender instanceof Player player)) {
                return null;
            }
            Region region = plugin.getFPlayerCache().getByPlayer(player).getCurrentRegion();
            if (region == null) {
                return null;
            }
            return getTabList(region.getStructures(OccupyWarStructure.class).keySet(), args[1]);
        }
        return null;
    }
}
