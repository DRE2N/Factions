package de.erethon.factions.region.schematic;

import de.erethon.bedrock.misc.FileUtil;
import io.papermc.paper.math.Position;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Fyreum
 */
@SuppressWarnings("UnstableApiUsage")
public class RegionSchematicManager {

    private final Map<String, RegionSchematic> schematics = new HashMap<>();

    public RegionSchematicManager(@NotNull File folder) {
        for (File file : FileUtil.getFilesForFolder(folder)) {
            RegionSchematic schematic = new RegionSchematic(file);
            schematics.put(schematic.getName(), schematic);
        }
    }

    public @NotNull RegionSchematic create(@NotNull String name, @NotNull Position a, @NotNull Position b) {
        int xLength = Math.max(a.blockX(), b.blockX()) - Math.min(a.blockX(), b.blockX()),
                yLength = Math.max(a.blockY(), b.blockY()) - Math.min(a.blockY(), b.blockY()),
                zLength = Math.max(a.blockZ(), b.blockZ()) - Math.min(a.blockZ(), b.blockZ());
        RegionSchematic schematic = new RegionSchematic(name, xLength, yLength, zLength);
        schematics.put(name, schematic);
        return schematic;
    }

    public void saveAll() {
        for (RegionSchematic schematic : schematics.values()) {
            schematic.save();
        }
    }

    /* Getters and setters */

    public @Nullable RegionSchematic getSchematic(@NotNull String name) {
        return schematics.get(name);
    }
}
