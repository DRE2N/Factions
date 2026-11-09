package de.erethon.factions.building;

import de.erethon.factions.faction.Faction;
import de.erethon.factions.util.FLogger;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Changes only effects affected by a choice, preserving the other effects' runtime state. */
final class BuildingSiteConfiguration {
    private BuildingSiteConfiguration() {
    }

    static boolean change(Player player, BuildSite site, String settingId, String optionId) {
        if (!site.canConfigure(player)) return false;
        BuildingUserConfig.Setting setting = site.getBuilding().getUserConfig().setting(settingId);
        if (setting == null) return false;
        BuildingUserConfig.Option option = setting.options().get(optionId);
        Faction owner = site.getFaction();
        if (option == null || !option.available(owner)) return false;
        String path = "userConfig." + setting.id();
        Object previous = site.get(path);
        if (option.id().equals(setting.selected(site.getString(path)).id())) return true;

        site.set(path, option.id());
        List<BuildingEffectData> desired = site.getConfiguredEffects();
        List<BuildingEffect> removed = site.getEffects().stream()
                .filter(effect -> !desired.contains(effect.data)).toList();
        List<BuildingEffect> added = new ArrayList<>();
        try {
            for (BuildingEffectData data : desired) {
                if (site.getEffects().stream().anyMatch(effect -> effect.data == data)) continue;
                BuildingEffect effect = data.newEffect(site);
                if (effect == null) throw new IllegalArgumentException("Cannot instantiate " + data.getId());
                added.add(effect);
            }
            // Persist before replacing live effects. Failed saves leave the existing production intact.
            site.save();
        } catch (IOException | RuntimeException exception) {
            owner.getBuildingEffects().removeAll(added);
            owner.getTickingBuildingEffects().removeAll(added);
            site.set(path, previous);
            FLogger.ERROR.log("Cannot change building setting " + settingId + ": " + exception.getMessage());
            return false;
        }

        removed.forEach(BuildingEffect::remove);
        owner.getBuildingEffects().removeAll(removed);
        owner.getTickingBuildingEffects().removeAll(removed);
        site.getEffects().removeAll(removed);
        site.getEffects().addAll(added);
        for (BuildingEffect effect : added) {
            effect.getRequiredContainers().forEach(site::requireContainer);
            effect.apply();
        }
        site.onChunkLoad();
        site.scheduleProgressUpdate();
        site.updateHolo();
        return true;
    }
}
