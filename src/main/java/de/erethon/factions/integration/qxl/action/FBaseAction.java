package de.erethon.factions.integration.qxl.action;

import de.erethon.factions.Factions;
import de.erethon.factions.alliance.AllianceCache;
import de.erethon.factions.building.BuildingManager;
import de.erethon.factions.faction.FactionCache;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.player.FPlayerCache;
import de.erethon.factions.region.RegionCache;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QBaseAction;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.data.QDatabaseManager;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Bukkit;

public abstract class FBaseAction extends QBaseAction {

    protected QuestsXL plugin = QuestsXL.get();
    protected QDatabaseManager databaseManager = plugin.getDatabaseManager();
    protected Factions factions = Factions.get();
    protected FactionCache factionCache = factions.getFactionCache();
    protected AllianceCache allianceCache = factions.getAllianceCache();
    protected RegionCache regionCache = factions.getRegionManager().getCache(Bukkit.getWorlds().getFirst());
    protected FPlayerCache fPlayerCache = factions.getFPlayerCache();
    protected BuildingManager buildingManager = factions.getBuildingManager();

    protected QPlayer getQPlayer(FPlayer fPlayer) {
        return databaseManager.getCurrentPlayer(fPlayer.getPlayer());
    }

    protected FPlayer getFPlayer(Quester quester) {
        if (!(quester instanceof QPlayer qPlayer)) {
            return null;
        }
        return fPlayerCache.getByPlayer(qPlayer.getPlayer());
    }
}
