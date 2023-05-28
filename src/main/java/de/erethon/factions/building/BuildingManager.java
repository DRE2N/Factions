package de.erethon.factions.building;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.region.Region;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BuildingManager {

    Factions plugin = Factions.get();

    private List<Building> buildings = new CopyOnWriteArrayList<>();
    private List<BuildSite> buildingTickets = new ArrayList<>();

    public BuildingManager(File dir) {
        load(dir);
    }

    public Building getByID(String id) {
        for (Building building : buildings) {
            if (building.getId().equals(id)) {
                return building;
            }
        }
        return null;
    }

    public List<Building> getBuildings() {
        return buildings;
    }

    public void load(File dir) {
        if (dir.listFiles() == null) {
            MessageUtil.log("No buildings found. Please create some.");
            return;
        }
        for (File file : dir.listFiles()) {
            buildings.add(new Building(file));
        }
        MessageUtil.log("Loaded " + buildings.size() + " Buildings.");
    }

    public void deleteBuilding(Player player) {
        Region rg = plugin.getFPlayerCache().getByPlayer(player).getLastRegion();
        if (rg == null) {
            MessageUtil.sendMessage(player, "&cNot in Region.");
            return;
        }
        BuildSite buildSite = getBuildSite(player.getTargetBlockExact(20), rg);
        if (buildSite == null) {
            MessageUtil.sendMessage(player, "&cNot a build site.");
        }
        buildSite.getRegion().getBuildSites().remove(buildSite);
        buildSite.getRegion().getOwner().getFactionBuildings().remove(buildSite);
        MessageUtil.sendMessage(player, "&aBuildSite deleted.");

    }

    public BuildSite getBuildSite(Location loc, Region region) {
        for (BuildSite buildSite : region.getBuildSites()) {
            if (buildSite.isInBuildSite(loc)) {
                return buildSite;
            }
        }
        return null;
    }

    public boolean hasOverlap(Location corner1, Location corner2, BuildSite existingSite) {
        return existingSite.isInBuildSite(corner1) || existingSite.isInBuildSite(corner2);
    }

    public BuildSite getBuildSite(Block check, Region region) {
        for (BuildSite buildSite : region.getBuildSites()) {
            if (buildSite.getInteractive().equals(check.getLocation())) {
                return buildSite;
            }
        }
        return null;
    }

    public List<BuildSite> getBuildingTickets() {
        return buildingTickets;
    }
}