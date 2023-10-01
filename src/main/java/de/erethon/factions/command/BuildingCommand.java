package de.erethon.factions.command;

import de.erethon.factions.Factions;
import de.erethon.factions.building.BuildSiteCache;
import de.erethon.factions.building.Building;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import org.bukkit.command.CommandSender;

public class BuildingCommand extends FCommand {

    // TODO: Temp for testing until we have NPCs or GUIs something else for selecting buildings

    public BuildingCommand() {
        setCommand("building");
        setAliases("b");
        setPermission("factions.building");
        setPlayerCommand(true);
        setMaxArgs(3);
        setFUsage("/f building");
        setDescription("...");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        Faction faction = fPlayer.getFaction();
        if (faction == null) {
            return;
        }
        BuildSiteCache cache = Factions.get().getBuildSiteCache();
        if (args[1].equalsIgnoreCase("list")) {
            if (cache.get(fPlayer.getPlayer().getChunk().getChunkKey()) == null) {
                fPlayer.sendMessage("No buildings found in region.");
                return;
            }
            cache.get(fPlayer.getPlayer().getChunk().getChunkKey()).forEach(b ->
                    fPlayer.sendMessage(b.getName()));
            return;
        }
        if (args[1].equalsIgnoreCase("add")) {
            Building building = Factions.get().getBuildingManager().getById(args[2]);
            if (building == null) {
                fPlayer.sendMessage("Building not found.");
                return;
            }
            building.build(fPlayer.getPlayer(), fPlayer.getFaction(), fPlayer.getCurrentRegion(), fPlayer.getPlayer().getLocation());
            fPlayer.sendMessage("Building " + building.getId() + " added.");
        }
        if (args[1].equalsIgnoreCase("remove")) {
            Building building = Factions.get().getBuildingManager().getById(args[2]);
            if (building == null) {
                fPlayer.sendMessage("Building not found.");
                return;
            }
            cache.get(fPlayer.getPlayer().getChunk().getChunkKey()).removeIf(b -> b.getBuilding().equals(building));
            fPlayer.sendMessage("Building " + building.getId() + " removed.");
        }
    }
}
