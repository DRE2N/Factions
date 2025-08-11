package de.erethon.factions.economy;

import de.erethon.tyche.models.Transaction;

import java.util.UUID;

/**
 * @author Fyreum
 */
public class FAccountDummy implements FAccount {

    public static final FAccountDummy INSTANCE = new FAccountDummy();

    @Override
    public boolean canAfford(double amount) {
        return true;
    }

    @Override
    public boolean canAfford(double amount, String currencyId) {
        return true;
    }

    @Override
    public Transaction deposit(double amount, String currencyId, String logReason, UUID initiator) {
        return null;
    }

    @Override
    public Transaction withdraw(double amount, String currencyId, String logReason, UUID initiator) {
        return null;
    }

    @Override
    public double getBalance(String currencyId) {
        return 0;
    }

    @Override
    public void setBalance(double amount, String currencyId) {

    }


    @Override
    public String getFormatted(double amount) {
        return String.valueOf(amount);
    }
}
