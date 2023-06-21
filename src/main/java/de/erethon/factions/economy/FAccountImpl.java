package de.erethon.factions.economy;

import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.faction.Faction;
import net.milkbowl.vault.economy.Economy;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public class FAccountImpl implements FAccount {

    final Economy economy = Factions.get().getEconomyProvider();

    protected final String name;

    /**
     * @param faction the faction to create an account for
     */
    public FAccountImpl(@NotNull Faction faction) {
        this("faction-" + faction.getId());
    }

    /**
     * @param alliance the alliance to create an account for
     */
    public FAccountImpl(@NotNull Alliance alliance) {
        this("alliance-" + alliance.getId());
    }

    /**
     * @param name the name to create an account fo
     */
    public FAccountImpl(String name) {
        this.name = name;
    }

    @Override
    public boolean canAfford(double amount) {
        checkAccount();
        return economy.has(name, amount);
    }

    @Override
    public void deposit(double amount) {
        checkAccount();
        economy.depositPlayer(name, amount);
    }

    @Override
    public void withdraw(double amount) {
        checkAccount();
        economy.withdrawPlayer(name, amount);
    }

    @Override
    public double getBalance() {
        checkAccount();
        return economy.getBalance(name);
    }

    @Override
    public void setBalance(double amount) {
        withdraw(getBalance());
        deposit(amount);
    }

    @Override
    public String getFormatted(double amount) {
        return economy.format(amount);
    }

    private void checkAccount() {
        if (!economy.hasAccount(name)) {
            economy.createPlayerAccount(name);
        }
    }
}
