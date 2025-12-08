package de.erethon.factions.marker;

import com.google.gson.JsonObject;
import de.erethon.bedrock.config.EConfig;
import de.erethon.factions.util.FLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Marker {

    private final File file;
    private final MarkerCache markerCache;
    private int id;
    private String iconPath;
    private int x;
    private int z;
    private Map<String, String> names = new HashMap<>();
    private Map<String, String> descriptions = new HashMap<>();

    protected Marker(@NotNull MarkerCache markerCache, @NotNull File file, int id, @NotNull String iconPath, int x, int z) {
        this.file = file;
        this.markerCache = markerCache;
        this.id = id;
        this.iconPath = iconPath;
        this.x = x;
        this.z = z;
        this.names.put("en", "Marker");
        this.names.put("de", "Markierung");
        this.descriptions.put("en", "");
        this.descriptions.put("de", "");
    }

    protected Marker(@NotNull MarkerCache markerCache, @NotNull File file) {
        this.file = file;
        this.markerCache = markerCache;
    }

    protected void load() {
        YamlConfiguration config;
        try {
            config = YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            FLogger.ERROR.log("Failed to load marker from file " + file.getName() + ": " + e.getMessage());
            return;
        }
        try {
            this.id = Integer.parseInt(file.getName().replace(".yml", ""));
        } catch (NumberFormatException e) {
            FLogger.ERROR.log("Failed to load marker from file " + file.getName() + ": invalid ID");
            return;
        }
        this.iconPath = config.getString("iconPath", "lectern.png");
        this.x = config.getInt("x", 0);
        this.z = config.getInt("z", 0);

        ConfigurationSection namesSection = config.getConfigurationSection("names");
        if (namesSection != null) {
            for (String lang : namesSection.getKeys(false)) {
                names.put(lang, namesSection.getString(lang));
            }
        }

        ConfigurationSection descriptionsSection = config.getConfigurationSection("descriptions");
        if (descriptionsSection != null) {
            for (String lang : descriptionsSection.getKeys(false)) {
                descriptions.put(lang, descriptionsSection.getString(lang));
            }
        }
    }

    public void save() {
        YamlConfiguration section = new YamlConfiguration();
        section.set("iconPath", iconPath);
        section.set("x", x);
        section.set("z", z);

        ConfigurationSection namesSection = section.createSection("names");
        for (Map.Entry<String, String> entry : names.entrySet()) {
            namesSection.set(entry.getKey(), entry.getValue());
        }

        ConfigurationSection descriptionsSection = section.createSection("descriptions");
        for (Map.Entry<String, String> entry : descriptions.entrySet()) {
            descriptionsSection.set(entry.getKey(), entry.getValue());
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try {
            section.save(file);
        } catch (Exception e) {
            FLogger.ERROR.log("Failed to save marker '" + id + "' to file: " + e.getMessage());
        }
    }

    public boolean delete() {
        FLogger.INFO.log("Deleting marker '" + id + "'...");
        markerCache.removeMarker(this);
        return file.delete();
    }

    public @NotNull JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("iconPath", iconPath);
        json.addProperty("x", x);
        json.addProperty("z", z);

        JsonObject namesJson = new JsonObject();
        for (Map.Entry<String, String> entry : names.entrySet()) {
            namesJson.addProperty(entry.getKey(), entry.getValue());
        }
        json.add("names", namesJson);

        JsonObject descriptionsJson = new JsonObject();
        for (Map.Entry<String, String> entry : descriptions.entrySet()) {
            descriptionsJson.addProperty(entry.getKey(), entry.getValue());
        }
        json.add("descriptions", descriptionsJson);

        return json;
    }

    /* Getters */

    public int getId() {
        return id;
    }

    public @NotNull String getIconPath() {
        return iconPath;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public @Nullable String getName(@NotNull String language) {
        return names.get(language);
    }

    public @NotNull Map<String, String> getNames() {
        return names;
    }

    public @Nullable String getDescription(@NotNull String language) {
        return descriptions.get(language);
    }

    public @NotNull Map<String, String> getDescriptions() {
        return descriptions;
    }

    /* Setters */

    public void setIconPath(@NotNull String iconPath) {
        this.iconPath = iconPath;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public void setPosition(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public void setName(@NotNull String language, @NotNull String name) {
        this.names.put(language, name);
    }

    public void setDescription(@NotNull String language, @NotNull String description) {
        this.descriptions.put(language, description);
    }
}

