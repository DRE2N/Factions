package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import org.jetbrains.annotations.NotNull;

public class MoneyConsumption extends BuildingEffect {

    private final int amount;
    private final boolean shouldDisableBuilding;

    private boolean buildingIsDisabled = false;

    public MoneyConsumption(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        amount = data.getInt("amount");
        shouldDisableBuilding = data.getBoolean("disableBuilding", true);
    }

    @Override
    public void onPayday() {
        if (buildingIsDisabled) {
            buildingIsDisabled = false;
            enableOtherEffects();
        }
        if (faction.getFAccount().canAfford(amount)) {
            faction.getFAccount().withdraw(amount);
        } else if (shouldDisableBuilding && !buildingIsDisabled) {
            disableOtherEffects();
            buildingIsDisabled = true;
        }
    }
}
