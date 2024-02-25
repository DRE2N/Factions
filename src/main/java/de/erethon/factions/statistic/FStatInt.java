package de.erethon.factions.statistic;

import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public class FStatInt {

    private final String key;
    private int value;

    public FStatInt(@NotNull String key) {
        this.key = key;
        FStatistics.STATS.put(key, this);
    }

    /* Getters and setters */

    public @NotNull String getKey() {
        return key;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void add() {
        value++;
    }

    public void add(int amount) {
        value += amount;
    }

    public void remove() {
        value--;
    }

    public void remove(int amount) {
        value -= amount;
    }
}
