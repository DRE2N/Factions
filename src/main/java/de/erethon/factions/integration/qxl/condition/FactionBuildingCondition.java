package de.erethon.factions.integration.qxl.condition;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.Building;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;

import java.util.Set;

@QLoadableDoc(
        value = "faction_building",
        description = "Checks if the player's faction has a certain number of a specific building. Only counts active buildings",
        shortExample = "faction_building: building=town_hall; amount=2",
        longExample = {
                "faction_building:",
                "  building: town_hall",
                "  amount: 2"
        })
public class FactionBuildingCondition extends FBaseCondition {

    @QParamDoc(name = "building", description = "The ID of the building to check for.")
    private Building building;
    @QParamDoc(name = "amount", description = "The amount of this building the faction must have.", def = "1")
    private int amount = 1;

    @Override
    public boolean check(Quester quester) {
        FPlayer fPlayer = getFPlayer(quester);
        if (fPlayer == null) {
            return fail(quester);
        }
        Faction faction = fPlayer.getFaction();
        if (faction == null) {
            return fail(quester);
        }
        Set<BuildSite> sites = faction.getFactionBuildings();
        int count = 0;
        for (BuildSite site : sites) {
            if (site.getBuilding().equals(building) && site.isActive()) {
                count++;
            }
        }
        if (count >= amount) {
            return success(quester);
        }
        return fail(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        String buildingId = cfg.getString("building");
        building = buildingManager.getById(buildingId);
        amount = cfg.getInt("amount", 1);
        if  (building == null) {
            throw new RuntimeException("Faction building " + buildingId + " does not exist.");
        }
    }
}
