package de.erethon.factions.portal;

import de.erethon.aergia.util.TeleportUtil;
import de.erethon.bedrock.config.ConfigUtil;
import de.erethon.bedrock.config.EConfig;
import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.util.FLogger;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Fyreum
 */
public class Portal extends EConfig {

    public static final int CONFIG_VERSION = 1;

    private final int id;
    private final Map<Alliance, Location> locations = new HashMap<>();
    private final Set<PortalCondition> conditions = new HashSet<>();

    public Portal(@NotNull File file, int id) {
        super(file, CONFIG_VERSION);
        this.id = id;
        load();
    }

    public void teleport(@NotNull FPlayer fPlayer) {
        Alliance alliance = fPlayer.getAlliance();
        if (alliance == null) {
            fPlayer.sendMessage(FMessage.ERROR_PLAYER_IS_NOT_IN_AN_ALLIANCE.message());
            return;
        }
        Location location = locations.get(alliance);
        if (location == null) {
            fPlayer.sendMessage(FMessage.ERROR_PORTAL_ALLIANCE_NOT_SETUP.message());
            return;
        }
        for (PortalCondition condition : conditions) {
            if (!condition.check(fPlayer)) {
                return;
            }
        }
        TeleportUtil.teleport(fPlayer, fPlayer.getEPlayer(), location);
    }

    /* Serialization */

    @Override
    public void load() {
        ConfigurationSection locationsSection = config.getConfigurationSection("locations");

        if (locationsSection != null) {
            for (String key : locationsSection.getKeys(false)) {
                int id;
                try {
                    id = Integer.parseInt(key);
                } catch (NumberFormatException e) {
                    FLogger.WARN.log("Invalid alliance id in '" + file.getName() + "' found: " + key);
                    continue;
                }
                Alliance alliance = Factions.get().getAllianceCache().getById(id);

                if (alliance == null) {
                    FLogger.WARN.log("Unknown alliance in '" + file.getName() + "' found: " + key);
                    continue;
                }
                locations.put(alliance, ConfigUtil.getLocation(locationsSection, key));
            }
        }
        List<String> conditionsList = config.getStringList("conditions");

        for (String key : conditionsList) {
            PortalCondition condition = PortalCondition.getByName(key);

            if (condition == null) {
                FLogger.WARN.log("Unknown portal condition in '" + file.getName() + "' found: " + key);
                continue;
            }
            conditions.add(condition);
        }
    }

    public void saveData() {
        locations.forEach((alliance, location) -> config.set("locations." + alliance.getId(), location));
        config.set("conditions", conditions.stream().map(PortalCondition::getName).toList());
        save();
    }

    /* Getters and setters */

    public int getId() {
        return id;
    }

    public @NotNull Map<Alliance, Location> getLocations() {
        return locations;
    }

    public @Nullable Location getLocation(@NotNull Alliance alliance) {
        return locations.get(alliance);
    }

    public void setLocation(@NotNull Alliance alliance, @Nullable Location location) {
        if (location == null) {
            locations.remove(alliance);
        } else {
            locations.put(alliance, location);
        }
    }

    public @NotNull Set<PortalCondition> getConditions() {
        return conditions;
    }

    public void addCondition(@NotNull PortalCondition condition) {
        conditions.add(condition);
    }

    public void removeCondition(@NotNull PortalCondition condition) {
        conditions.remove(condition);
    }

}
