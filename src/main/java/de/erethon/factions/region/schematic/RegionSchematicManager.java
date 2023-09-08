package de.erethon.factions.region.schematic;

import de.erethon.bedrock.misc.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Fyreum
 */
public class RegionSchematicManager {

    private final Map<String, RegionSchematic> schematics = new HashMap<>();

    public RegionSchematicManager(@NotNull File folder) {
        for (File file : FileUtil.getFilesForFolder(folder)) {
            RegionSchematic schematic = new RegionSchematic(file);
            schematics.put(schematic.getName(), schematic);
        }
    }

    public @NotNull RegionSchematic initializeSchematic(@NotNull String name) {
        RegionSchematic schematic = new RegionSchematic(name);
        schematics.put(name, schematic);
        return schematic;
    }

    public void saveAll() {
        for (RegionSchematic schematic : schematics.values()) {
            schematic.saveData();
        }
    }

    /* Getters and setters */

    public @Nullable RegionSchematic getSchematic(@NotNull String name) {
        return schematics.get(name);
    }
}
