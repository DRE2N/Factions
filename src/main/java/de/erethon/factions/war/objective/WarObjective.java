package de.erethon.factions.war.objective;

import de.erethon.factions.Factions;
import de.erethon.factions.player.FPlayer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a functional warzone area, that has influence on the outcome of a battle.
 *
 * @author Fyreum
 */
public abstract class WarObjective {

    public static final NamespacedKey NAME_KEY = new NamespacedKey(Factions.get(), "warObjectiveName");

    protected final Factions plugin = Factions.get();
    protected ConfigurationSection config;
    /* Settings */
    protected boolean capitalObjective;
    protected String name;
    protected Location location;
    protected double radius;
    /* Temporary */
    protected Map<FPlayer, Long> activePlayers = new HashMap<>();
    protected Set<FPlayer> activeSpectators = new HashSet<>();

    public WarObjective(@NotNull ConfigurationSection config) {
        this.config = config;
    }

    /**
     * Activates the war objective and setups all functionalities.
     * Called once after the objective was loaded.
     */
    public void activate() {

    }

    /**
     * Deactivates the war objective and removes all functionalities.
     * Called after an active war phase ends.
     */
    public void deactivate() {

    }

    public void onEnter(@NotNull FPlayer fPlayer) {
        activePlayers.put(fPlayer, System.currentTimeMillis());
        fPlayer.getActiveWarObjectives().add(this);
    }

    public void onExit(@NotNull FPlayer fPlayer) {
        long enterTime = activePlayers.remove(fPlayer);
        fPlayer.getActiveWarObjectives().remove(this);
        fPlayer.getWarStats().incrementCapturingTime(System.currentTimeMillis() - enterTime);
    }

    public void onSpectatorEnter(@NotNull FPlayer fPlayer) {
        activeSpectators.add(fPlayer);
    }

    public void onSpectatorExit(@NotNull FPlayer fPlayer) {
        activeSpectators.remove(fPlayer);
    }

    /* Serialization */

    public void load() {
        capitalObjective = config.getBoolean("capitalObjective");
        name = config.getString("name");
        location = Location.deserialize(config.getConfigurationSection("location").getValues(false));
        radius = config.getDouble("radius", 5.0);
    }

    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put("type", getClass().getName());
        serialized.put("capitalObjective", capitalObjective);
        serialized.put("name", name);
        serialized.put("location", location.serialize());
        serialized.put("radius", radius);
        return serialized;
    }

    /* Getters and setters */

    public boolean isCapitalObjective() {
        return capitalObjective;
    }

    public void setCapitalObjective(boolean capitalObjective) {
        this.capitalObjective = capitalObjective;
    }

    public @NotNull String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public @NotNull Location getLocation() {
        return location;
    }

    public void setLocation(@NotNull Location location) {
        this.location = location;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public @NotNull Map<FPlayer, Long> getActivePlayers() {
        return activePlayers;
    }

    public boolean isActive(@NotNull FPlayer fPlayer) {
        return activePlayers.containsKey(fPlayer);
    }

    public @NotNull Set<FPlayer> getActiveSpectators() {
        return activeSpectators;
    }

    public boolean isSpectator(@NotNull FPlayer fPlayer) {
        return activeSpectators.contains(fPlayer);
    }

    /* Statics */

    /**
     * @throws IllegalArgumentException If the config was erroneous
     */
    public static @NotNull WarObjective deserialize(@NotNull ConfigurationSection config) {
        String typeName = config.getString("type");
        Class<?> type;
        try {
            type = Class.forName(typeName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Illegal war objective type for '" + config.getName() + "' found: " + typeName);
        }
        Constructor<?> constructor;
        try {
            constructor = type.getDeclaredConstructor(ConfigurationSection.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("No matching constructor for war objective type '" + typeName + "' found: " + ConfigurationSection.class.getName());
        }
        Object object;
        try {
            object = constructor.newInstance(config);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Couldn't instantiate war objective '" + config.getName() + "'", e);
        }
        if (!(object instanceof WarObjective objective)) {
            throw new IllegalArgumentException("Illegal war objective type '" + typeName + "': Not a war objective");
        }
        objective.load();
        return objective;
    }
}
