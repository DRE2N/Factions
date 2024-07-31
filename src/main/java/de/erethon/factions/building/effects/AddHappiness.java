package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.economy.population.HappinessModifier;
import de.erethon.factions.economy.population.PopulationLevel;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class AddHappiness extends BuildingEffect {

    private HappinessModifier modifier;

    public AddHappiness(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        HashMap<PopulationLevel, Double> levels = new HashMap<>();
        for (String level : data.getConfigurationSection("levels").getKeys(false)) {
            levels.put(PopulationLevel.valueOf(level), data.getDouble("levels." + level));
        }
        modifier = new HappinessModifier(data.getString("name") + site.getBuilding().getId(), Component.translatable("factions.buildings.building." + site.getBuilding().getId() + ".name"), levels);
    }

    @Override
    public void apply() {
        faction.getEconomy().addOrReplaceHappinessModifier(modifier);
    }

    @Override
    public void remove() {
        faction.getEconomy().removeHappinessModifier(modifier);
    }
}
