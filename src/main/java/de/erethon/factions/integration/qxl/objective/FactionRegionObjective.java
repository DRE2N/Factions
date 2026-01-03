package de.erethon.factions.integration.qxl.objective;

import de.erethon.factions.event.FPlayerCrossRegionEvent;
import de.erethon.factions.region.Region;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.objective.ActiveObjective;
import org.bukkit.entity.Player;

@QLoadableDoc(
        value = "faction_region",
        description = "Objective completed when the player enters a specific faction region.",
        shortExample = "faction_region: region=Arvisburg",
        longExample = {
                "faction_region:",
                "  region: Theanor"
        })
public class FactionRegionObjective extends FBaseObjective<FPlayerCrossRegionEvent> {

    private Region region;

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        return QTranslatable.fromString("en=Enter Region" + region.getName() + "; de=Betrete Region " + region.getName());
    }

    @Override
    public Class<FPlayerCrossRegionEvent> getEventType() {
        return FPlayerCrossRegionEvent.class;
    }

    @Override
    public void check(ActiveObjective activeObjective, FPlayerCrossRegionEvent fPlayerCrossRegionEvent) {
        if  (!conditions(fPlayerCrossRegionEvent.getFPlayer().getPlayer())) return;
        if (fPlayerCrossRegionEvent.getNewRegion() != null && fPlayerCrossRegionEvent.getNewRegion().equals(region)) {
            checkCompletion(activeObjective, this, getQPlayer(fPlayerCrossRegionEvent.getFPlayer()));
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        String regionName = cfg.getString("region");
        region = regionCache.getByName(regionName);
        if  (region == null) {
            throw new RuntimeException("Region " + regionName + " does not exist. (FactionRegionObjective in " + id() + ")");
        }
    }
}
