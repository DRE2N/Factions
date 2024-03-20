package de.erethon.factions.statistic;

import de.erethon.factions.Factions;
import de.erethon.factions.util.FLogger;
import io.prometheus.client.Gauge;

/**
 * @author Fyreum
 */
public class FStatistics {

    public static final Gauge ALLIANCE_AMOUNT = Gauge.build("alliances_amount", "The current amount of alliances").register();
    public static final Gauge ALLIANCE_AVERAGE_MONEY_AMOUNT = Gauge.build("alliances_average_money_amount", "The average amount of money of all alliances").register();
    public static final Gauge ALLIANCE_HIGHEST_MONEY_AMOUNT = Gauge.build("alliances_highest_money_amount", "The highest amount of money an alliance has").register();

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
        REGIONS_AMOUNT.setChild(new Gauge.Child() {
            @Override
            public double get() {
                return plugin.getRegionManager().getCachedRegionsAmount();
            }
        });
        FACTIONS_AMOUNT.setChild(new Gauge.Child() {
            @Override
            public double get() {
                return plugin.getFactionCache().getCache().size();
            }
        });

        // Don't include the other stats here, as they are mathematically more complex and shouldn't be called so often
    }

    public static void update() {
        FLogger.DEBUG.log("Updating statistics...");
        REGIONS_PER_FACTION.set(plugin.getFactionCache().getCache().values().stream().mapToInt(f -> f.getRegions().size()).average().orElse(0));

        ALLIANCE_AVERAGE_MONEY_AMOUNT.set(plugin.getAllianceCache().getCache().values().stream().mapToDouble(a -> a.getFAccount().getBalance()).average().orElse(0));
        ALLIANCE_HIGHEST_MONEY_AMOUNT.set(plugin.getAllianceCache().getCache().values().stream().mapToDouble(a -> a.getFAccount().getBalance()).max().orElse(0));

        FACTIONS_AVERAGE_MONEY_AMOUNT.set(plugin.getFactionCache().getCache().values().stream().mapToDouble(f -> f.getFAccount().getBalance()).average().orElse(0));
        FACTIONS_HIGHEST_MONEY_AMOUNT.set(plugin.getFactionCache().getCache().values().stream().mapToDouble(f -> f.getFAccount().getBalance()).max().orElse(0));
    }

}
