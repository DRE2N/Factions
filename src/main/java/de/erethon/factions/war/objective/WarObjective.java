package de.erethon.factions.war.objective;

import de.erethon.factions.Factions;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionStructure;
import io.papermc.paper.math.Position;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a functional warzone area, that has influence on the outcome of a battle.
 *
 * @author Fyreum
 */
public abstract class WarObjective extends RegionStructure {

    public static final NamespacedKey NAME_KEY = new NamespacedKey(Factions.get(), "warObjectiveName");

    /* Settings */
    protected boolean capitalObjective;
    protected String name;
    /* Temporary */
    protected Map<FPlayer, Long> activePlayers = new HashMap<>();
    protected Set<FPlayer> activeSpectators = new HashSet<>();

    public WarObjective(@NotNull Region region, @NotNull ConfigurationSection config) {
        super(region, config);
        this.capitalObjective = config.getBoolean("capitalObjective");
        this.name = config.getString("name");
    }

    public WarObjective(@NotNull Region region, @NotNull ConfigurationSection config, @NotNull Position a, @NotNull Position b) {
        super(region, config, a, b);
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

    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put("type", getClass().getName());
        serialized.put("capitalObjective", capitalObjective);
        serialized.put("name", name);
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

}
