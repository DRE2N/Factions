package de.erethon.factions.command;

import de.erethon.bedrock.command.CommandFailedException;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionStructure;
import de.erethon.factions.war.structure.WarCastleStructure;
import de.erethon.factions.util.FUtil;
import de.erethon.factions.war.objective.CrystalWarObjective;
import de.erethon.factions.war.objective.OccupyWarObjective;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.List;

/**
 * @author Fyreum
 */
public class RegionStructureCreateCommand extends FCommand {

    // todo: There has to be a better way of doing this... (maybe some sort of property API)
    public static final List<String> TYPES = List.of("castle", "crystal", "occupy");
    public static final List<String> CASTLE_OPTIONS = List.of("schematicId:");
    public static final List<String> CRYSTAL_OPTIONS = List.of("damagePerTick:", "maxHealth:", "tickInterval:");
    public static final List<String> OCCUPY_OPTIONS = List.of("occupyDuration:", "occupiedInterval:", "tickInterval:",
            "warProgressDecline:", "warProgressDeclineContested:", "warProgressPerOccupiedInterval:");

    public RegionStructureCreateCommand() {
        setCommand("create");
        setAliases("c");
        setMinMaxArgs(1, Integer.MAX_VALUE);
        setPermissionFromName(RegionStructureCommand.CMD_PREFIX);
        setFUsage(RegionStructureCommand.CMD_PREFIX + " " + getCommand());
        setDescription("Erstellt eine Regionsstruktur");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        Location pos1 = fPlayer.getPos1(),
                 pos2 = fPlayer.getPos2();
        assure(pos1 != null && pos2 != null, FMessage.ERROR_NO_SELECTION);
        assure(pos1.getWorld() != null && pos1.getWorld().equals(pos2.getWorld()), FMessage.ERROR_SELECTION_IN_DIFFERENT_WORLDS);

        Region region = plugin.getRegionManager().getRegionByLocation(pos1);
        assure(region != null, FMessage.ERROR_SELECTION_IN_DIFFERENT_REGIONS);
        assure(FUtil.regionContainsAABB(region, pos1, pos2), FMessage.ERROR_SELECTION_IN_DIFFERENT_REGIONS);

        ConfigurationSection config = createConfig(args);
        RegionStructure structure = switch (args[1].toLowerCase()) {
            case "castle" -> new WarCastleStructure(region, config, pos1, pos2);
            case "crystal" -> new CrystalWarObjective(region, config, pos1, pos2).setAlliance(fPlayer.getAlliance());
            case "occupy" -> new OccupyWarObjective(region, config, pos1, pos2);
            default -> throw new CommandFailedException(FMessage.ERROR_REGION_STRUCTURE_TYPE_NOT_FOUND, args[1]);
        };
        region.getStructures().put(structure.getName(), structure);
        sender.sendMessage(FMessage.CMD_REGION_STRUCTURE_CREATE_SUCCESS.message(structure.getName()));
    }

    private ConfigurationSection createConfig(String[] args) {
        ConfigurationSection config = new MemoryConfiguration();
        if (args.length <= 3) {
            return config;
        }
        for (int i = 3; i < args.length; i++) {
            String[] split = args[i].split(":", 2);
            assure(split.length == 2, FMessage.ERROR_REGION_STRUCTURE_WRONG_ARG_FORMAT);
            config.set(split[0], split[1]);
        }
        return config;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            return null;
        }
        if (args.length == 2) {
            return getTabList(TYPES, args[1]);
        }
        String current = args[args.length - 1];
        return switch (args[1].toLowerCase()) {
            case "castle" -> getTabList(CASTLE_OPTIONS, current);
            case "crystal" -> getTabList(CRYSTAL_OPTIONS, current);
            case "occupy" -> getTabList(OCCUPY_OPTIONS, current);
            default -> null;
        };
    }
}
