package de.erethon.factions.portal;

import de.erethon.bedrock.misc.FileUtil;
import de.erethon.factions.util.FLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Fyreum
 */
public class PortalManager {

    private final File folder;
    private final Map<Integer, Portal> portals = new HashMap<>();

    public PortalManager(@NotNull File folder) {
        this.folder = folder;
    }

    public@NotNull Portal createPortal() {
        int id = generateId();
        File file = new File(folder, id + ".yml");
        Portal portal = new Portal(file, id);
        portals.put(id, portal);
        return portal;
    }

    public void loadAll() {
        for (File file : FileUtil.getFilesForFolder(folder)) {
            int id;
            try {
                id = Integer.parseInt(file.getName().replace(".yml", ""));
            } catch (NumberFormatException e) {
                FLogger.ERROR.log("Invalid portal id found: " + file.getName());
                continue;
            }
            if (portals.containsKey(id)) {
                FLogger.ERROR.log("Duplicate portal id found: " + id);
                continue;
            }
            portals.put(id, new Portal(file, id));
        }
        FLogger.INFO.log("Loaded " + portals.size() + " portals");
    }

    public void saveAll() {
        for (Portal portal : portals.values()) {
            portal.saveData();
        }
    }

    public int generateId() {
        int id = 0;
        while (portals.containsKey(id)) {
            id++;
        }
        return id;
    }

    /* Getters */

    public @NotNull Map<Integer, Portal> getPortals() {
        return portals;
    }

    public @Nullable Portal getById(int id) {
        return portals.get(id);
    }

}
