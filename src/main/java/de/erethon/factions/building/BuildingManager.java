package de.erethon.factions.building;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.region.Region;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BuildingManager {

    Factions plugin = Factions.get();

    private final List<Building> buildings = new CopyOnWriteArrayList<>();
    private final List<BuildSite> buildingTickets = new ArrayList<>();

    public BuildingManager(@NotNull File dir) {
        load(dir);
    }

    public @Nullable Building getById(@NotNull String id) {
        for (Building building : buildings) {
            if (building.getId().equals(id)) {
                return building;
            }
        }
        return null;
    }

    public @NotNull List<Building> getBuildings() {
        return buildings;
    }

    public void load(@NotNull File dir) {
        if (dir.listFiles() == null) {
            MessageUtil.log("No buildings found. Please create some.");
            return;
        }
        for (File file : dir.listFiles()) {
            buildings.add(new Building(file));
        }
        MessageUtil.log("Loaded " + buildings.size() + " Buildings.");
    }

    public void deleteBuilding(@NotNull Player player) {
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
        Faction owner = buildSite.getRegion().getOwner();
        owner.getFactionBuildings().remove(buildSite);
        MessageUtil.sendMessage(player, "&aBuildSite deleted.");
    }

    public @Nullable BuildSite getBuildSite(@NotNull Location loc, @NotNull Region region) {
        for (BuildSite buildSite : region.getBuildSites()) {
            if (buildSite.isInBuildSite(loc)) {
                return buildSite;
            }
        }
        return null;
    }

    public boolean hasOverlap(@NotNull Location corner1, @NotNull Location corner2, @NotNull BuildSite existingSite) {
        return existingSite.isInBuildSite(corner1) || existingSite.isInBuildSite(corner2);
    }

    @Contract("null, _ -> null; !null, _ -> _")
    public @Nullable BuildSite getBuildSite(@Nullable Block check, @NotNull Region region) {
        if (check == null) {
            return null;
        }
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