package de.erethon.factions.statistic;

import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.economy.FEconomy;
import de.erethon.factions.util.FLogger;
import io.prometheus.client.Gauge;

/**
 * @author Fyreum
 */
public class FStatistics {

    public static final Gauge ALLIANCE_AMOUNT = Gauge.build("alliances_amount", "The current amount of alliances").register();
    public static final Gauge ALLIANCE_MONEY_AMOUNT = Gauge.build("alliances_money_amount", "The amount of money each alliance has").labelNames("alliance").register();

    public static final Gauge FACTIONS_AMOUNT = Gauge.build("factions_amount", "The current amount of factions").register();
    public static final Gauge FACTIONS_AVERAGE_MONEY_AMOUNT = Gauge.build("factions_average_money_amount", "The average amount of money of all factions").register();
    public static final Gauge FACTIONS_HIGHEST_MONEY_AMOUNT = Gauge.build("factions_highest_money_amount", "The highest amount of money a faction has").register();

    public static final Gauge REGIONS_AMOUNT = Gauge.build("regions_amount", "The current amount of regions").register();
    public static final Gauge REGIONS_PER_FACTION = Gauge.build("regions_per_faction", "The average amount of claimed regions per faction").register();

    private static final Factions plugin = Factions.get();

    public static void initialize() {
        FLogger.DEBUG.log("Initializing statistics...");
        ALLIANCE_AMOUNT.setChild(new Gauge.Child() {
            @Override
            public double get() {
                return plugin.getAllianceCache().getCache().size();
            }
        });
        FACTIONS_AMOUNT.setChild(new Gauge.Child() {
            @Override
            public double get() {
                return plugin.getFactionCache().getCache().size();
            }
        });
        REGIONS_AMOUNT.setChild(new Gauge.Child() {
            @Override
            public double get() {
                return plugin.getRegionManager().getCachedRegionsAmount();
            }
        });

        // Don't include the other stats here, as they are mathematically more complex and shouldn't be called so often
    }

    public static void update() {
        FLogger.DEBUG.log("Updating statistics...");
        for (Alliance alliance : plugin.getAllianceCache()) {
            ALLIANCE_MONEY_AMOUNT.labels(String.valueOf(alliance.getId())).set(alliance.getFAccount().getBalance(FEconomy.TAX_CURRENCY));
        }

        FACTIONS_AVERAGE_MONEY_AMOUNT.set(plugin.getFactionCache().getCache().values().stream().mapToDouble(f -> f.getFAccount().getBalance(FEconomy.TAX_CURRENCY)).average().orElse(0));
        FACTIONS_HIGHEST_MONEY_AMOUNT.set(plugin.getFactionCache().getCache().values().stream().mapToDouble(f -> f.getFAccount().getBalance(FEconomy.TAX_CURRENCY)).max().orElse(0));

        REGIONS_PER_FACTION.set(plugin.getFactionCache().getCache().values().stream().mapToInt(f -> f.getRegions().size()).average().orElse(0));
    }

}
