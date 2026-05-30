package de.erethon.factions.region;

import de.erethon.factions.util.FLogger;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.war.RegionalWarTracker;
import io.papermc.paper.math.Position;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A region that participates in war mechanics.
 * Contains the regional war tracker and region structures.
 *
 * @author Fyreum
 */
public class WarRegion extends Region {

    private final RegionalWarTracker regionalWarTracker = new RegionalWarTracker(this);
    private final Map<String, RegionStructure> structures = new HashMap<>();

    protected WarRegion(@NotNull RegionCache regionCache, @NotNull File file, int id, @NotNull String name, @Nullable String description) {
        super(regionCache, file, id, name, description);
    }

    protected WarRegion(@NotNull RegionCache regionCache, @NotNull File file) throws NumberFormatException {
        super(regionCache, file);
    }

    @Override
    public void load() {
        super.load();
        regionalWarTracker.load(config.getConfigurationSection("warTracker"));

        ConfigurationSection structuresSection = config.getConfigurationSection("structures");
        if (structuresSection != null) {
            for (String key : structuresSection.getKeys(false)) {
                ConfigurationSection section = structuresSection.getConfigurationSection(key);
                if (section == null) {
                    FLogger.ERROR.log("Unknown region structure in region '" + getId() + "' found: " + key);
                    continue;
                }
                RegionStructure structure = RegionStructure.deserialize(this, section);
                structures.put(structure.getName(), structure);
            }
            if (!structures.isEmpty()) {
                FLogger.REGION.log("Loaded " + structures.size() + " structures in region '" + getId() + "'");
            }
        }

        if (getType().isWarGround() && plugin.getWar() != null) {
            plugin.getWar().registerRegion(regionalWarTracker);
        }
    }

    @Override
    protected void serializeData() {
        super.serializeData();
        plugin.getDatabaseManager().saveRegionWarTracker(regionalWarTracker);
        config.set("warTracker", sanitizeYamlValue(regionalWarTracker.serialize()));
        Map<String, Object> serializedStructures = new HashMap<>(structures.size());
        structures.forEach((name, structure) -> serializedStructures.put(String.valueOf(serializedStructures.size()), sanitizeYamlValue(structure.serialize())));
        config.set("structures", serializedStructures);
    }

    @SuppressWarnings("unchecked")
    private Object sanitizeYamlValue(Object value) {
        if (value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Location) {
            return value;
        }
        if (value instanceof Alliance alliance) {
            return alliance.getId();
        }
        if (value instanceof Faction faction) {
            return faction.getId();
        }
        if (value instanceof TextColor color) {
            return color.asHexString();
        }
        if (value instanceof BossBar.Color color) {
            return color.name();
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new HashMap<>(map.size());
            map.forEach((key, nestedValue) -> sanitized.put(String.valueOf(key), sanitizeYamlValue(nestedValue)));
            return sanitized;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> sanitized = new ArrayList<>();
            for (Object nestedValue : iterable) {
                sanitized.add(sanitizeYamlValue(nestedValue));
            }
            return sanitized;
        }
        return String.valueOf(value);
    }

    @Override
    public void setType(@NotNull RegionType type) {
        RegionType oldType = getType();
        super.setType(type);
        // Handle war registration changes
        if (oldType.isWarGround() && !type.isWarGround()) {
            plugin.getWar().unregisterRegion(regionalWarTracker);
        } else if (!oldType.isWarGround() && type.isWarGround()) {
            plugin.getWar().registerRegion(regionalWarTracker);
        }
    }

    /* War tracker */

    public @NotNull RegionalWarTracker getRegionalWarTracker() {
        return regionalWarTracker;
    }

    /* Structures */

    public @NotNull Map<String, RegionStructure> getStructures() {
        return structures;
    }

    public <T extends RegionStructure> @NotNull Map<String, T> getStructures(@NotNull Class<T> type) {
        Map<String, T> filtered = new HashMap<>();
        for (RegionStructure structure : structures.values()) {
            if (type.isInstance(structure)) {
                filtered.put(structure.getName(), (T) structure);
            }
        }
        return filtered;
    }

    public @Nullable RegionStructure getStructure(@NotNull String name) {
        return structures.get(name);
    }

    public <T extends RegionStructure> @Nullable T getStructure(@NotNull String name, @NotNull Class<T> type) {
        RegionStructure structure = getStructure(name);
        return type.isInstance(structure) ? (T) structure : null;
    }

    @SuppressWarnings("UnstableApiUsage")
    public @Nullable RegionStructure getStructureAt(@NotNull Position position) {
        for (RegionStructure structure : structures.values()) {
            if (structure.containsPosition(position)) {
                return structure;
            }
        }
        return null;
    }

    @SuppressWarnings("UnstableApiUsage")
    public @NotNull List<RegionStructure> getStructuresAt(@NotNull Position position) {
        List<RegionStructure> found = new ArrayList<>();
        for (RegionStructure structure : structures.values()) {
            if (structure.containsPosition(position)) {
                found.add(structure);
            }
        }
        return found;
    }

    @SuppressWarnings("UnstableApiUsage")
    public <T extends RegionStructure> @NotNull List<T> getStructuresAt(@NotNull Position position, @NotNull Class<T> type) {
        List<T> found = new ArrayList<>();
        for (RegionStructure structure : structures.values()) {
            if (structure.containsPosition(position) && type.isInstance(structure)) {
                found.add((T) structure);
            }
        }
        return found;
    }

    public void addStructure(@NotNull RegionStructure structure) {
        structures.put(structure.getName(), structure);
    }

    public void removeStructure(@NotNull RegionStructure structure) {
        structures.remove(structure.getName());
    }
}

