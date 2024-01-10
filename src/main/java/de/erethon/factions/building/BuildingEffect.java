package de.erethon.factions.building;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.faction.Faction;
import org.jetbrains.annotations.NotNull;

public class BuildingEffect {
    protected final BuildingEffectData effect;
    protected final BuildSite site;
    protected Faction faction;

    public BuildingEffect(@NotNull BuildingEffectData effect, BuildSite site) {
        this.effect = effect;
        this.site = site;
        this.faction = site.getRegion().getFaction();
    }

    public void tick() {
    }

    public void apply() {
    }

    public void remove() {
    }

    public @NotNull BuildingEffectData getEffect() {
        return effect;
    }

    public @NotNull BuildSite getSite() {
        return site;
    }

    public @NotNull Faction getFaction() {
        return faction;
    }

}
