package de.erethon.factions.command;

import de.erethon.bedrock.command.CommandFailedException;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionStructure;
import de.erethon.factions.region.WarRegion;
import de.erethon.factions.war.structure.ResourceStructure;
import de.erethon.factions.war.structure.WarFortressStructure;
import de.erethon.factions.war.structure.WarCastleStructure;
import de.erethon.factions.util.FUtil;
import de.erethon.factions.util.WorldEditSelection;
import de.erethon.factions.war.structure.CrystalWarStructure;
import de.erethon.factions.war.structure.OccupyWarStructure;
import de.erethon.factions.war.structure.WarStructure;
import io.papermc.paper.math.Position;
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
    public static final List<String> TYPES = List.of("castle", "fortress", "crystal", "occupy", "resource");
    public static final List<String> CASTLE_OPTIONS = List.of("schematicId:");
    public static final List<String> CRYSTAL_OPTIONS = List.of("damagePerTick:", "maxHealth:", "tickInterval:");
    public static final List<String> OCCUPY_OPTIONS = List.of("occupyDuration:", "occupiedInterval:", "tickInterval:",
            "warProgressDecline:", "warProgressDeclineContested:", "warProgressPerOccupiedInterval:");
    public static final List<String> RESOURCE_OPTIONS = List.of("resourceType:");

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
        String type = args[1].toLowerCase();
        boolean crystal = "crystal".equals(type);
        Position pos1;
        Position pos2;

        Location location = fPlayer.getPlayer().getLocation();
        Region region;
        if (crystal) {
            pos1 = Position.block(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            pos2 = pos1;
            region = plugin.getRegionManager().getRegionByLocation(location);
        } else {
            WorldEditSelection selection = WorldEditSelection.get(fPlayer.getPlayer());
            assure(selection != null, FMessage.ERROR_NO_SELECTION);
            pos1 = selection.min();
            pos2 = selection.max();
            region = plugin.getRegionManager().getRegionByLocation(selection.minLocation());
            assure(region == plugin.getRegionManager().getRegionByLocation(selection.maxLocation()), FMessage.ERROR_SELECTION_IN_DIFFERENT_REGIONS);
        }
        assure(region != null, FMessage.ERROR_SELECTION_IN_DIFFERENT_REGIONS);
        assure(region instanceof WarRegion, FMessage.ERROR_REGION_IS_NOT_A_WARZONE);
        WarRegion warRegion = (WarRegion) region;
        assure(crystal || FUtil.regionContainsAABB(region, pos1, pos2), FMessage.ERROR_SELECTION_IN_DIFFERENT_REGIONS);

        ConfigurationSection config = createConfig(args);
        RegionStructure structure = switch (type) {
            case "castle" -> new WarCastleStructure(warRegion, config, pos1, pos2);
            case "fortress" -> new WarFortressStructure(warRegion, config, pos1, pos2);
            case "crystal" -> new CrystalWarStructure(warRegion, config, pos1, pos2).setAlliance(fPlayer.getAlliance());
            case "occupy" -> new OccupyWarStructure(warRegion, config, pos1, pos2);
            case "resource" -> new ResourceStructure(warRegion, config, pos1, pos2);
            default -> throw new CommandFailedException(FMessage.ERROR_REGION_STRUCTURE_TYPE_NOT_FOUND, args[1]);
        };
        warRegion.getStructures().put(structure.getName(), structure);
        if (structure instanceof WarStructure warStructure && shouldActivate(warRegion)) {
            warStructure.activate();
        }
        warRegion.saveData();
        sender.sendMessage(FMessage.CMD_REGION_STRUCTURE_CREATE_SUCCESS.message(structure.getName()));
    }

    private boolean shouldActivate(WarRegion region) {
        if (!plugin.getCurrentWarPhase().isAllowPvP()) {
            return false;
        }
        return region.getType() != de.erethon.factions.region.RegionType.CAPITAL || plugin.getCurrentWarPhase().isOpenCapital();
    }

    private ConfigurationSection createConfig(String[] args) {
        ConfigurationSection config = new MemoryConfiguration();
        if (args.length >= 3 && !args[2].contains(":")) {
            config.set("name", args[2]);
        }
        if (args.length <= 3) {
            return config;
        }
        int firstOption = args[2].contains(":") ? 2 : 3;
        for (int i = firstOption; i < args.length; i++) {
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
            case "resource" -> getTabList(RESOURCE_OPTIONS, current);
            default -> null;
        };
    }
}
