package de.erethon.factions.region;

import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.util.WarMath;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * A region with PvE level bounds for mob scaling and similar mechanics.
 *
 * @author Malfrador
 */
public class PvERegion extends Region {

    private static final double MAX_INFLUENCE = 100.0;
    private static final double MIN_INFLUENCE = 0.0;
    private static final double DEFAULT_INFLUENCE = 33.33;

    private int lowerLevelBound = -1;
    private int upperLevelBound = -1;

    private final Map<Alliance, Double> allianceInfluence = new HashMap<>();

    protected PvERegion(@NotNull RegionCache regionCache, @NotNull File file, int id, @NotNull String name, @Nullable String description) {
        super(regionCache, file, id, name, description);
    }

    protected PvERegion(@NotNull RegionCache regionCache, @NotNull File file) throws NumberFormatException {
        super(regionCache, file);
    }

    @Override
    public void load() {
        super.load();
        lowerLevelBound = config.getInt("lowerLevelBound", lowerLevelBound);
        upperLevelBound = config.getInt("upperLevelBound", upperLevelBound);
        if (config.isConfigurationSection("alliances")) {
            for (String key : config.getConfigurationSection("alliances").getKeys(false)) {
                int id;
                try {
                    id = Integer.parseInt(key);
                } catch (NumberFormatException e) {
                    continue;
                }
                Alliance alliance = plugin.getAllianceCache().getById(id);
                if (alliance != null) {
                    double influence = config.getDouble("alliances." + key, 0.0);
                    allianceInfluence.put(alliance, influence);
                }
            }
        } else {
            // Initialize default influence for all alliances
            for (Alliance alliance : plugin.getAllianceCache().getCache().values()) {
                allianceInfluence.put(alliance, DEFAULT_INFLUENCE);
            }
        }
    }

    @Override
    protected void serializeData() {
        super.serializeData();
        config.set("lowerLevelBound", lowerLevelBound);
        config.set("upperLevelBound", upperLevelBound);
        for (Alliance alliance : allianceInfluence.keySet()) {
            config.set("alliances." + alliance.getId(), allianceInfluence.get(alliance));
        }
    }

    /* Level bounds */

    public void setMinMaxLevel(int lower, int upper) {
        this.lowerLevelBound = lower;
        this.upperLevelBound = upper;
    }

    public int getLowerLevelBound() {
        return lowerLevelBound;
    }

    public int getUpperLevelBound() {
        return upperLevelBound;
    }

    /* Alliance influence */

    public void addInfluence(Alliance alliance, double amount) {
        allianceInfluence.put(alliance, Math.min(MAX_INFLUENCE, getInfluence(alliance) + amount));
    }

    public double getInfluence(Alliance alliance) {
        return allianceInfluence.getOrDefault(alliance, 0.0);
    }

    public void removeInfluence(Alliance alliance, double amount) {
        allianceInfluence.put(alliance, Math.max(MIN_INFLUENCE, getInfluence(alliance) - amount));
    }

    public Map<Alliance, Double> getAllAllianceInfluences() {
        return allianceInfluence;
    }

    public Component getFormattedInfluence() {
        Component component = WarMath.getAllianceInfluenceBar(allianceInfluence);
        Component hover = Component.text("");
        for (Alliance alliance : allianceInfluence.keySet()) {
            hover = hover.append(Component.newline())
                    .append(alliance.getColoredName()
                    .append(Component.text(": " + String.format("%.2f", getInfluence(alliance)) + "%")));
        }
        component = component.hoverEvent(hover);
        return Component.translatable("factions.cmd.region.info.influence").append(Component.text(" ")).append(component);
    }

}

