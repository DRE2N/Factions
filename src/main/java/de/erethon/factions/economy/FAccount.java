package de.erethon.factions.economy;

import de.erethon.tyche.models.Transaction;

import java.util.UUID;

/**
 * @author Fyreum
 */
public interface FAccount {

    /**
     * @param amount the amount of money to check for
     */
    boolean canAfford(double amount);

    boolean canAfford(double amount, String currencyId);

    Transaction deposit(double amount, String currencyId, String logReason, UUID initiator);

    Transaction withdraw(double amount, String currencyId, String logReason, UUID initiator);

    double getBalance(String currencyId);

    /**
     * @param amount the amount of money to set
     */
    void setBalance(double amount, String currencyId);

    String getFormatted(double amount);
}
