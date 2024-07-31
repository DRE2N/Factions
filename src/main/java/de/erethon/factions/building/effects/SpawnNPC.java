package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import org.jetbrains.annotations.NotNull;

public class SpawnNPC extends BuildingEffect {
    public SpawnNPC(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
    }
}
