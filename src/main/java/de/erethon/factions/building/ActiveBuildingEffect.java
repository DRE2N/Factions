package de.erethon.factions.building;

import org.jetbrains.annotations.NotNull;

public final class ActiveBuildingEffect {
    private final @NotNull BuildingEffect effect;
    private final @NotNull BuildSite site;

    public ActiveBuildingEffect(@NotNull BuildingEffect effect, @NotNull BuildSite site) {
        this.effect = effect;
        this.site = site;
        effect.apply(site.getRegion().getOwner());
    }

    public @NotNull BuildingEffect getEffect() {
        return effect;
    }

    public @NotNull BuildSite getSite() {
        return site;
    }

}
