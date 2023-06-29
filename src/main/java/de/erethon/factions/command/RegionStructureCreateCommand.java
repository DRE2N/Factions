package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.LazyChunk;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionCache;
import de.erethon.factions.region.structure.RegionStructure;
import de.erethon.factions.region.structure.WarCastleStructure;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * @author Fyreum
 */
public class RegionStructureCreateCommand extends FCommand {

    public final List<String> types = List.of("WarCastle");

    public RegionStructureCreateCommand() {
        setCommand("create");
        setAliases("c");
        setMinMaxArgs(1, 1);
        setPermissionFromName(RegionStructureCommand.CMD_PREFIX);
        setFUsage(RegionStructureCommand.CMD_PREFIX + " " + getCommand());
        setDescription("Erstellt eine Regionsstruktur");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        Location pos1 = fPlayer.getPos1(), pos2 = fPlayer.getPos2();
        assure(pos1 != null && pos2 != null, FMessage.ERROR_NO_SELECTION);
        assure(pos1.getWorld() != null && pos1.getWorld().equals(pos2.getWorld()), FMessage.ERROR_SELECTION_IN_DIFFERENT_WORLDS);

        RegionCache cache = plugin.getRegionManager().getCache(pos1.getWorld());
        assure(cache != null, FMessage.ERROR_WORLD_IS_REGIONLESS, pos1.getWorld().getName());
        Region region = cache.getByLocation(pos1);
        assure(region != null, FMessage.ERROR_SELECTION_IN_DIFFERENT_REGIONS);

        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        for (int x = Math.min(pos1.getBlockX(), pos2.getBlockX()); x < maxX; x++) {
            for (int z = Math.min(pos1.getBlockZ(), pos2.getBlockZ()); z < maxZ; z++) {
                assure(region.equals(cache.getByChunk(new LazyChunk(x >> 4, z >> 4))), FMessage.ERROR_SELECTION_IN_DIFFERENT_REGIONS);
            }
        }
        RegionStructure structure = null;

        if (args[1].equalsIgnoreCase("warcastle")) {
            structure = new WarCastleStructure(pos1, pos2);
        }
        assure(structure != null, FMessage.ERROR_REGION_STRUCTURE_TYPE_NOT_FOUND, args[1]);
        region.getStructures().add(structure);
        sender.sendMessage(FMessage.CMD_REGION_STRUCTURE_CREATE_SUCCESS.message(String.valueOf(region.getStructures().size())));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabList(types, args[1]);
        }
        return null;
    }
}
