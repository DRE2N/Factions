package de.erethon.factions.building;

import io.papermc.paper.math.Position;
import org.bukkit.Location;

public record BuildSiteSection(String name, Position corner1, Position corner2, boolean protectedSection) {

    public boolean contains(Position position) {
        return position.x() >= corner1.x() && position.x() <= corner2.x() &&
                position.y() >= corner1.y() && position.y() <= corner2.y() &&
                position.z() >= corner1.z() && position.z() <= corner2.z();
    }

    public boolean contains(double x, double y, double z) {
        return x >= corner1.x() && x <= corner2.x() &&
                y >= corner1.y() && y <= corner2.y() &&
                z >= corner1.z() && z <= corner2.z();
    }

    public boolean contains(Location location) {
        return contains(location.x(), location.y(), location.z());
    }
}
