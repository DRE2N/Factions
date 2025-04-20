package de.erethon.factions.util;

public class IntRange {

    private final double min;
    private final double max;

    public IntRange(double min, double max) {
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
    }

    public boolean containsDouble(double value) {
        return value >= min && value <= max;
    }

    public boolean containsInt(int value) {
        return value >= (int) min && value <= (int) max;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public int getMinimumInteger() {
        return (int) Math.floor(min);
    }

    public int getMaximumInteger() {
        return (int) Math.ceil(max);
    }

    @Override
    public String toString() {
        return "IntRange{" +
                "min=" + min +
                ", max=" + max +
                '}';
    }
}
