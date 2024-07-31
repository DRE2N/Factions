package de.erethon.factions.building;

import org.bukkit.Location;

public record BuildSiteCoordinate(int x, int y, int z) {

    @Override
    public String toString() {
        return x + ";" + y + ";" + z;
    }

    public static BuildSiteCoordinate fromString(String string) {
        String[] split = string.split(";");
        return new BuildSiteCoordinate(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
    }

    public int distance(BuildSiteCoordinate other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y) + Math.abs(z - other.z);
    }

    public int distance(Location other) {
        int otherX = other.getBlockX();
        int otherY = other.getBlockY();
        int otherZ = other.getBlockZ();
        return Math.abs(x - otherX) + Math.abs(y - otherY) + Math.abs(z - otherZ);
    }
}
