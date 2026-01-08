package de.erethon.factions.economy;

import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.faction.Faction;
import de.erethon.tyche.EconomyService;
import de.erethon.tyche.models.OwnerType;
import de.erethon.tyche.models.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * @author Fyreum
 */
public class FAccountImpl implements FAccount {

    final EconomyService economyService = Factions.get().getEconomyService();

    protected final UUID accountId;

    /**
     * @param faction the faction to create an account for
     */
    public FAccountImpl(@NotNull Faction faction) {
        this(faction.getUniqueId());
    }

    /**
     * @param alliance the alliance to create an account for
     */
    public FAccountImpl(@NotNull Alliance alliance) {
        this(alliance.getUniqueId());
    }

    /**
     * @param name the name to create an account fo
     */
    public FAccountImpl(UUID accountId) {
        this.accountId = accountId;
    }

    @Override
    public boolean canAfford(double amount) {
        return false;
    }

    @Override
    public boolean canAfford(double amount, String currencyId) {
        return economyService.getBalance(accountId, OwnerType.FACTION, currencyId).join() >= amount;
    }

    @Override
    public @Nullable Transaction deposit(double amount, String currencyId, String logReason, UUID initiator) {
        if (amount == 0) {
            return null;
        }
        if (amount < 0) {
            withdraw(amount, currencyId, logReason, initiator);
        }
        return economyService.deposit(accountId, OwnerType.FACTION, currencyId, (long) amount, logReason, null).join();
    }

    @Override
    public @Nullable Transaction withdraw(double amount, String currencyId, String logReason, UUID initiator) {
        if (amount == 0) {
            return null;
        }
        if (amount < 0) {
            deposit(-amount, currencyId, logReason, initiator);
        }
        return economyService.withdraw(accountId, OwnerType.FACTION, currencyId, (long) amount, logReason, null).join();
    }

    @Override
    public double getBalance(String currencyId) {
        return economyService.getBalance(accountId, OwnerType.FACTION, currencyId).join();
    }

    @Override
    public void setBalance(double amount, String currencyId) {
        withdraw(getBalance(currencyId), FEconomy.TAX_CURRENCY, "Set balance for " + accountId, null);
        deposit(amount, currencyId, "Set balance for " + accountId, null);
    }

    @Override
    public String getFormatted(double amount) {
        return "";
    }

}
