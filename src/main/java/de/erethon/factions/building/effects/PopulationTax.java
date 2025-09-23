package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.economy.FEconomy;
import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.faction.Faction;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class PopulationTax extends BuildingEffect {

    private final Map<PopulationLevel, Double> taxes = new HashMap<>();
    private final double maximumTotal;

    public PopulationTax(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        for (PopulationLevel level : PopulationLevel.values()) {
            taxes.put(level, data.getDouble("taxes." + level.name().toLowerCase(), 0.0));
        }
        maximumTotal = data.getDouble("maximumTotal", 10);
    }

    // Taxes before the rest of the payday to prevent factions from going into debt despite enough income
    @Override
    public void onPrePayday() {
        payTaxes();
    }

    private void payTaxes() {
        double total = 0;
        for (PopulationLevel level : PopulationLevel.values()) {
            double tax = taxes.get(level);
            if (tax == 0) {
                continue;
            }
            Faction faction = site.getFaction();
            if (faction == null) {
                continue;
            }
            double baseAmount = tax * faction.getPopulation(level);
            double amount = baseAmount * Math.max(faction.getHappiness(level), 1);
            total += amount;
            // Limit the total amount of tax money that can be received from a single effect
            if (total > maximumTotal) {
                amount = maximumTotal;
            }
            if (amount == 0) {
                continue;
            }
            faction.getFAccount().deposit(Math.min(1, amount), FEconomy.TAX_CURRENCY, "Population tax for " + level.name().toLowerCase(), site.getUuid());
        }
    }
}
