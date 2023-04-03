package de.erethon.factions.economy;

/**
 * @author Fyreum
 */
public interface FAccount {

    /**
     * @param amount the amount of money to check for
     */
    boolean canAfford(double amount);

    /**
     * @param amount the amount of money to deposit
     */
    void deposit(double amount);

    /**
     * @param amount the amount of money to withdraw
     */
    void withdraw(double amount);

    /**
     * @return the amount of money that this account stores
     */
    double getBalance();

    /**
     * @param amount the amount of money to set
     */
    void setBalance(double amount);

    /**
     * @return a formatted balance String
     */
    default String getFormatted() {
        return getFormatted(getBalance());
    }

    /**
     * @return a formatted balance string of the specified amount
     */
    String getFormatted(double amount);
}
