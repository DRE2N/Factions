package de.erethon.factions.building;

import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author Malfrador
 */
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

    public void onBreakBlock(FPlayer player, Block block, Set<BuildSiteSection> sections, Cancellable event) {
    }

    public void onPlaceBlock(FPlayer player, Block block, Set<BuildSiteSection> sections, Cancellable event) {
    }

    public void onEnter(FPlayer player) {
    }

    public void onLeave(FPlayer player) {
    }

    public void onFactionJoin(FPlayer player) {
    }

    public void onFactionLeave(FPlayer player) {
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
