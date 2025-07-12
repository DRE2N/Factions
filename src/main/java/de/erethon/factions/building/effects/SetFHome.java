package de.erethon.factions.building.effects;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.region.RegionPOIContainer;
import de.erethon.factions.region.RegionPOIType;
import io.papermc.paper.math.Position;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public class SetFHome extends BuildingEffect {

    RegionPOIContainer container;

    public SetFHome(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
    }

    @Override
    public void apply() {
        if (site.getNamedPositions().containsKey("fhome")) {
            Position pos = site.getNamedPositions().get("fhome");
            if (pos == null) {
                MessageUtil.log("SetFHome effect requires a named position 'fhome' in the build site " + site.getBuilding().getId() + ".");
                return;
            }
            faction.setFHome(new Location(site.getInteractive().getWorld(), pos.x(), pos.y(), pos.z()));
            container = new RegionPOIContainer(pos, site);
            site.getRegion().addPOI(RegionPOIType.F_HOME, container);
        }
    }

    @Override
    public void remove() {
        faction.setFHome(null);
        site.getRegion().removePOI(RegionPOIType.F_HOME, container);
    }
}
