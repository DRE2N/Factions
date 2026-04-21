package de.erethon.factions.blocklog.mapupdate;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Renders cuboid zone outlines with stretched BlockDisplay entities.
 */
public class MapUpdateSelectionVisualizer {

    private static final float EDGE_THICKNESS = 0.33f;

    public enum ZoneType {
        INCLUDE(Material.GREEN_CONCRETE),
        PROTECT(Material.RED_CONCRETE),;

        private final Material material;

        ZoneType(Material material) {
            this.material = material;
        }

        public Material getMaterial() {
            return material;
        }
    }

    public @NotNull List<UUID> spawnOutline(@NotNull World world,
                                            @NotNull CuboidRegion zone,
                                            @NotNull ZoneType zoneType) {
        BlockVector3 min = zone.getMinimumPoint();
        BlockVector3 max = zone.getMaximumPoint();

        int lengthX = max.x() - min.x() + 1;
        int lengthY = max.y() - min.y() + 1;
        int lengthZ = max.z() - min.z() + 1;

        List<UUID> ids = new ArrayList<>(12);

        // X edges
        ids.add(spawnEdge(world, min.x(), min.y(), min.z(), lengthX, EDGE_THICKNESS, EDGE_THICKNESS, zoneType));
        ids.add(spawnEdge(world, min.x(), min.y(), max.z(), lengthX, EDGE_THICKNESS, EDGE_THICKNESS, zoneType));
        ids.add(spawnEdge(world, min.x(), max.y(), min.z(), lengthX, EDGE_THICKNESS, EDGE_THICKNESS, zoneType));
        ids.add(spawnEdge(world, min.x(), max.y(), max.z(), lengthX, EDGE_THICKNESS, EDGE_THICKNESS, zoneType));

        // Y edges
        ids.add(spawnEdge(world, min.x(), min.y(), min.z(), EDGE_THICKNESS, lengthY, EDGE_THICKNESS, zoneType));
        ids.add(spawnEdge(world, min.x(), min.y(), max.z(), EDGE_THICKNESS, lengthY, EDGE_THICKNESS, zoneType));
        ids.add(spawnEdge(world, max.x(), min.y(), min.z(), EDGE_THICKNESS, lengthY, EDGE_THICKNESS, zoneType));
        ids.add(spawnEdge(world, max.x(), min.y(), max.z(), EDGE_THICKNESS, lengthY, EDGE_THICKNESS, zoneType));

        // Z edges
        ids.add(spawnEdge(world, min.x(), min.y(), min.z(), EDGE_THICKNESS, EDGE_THICKNESS, lengthZ, zoneType));
        ids.add(spawnEdge(world, min.x(), max.y(), min.z(), EDGE_THICKNESS, EDGE_THICKNESS, lengthZ, zoneType));
        ids.add(spawnEdge(world, max.x(), min.y(), min.z(), EDGE_THICKNESS, EDGE_THICKNESS, lengthZ, zoneType));
        ids.add(spawnEdge(world, max.x(), max.y(), min.z(), EDGE_THICKNESS, EDGE_THICKNESS, lengthZ, zoneType));

        return ids;
    }

    private UUID spawnEdge(@NotNull World world,
                           double x,
                           double y,
                           double z,
                           float scaleX,
                           float scaleY,
                           float scaleZ,
                           @NotNull ZoneType zoneType) {
        BlockDisplay display = world.spawn(new Location(world, x, y, z), BlockDisplay.class, entity -> {
            entity.setBlock(zoneType.getMaterial().createBlockData());
            entity.setInterpolationDuration(0);
            entity.setInterpolationDelay(0);
            entity.setTeleportDuration(0);
            entity.setPersistent(false);
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f(0f, 0f, 0f, 1f),
                    new Vector3f(scaleX, scaleY, scaleZ),
                    new AxisAngle4f(0f, 0f, 0f, 1f)
            ));
        });
        return display.getUniqueId();
    }
}

