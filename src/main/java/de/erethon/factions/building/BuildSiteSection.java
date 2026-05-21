package de.erethon.factions.building;

import io.papermc.paper.math.Position;
import org.bukkit.Location;

public record BuildSiteSection(String name, Position corner1, Position corner2, boolean protectedSection) {

    public boolean contains(Position position) {
        return contains(position.x(), position.y(), position.z());
    }

    public boolean contains(double x, double y, double z) {
        return x >= Math.min(corner1.x(), corner2.x()) && x <= Math.max(corner1.x(), corner2.x()) &&
                y >= Math.min(corner1.y(), corner2.y()) && y <= Math.max(corner1.y(), corner2.y()) &&
                z >= Math.min(corner1.z(), corner2.z()) && z <= Math.max(corner1.z(), corner2.z());
    }

    public boolean contains(Location location) {
        return contains(location.x(), location.y(), location.z());
    }
}
