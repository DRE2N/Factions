package de.erethon.factions.integration.qxl.condition;

import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;

@QLoadableDoc(
        value = "faction_region",
        description = "Checks if a quester is currently in a specific faction region.",
        shortExample = "faction_region: region=example_region",
        longExample = {
                "faction_region:",
                "  region: example_region",
        }
)
public class FactionRegionCondition extends FBaseCondition {

    @QParamDoc(name = "region", description = "The name of the faction region the quester must be in.", required = true)
    private Region region;

    @Override
    public boolean check(Quester quester) {
        FPlayer fPlayer = getFPlayer(quester);
        if (fPlayer != null) {
            if (fPlayer.getCurrentRegion() == region) {
                return success(quester);
            }
        }
        return fail(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        String regionId = cfg.getString("region");
        region = regionCache.getByName(regionId);
        if  (region == null) {
            throw new RuntimeException("Region " + regionId + " does not exist");
        }
    }
}
