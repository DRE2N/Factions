package de.erethon.factions.building.effects;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.building.Building;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.faction.Faction;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class ResourceChangeEffect extends BuildingEffect {

    int test = 0;

    public ResourceChangeEffect(Building building, ConfigurationSection section) {
        super(building, section);
    }

    @Override
    public void apply(@NotNull Faction faction) {
        MessageUtil.log("Applying resource change effect to " + faction.getName() + " with test value " + test);
    }

    @Override
    public void remove(@NotNull Faction faction) {
        MessageUtil.log("Removing resource change effect from " + faction.getName());
    }

    @Override
    public void load(@NotNull ConfigurationSection section) {
        super.load(section);
        test = section.getInt("test", 1);
    }
}
