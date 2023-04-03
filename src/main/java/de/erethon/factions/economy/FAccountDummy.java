package de.erethon.factions.economy;

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
    public void deposit(double amount) {
    }

    @Override
    public void withdraw(double amount) {
    }

    @Override
    public double getBalance() {
        return 0;
    }

    @Override
    public void setBalance(double amount) {
    }

    @Override
    public String getFormatted(double amount) {
        return String.valueOf(amount);
    }
}
