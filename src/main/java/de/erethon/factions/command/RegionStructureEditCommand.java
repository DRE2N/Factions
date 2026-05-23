package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionStructure;
import de.erethon.factions.region.WarRegion;
import de.erethon.factions.util.FUtil;
import de.erethon.factions.util.WorldEditSelection;
import de.erethon.factions.war.structure.CrystalWarStructure;
import de.erethon.factions.war.structure.OccupyWarStructure;
import de.erethon.factions.war.structure.TickingWarStructure;
import de.erethon.factions.war.structure.WarStructure;
import io.papermc.paper.math.Position;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public class RegionStructureEditCommand extends FCommand {

    private static final List<String> COMMON_OPTIONS = List.of("tickInterval:");
    private static final List<String> OCCUPY_OPTIONS = List.of("occupyDuration:", "occupiedInterval:",
            "warProgressDecline:", "warProgressDeclineContested:", "warProgressPerOccupiedInterval:");
    private static final List<String> CRYSTAL_OPTIONS = List.of("energyLossOnDamage:", "energyLossPerInterval:",
            "maxEnergy:", "energy:", "energyGainPerCarrier:", "energyLossForCarrierSpawn:", "carrierSpawnInterval:");

    public RegionStructureEditCommand() {
        setCommand("edit");
        setAliases("e");
        setMinMaxArgs(2, Integer.MAX_VALUE);
        setPermissionFromName(RegionStructureCommand.CMD_PREFIX);
        setFUsage(RegionStructureCommand.CMD_PREFIX + " " + getCommand() + " [region] <structure> <key:value...>");
        setDescription("Bearbeitet Eigenschaften einer Regionsstruktur");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Target target = target(sender, args);
        assure(args.length > target.firstOption(), FMessage.ERROR_REGION_STRUCTURE_WRONG_ARG_FORMAT);
        boolean restart = target.structure() instanceof TickingWarStructure ticking && ticking.isTicking();
        if (restart) {
            ((WarStructure) target.structure()).deactivate();
        }
        RegionStructure structure = target.structure();
        for (int i = target.firstOption(); i < args.length; i++) {
            structure = edit(sender, target.region(), structure, args[i]);
        }
        if (restart) {
            ((WarStructure) structure).activate();
        }
        target.region().saveData();
        sender.sendMessage(Component.text("Edited structure " + structure.getName() + "."));
    }

    private Target target(CommandSender sender, String[] args) {
        Region current = sender instanceof org.bukkit.entity.Player ? getRegion(getFPlayer(sender)) : null;
        if (current instanceof WarRegion warRegion) {
            RegionStructure structure = warRegion.getStructure(args[1]);
            if (structure != null) {
                return new Target(warRegion, structure, 2);
            }
        }
        assure(args.length >= 4, FMessage.ERROR_REGION_STRUCTURE_WRONG_ARG_FORMAT);
        Region region = getRegion(args[1]);
        assure(region instanceof WarRegion, FMessage.ERROR_REGION_IS_NOT_A_WARZONE);
        RegionStructure structure = ((WarRegion) region).getStructure(args[2]);
        assure(structure != null, FMessage.ERROR_WAR_OBJECTIVE_NOT_FOUND, args[2]);
        return new Target((WarRegion) region, structure, 3);
    }

    private RegionStructure edit(CommandSender sender, WarRegion region, RegionStructure structure, String arg) {
        if ("area".equalsIgnoreCase(arg)) {
            return editArea(sender, region, structure);
        }
        if ("position".equalsIgnoreCase(arg) || "pos".equalsIgnoreCase(arg)) {
            return editPosition(sender, region, structure);
        }
        String[] split = arg.split(":", 2);
        assure(split.length == 2, FMessage.ERROR_REGION_STRUCTURE_WRONG_ARG_FORMAT);
        String key = split[0].toLowerCase();
        String value = split[1];
        if (structure instanceof TickingWarStructure ticking && "tickinterval".equals(key)) {
            ticking.setTickInterval(parseLong(value));
            return structure;
        }
        if (structure instanceof OccupyWarStructure occupy && editOccupy(occupy, key, value)) {
            return structure;
        }
        if (structure instanceof CrystalWarStructure crystal && editCrystal(crystal, key, value)) {
            return structure;
        }
        throw new de.erethon.factions.util.FException("Unknown structure property " + key, FMessage.ERROR_REGION_STRUCTURE_WRONG_ARG_FORMAT);
    }

    private RegionStructure editArea(CommandSender sender, WarRegion region, RegionStructure structure) {
        Player player = getFPlayer(sender).getPlayer();
        WorldEditSelection selection = WorldEditSelection.get(player);
        assure(selection != null, FMessage.ERROR_NO_SELECTION);
        assure(region == plugin.getRegionManager().getRegionByLocation(selection.minLocation()), FMessage.ERROR_SELECTION_IN_DIFFERENT_REGIONS);
        assure(region == plugin.getRegionManager().getRegionByLocation(selection.maxLocation()), FMessage.ERROR_SELECTION_IN_DIFFERENT_REGIONS);
        assure(FUtil.regionContainsAABB(region, selection.min(), selection.max()), FMessage.ERROR_SELECTION_IN_DIFFERENT_REGIONS);
        return replaceStructure(region, structure, selection.min(), selection.max());
    }

    private RegionStructure editPosition(CommandSender sender, WarRegion region, RegionStructure structure) {
        assure(structure instanceof CrystalWarStructure, FMessage.ERROR_REGION_STRUCTURE_WRONG_ARG_FORMAT);
        Player player = getFPlayer(sender).getPlayer();
        assure(region == plugin.getRegionManager().getRegionByLocation(player.getLocation()), FMessage.ERROR_SELECTION_IN_DIFFERENT_REGIONS);
        Position position = Position.block(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
        return replaceStructure(region, structure, position, position);
    }

    private RegionStructure replaceStructure(WarRegion region, RegionStructure oldStructure, Position min, Position max) {
        if (oldStructure instanceof WarStructure warStructure) {
            warStructure.deactivate();
        }
        if (oldStructure instanceof Listener listener) {
            HandlerList.unregisterAll(listener);
        }
        ConfigurationSection config = new MemoryConfiguration();
        oldStructure.serialize().forEach(config::set);
        config.set("minPosition", java.util.Map.of("x", min.blockX(), "y", min.blockY(), "z", min.blockZ()));
        config.set("maxPosition", java.util.Map.of("x", max.blockX(), "y", max.blockY(), "z", max.blockZ()));
        RegionStructure replacement = RegionStructure.deserialize(region, config);
        region.getStructures().put(oldStructure.getName(), replacement);
        return replacement;
    }

    private boolean editOccupy(OccupyWarStructure occupy, String key, String value) {
        switch (key) {
            case "occupyduration", "capturetime" -> occupy.setOccupyDuration(Math.max(1, parseLong(value) / occupy.getTickInterval()));
            case "occupiedinterval" -> occupy.setOccupiedInterval(Math.max(1, parseLong(value) / occupy.getTickInterval()));
            case "warprogressdecline" -> occupy.setWarProgressDecline(parseInt(value));
            case "warprogressdeclinecontested" -> occupy.setWarProgressDeclineContested(parseInt(value));
            case "warprogressperoccupiedinterval", "warpointsperoccupiedinterval" -> occupy.setWarProgressPerOccupiedInterval(parseInt(value));
            default -> {
                return false;
            }
        }
        return true;
    }

    private boolean editCrystal(CrystalWarStructure crystal, String key, String value) {
        switch (key) {
            case "energylossondamage", "damagepertick" -> crystal.setEnergyLossOnDamage(parseDouble(value));
            case "energylossperinterval" -> crystal.setEnergyLossPerInterval(parseDouble(value));
            case "maxenergy", "maxhealth" -> crystal.setMaxEnergy(parseDouble(value));
            case "energy" -> crystal.setEnergy(parseDouble(value));
            case "energygainpercarrier" -> crystal.setEnergyGainPerCarrier(parseDouble(value));
            case "energylossforcarrierspawn" -> crystal.setEnergyLossForCarrierSpawn(parseDouble(value));
            case "carrierspawninterval" -> crystal.setCarrierSpawnInterval(parseLong(value));
            default -> {
                return false;
            }
        }
        return true;
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new de.erethon.factions.util.FException("Not a long: " + value, FMessage.ERROR_NOT_PARSABLE_INTEGER, value);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> completions = new ArrayList<>();
            if (sender instanceof Player player) {
                Region region = getFPlayerRaw(sender).getCurrentRegion();
                if (region instanceof WarRegion warRegion) {
                    completions.addAll(warRegion.getStructures().keySet());
                }
                completions.addAll(getTabRegions(player, args[1]));
            }
            completions.addAll(getTabRegions(args[1]));
            return getTabList(completions, args[1]);
        }
        if (args.length == 3) {
            Region region = regionForTab(sender, args[1]);
            if (region instanceof WarRegion warRegion) {
                return getTabList(warRegion.getStructures().keySet(), args[2]);
            }
        }
        String current = args[args.length - 1];
        List<String> options = new ArrayList<>(List.of("area", "position"));
        options.addAll(COMMON_OPTIONS);
        options.addAll(OCCUPY_OPTIONS);
        options.addAll(CRYSTAL_OPTIONS);
        return getTabList(options, current);
    }

    private record Target(WarRegion region, RegionStructure structure, int firstOption) {
    }

    private Region regionForTab(CommandSender sender, String regionArg) {
        if ("here".equalsIgnoreCase(regionArg) && sender instanceof Player player) {
            return plugin.getRegionManager().getRegionByLocation(player.getLocation());
        }
        return plugin.getRegionManager().getRegionByName(regionArg);
    }
}
