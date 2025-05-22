package de.erethon.factions.economy;

import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.event.FactionDisbandEvent;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.util.FUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.time.ZonedDateTime;

/**
 * @author Fyreum
 */
public class TaxManager {

    final Factions plugin = Factions.get();
    private BukkitTask factionTaxTask;

    public void runFactionTaxTask() {
        if (factionTaxTask != null) {
            factionTaxTask.cancel();
        }
        if (factionTaxTask == null) { // Only run it when its actually triggered by the scheduler
            return;
        }
        taxFactions();
        ZonedDateTime now = FUtil.getDateTime();
        ZonedDateTime taxTime = FUtil.getNoonDateTime();
        if (now.isAfter(taxTime)) {
            taxTime = taxTime.plusDays(1);
        }
        long delay = taxTime.toInstant().toEpochMilli() - now.toInstant().toEpochMilli();
        factionTaxTask = Bukkit.getScheduler().runTaskLater(plugin, this::runFactionTaxTask, delay); // Run this on the main thread, as effects might spawn entities, access blocks, etc.
    }

    // This method is called each day at 12:00h.
    public void taxFactions() {
        for (Faction faction : plugin.getFactionCache()) {
            if (!faction.hasAlliance()) {
                FLogger.ECONOMY.log("Faction " + faction.getName() + " has no alliance, skipped.");
                continue;
            }
            Alliance alliance = faction.getAlliance();
            FAccount fAccount = faction.getFAccount();
            faction.getEconomy().doEconomyCalculations();
            double amount = faction.calculateRegionTaxes();

            if (amount <= 0) {
                continue;
            }
            if (fAccount.getBalance() > 0 && faction.getCurrentTaxDebt() > 0) {
                if (fAccount.canAfford(faction.getCurrentTaxDebt())) {
                    fAccount.withdraw(faction.getCurrentTaxDebt());
                    FLogger.ECONOMY.log("Faction '" + faction.getId() + "' paid off all their debt of " + faction.getCurrentTaxDebt());
                    faction.setCurrentTaxDebt(0);
                } else {
                    faction.removeCurrentTaxDebt(fAccount.getBalance());
                    FLogger.ECONOMY.log("Faction '" + faction.getId() + "' partially paid off their debt of " + fAccount.getBalance() + ": Missing " + faction.getCurrentTaxDebt());
                    fAccount.setBalance(0);
                }
            }
            if (fAccount.canAfford(amount)) {
                fAccount.withdraw(amount);
                alliance.getFAccount().deposit(amount * plugin.getFConfig().getTaxConversionRate());
                FLogger.ECONOMY.log("Faction '" + faction.getId() + "' was taxed " + amount);
                faction.sendMessage(FMessage.FACTION_INFO_PAYED_DAILY_TAXES.message(fAccount.getFormatted(amount)));
                continue;
            }
            double missingAmount = amount - fAccount.getBalance();
            alliance.getFAccount().deposit(fAccount.getBalance());
            fAccount.setBalance(0);
            faction.addCurrentTaxDebt(missingAmount);
            FLogger.ECONOMY.log("Faction '" + faction.getId() + "' was not able to pay the taxed amount of " + amount + ": Missing " + missingAmount);
            alliance.sendMessage(FMessage.ALLIANCE_INFO_FACTION_CANNOT_AFFORD_DAILY_TAXES.message());

            if (faction.getCurrentTaxDebt() > plugin.getFConfig().getMaximumFactionDebts().get(faction.getLevel())) {
                faction.disband(FactionDisbandEvent.Reason.TOO_MUCH_TAX_DEBT);
            }
        }
    }

}
