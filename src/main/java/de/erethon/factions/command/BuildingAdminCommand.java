package de.erethon.factions.command;

import de.erethon.factions.Factions;
import de.erethon.factions.building.BuildSiteCache;
import de.erethon.factions.building.Building;
import de.erethon.factions.building.BuildingManager;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

public class BuildingAdminCommand extends FCommand {

    // TODO: Temp for testing until we have NPCs or GUIs something else for selecting buildings

    public BuildingAdminCommand() {
        setCommand("buildingadmin");
        setAliases("ba");
        setPermission("factions.buildingadmin");
        setPlayerCommand(true);
        setMaxArgs(3);
        setFUsage("/f buildingadmin");
        setDescription("...");
        addSubCommand(new BuildingSectionCommand());
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        Faction faction = fPlayer.getFaction();
        if (faction == null) {
            return;
        }
        if (args.length == 1) {
            fPlayer.sendMessage("Usage: /f building <list|add|remove> <id>");
            return;
        }
        BuildSiteCache cache = Factions.get().getBuildSiteCache();
        if (args[1].equalsIgnoreCase("list")) {
            if (cache.get(fPlayer.getPlayer().getChunk().getChunkKey()) == null) {
                fPlayer.sendMessage("No buildings found in region.");
                return;
            }
            cache.get(fPlayer.getPlayer().getChunk().getChunkKey()).forEach(b ->
                    fPlayer.sendMessage(b.getBuilding().getId()));
            return;
        }
        if (args[1].equalsIgnoreCase("add")) {
            Building building = Factions.get().getBuildingManager().getById(args[2]);
            if (building == null) {
                fPlayer.sendMessage("Building not found.");
                return;
            }
            ItemStack item = BuildingManager.getBuildingItemStack(building, faction, fPlayer.getPlayer());
            fPlayer.getPlayer().getInventory().addItem(item);
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
