package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import io.papermc.paper.math.Position;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public class SetFHome extends BuildingEffect {
    public SetFHome(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
    }

    @Override
    public void apply() {
        if (site.getNamedPositions().containsKey("fhome")) {
            Position pos = site.getNamedPositions().get("fhome");
            faction.setFHome(new Location(site.getInteractive().getWorld(), pos.x(), pos.y(), pos.z()));
        }
    }

    @Override
    public void remove() {
        faction.setFHome(null);
    }
}
