package de.erethon.factions.war.objective;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FBroadcastUtil;
import io.papermc.paper.math.Position;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author Fyreum
 */
public class CrystalWarObjective extends TickingWarObjective {

    /* Settings */
    protected double energyLossPerInterval;
    protected double maxEnergy;
    protected double maxHealth;
    protected double scorePerInterval;
    /* Temporary */
    protected Alliance alliance;
    protected EnderCrystal crystal;
    protected double energy;
    protected double health;
    protected TextDisplay energyDisplay, healthDisplay;
    protected Location spawnLocation;

    public CrystalWarObjective(@NotNull Region region, @NotNull ConfigurationSection config) {
        super(region, config);
    }

    public CrystalWarObjective(@NotNull Region region, @NotNull ConfigurationSection config, @NotNull Position a, @NotNull Position b) {
        super(region, config, a, b);
    }

    @Override
    protected void load(@NotNull ConfigurationSection config) {
        this.energyLossPerInterval = config.getDouble("energyLossPerInterval", 5.0);
        this.maxEnergy = config.getDouble("maxEnergy", 100.0);
        this.maxHealth = config.getDouble("maxHealth", 2000.0);
        this.scorePerInterval = config.getDouble("scorePerInterval", 1.0);
        this.energy = maxEnergy;
        this.health = maxHealth;
        this.spawnLocation = getCenterPosition().toLocation(region.getWorld());
        this.spawnLocation.setY(yRange.getMinimumInteger());
    }

    @Override
    public void tick() {
        if (energy <= 0) {
            return;
        }
        setEnergy(Math.max(energy - energyLossPerInterval, 0));
        region.getRegionalWarTracker().addScore(alliance, scorePerInterval);
    }

    public void damage(double damage, @Nullable FPlayer damager) {
        health = Math.max(health - damage, 0);
        displayHealth();
        if (health > 0) {
            return;
        }
        destroy(damager);
    }

    public void destroy(@Nullable FPlayer damager) {
        if (damager != null) {
            FBroadcastUtil.broadcastWar(FMessage.WAR_OBJECTIVE_DESYTROYED_BY_PLAYER, alliance.getDisplayShortName(), region.getName(), damager.getLastName());
        } else {
            FBroadcastUtil.broadcastWar(FMessage.WAR_OBJECTIVE_DESYTROYED, alliance.getDisplayShortName(), region.getName());
        }
        deactivate();
        // todo: Delete objective after determination
        spawnLocation.createExplosion(4f, false, false);
    }

    /* Setup */

    @Override
    public void activate() {
        super.activate();
        World world = spawnLocation.getWorld();
        crystal = world.spawn(spawnLocation, EnderCrystal.class, c -> c.getPersistentDataContainer().set(NAME_KEY, PersistentDataType.STRING, name));
        crystal.addPassenger(energyDisplay = world.spawn(spawnLocation, TextDisplay.class, this::displayEnergy));
        energyDisplay.addPassenger(healthDisplay = world.spawn(spawnLocation, TextDisplay.class, this::displayHealth));
    }

    @Override
    public void deactivate() {
        super.deactivate();
        healthDisplay.remove();
        energyDisplay.remove();
        crystal.remove();
    }

    @Override
    public void onTemporaryOccupy(@NotNull Alliance alliance) {
        deactivate();
    }

    private void displayEnergy() {
        displayEnergy(energyDisplay);
    }

    private void displayEnergy(TextDisplay display) {
        displayPercentage(display, maxEnergy, energy, "blue");
    }

    private void displayHealth() {
        displayHealth(healthDisplay);
    }

    private void displayHealth(TextDisplay display) {
        displayPercentage(display, maxHealth, health, "red");
    }

    private void displayPercentage(TextDisplay display, double max, double current, String color) {
        int red = (int) (20 * (1 / max * current));
        display.text(MessageUtil.parse("<" + color + ">" + "█".repeat(red) + "<gray>" + "█".repeat(20 - red)));
    }

    /* Serialization */

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> serialized = super.serialize();
        serialized.put("energyLossPerInterval", energyLossPerInterval);
        serialized.put("maxEnergy", maxEnergy);
        serialized.put("maxHealth", maxHealth);
        serialized.put("scorePerInterval", scorePerInterval);
        return serialized;
    }

    /* Getters and setters */

    public double getEnergyLossPerInterval() {
        return energyLossPerInterval;
    }

    public void setEnergyLossPerInterval(double energyLossPerInterval) {
        this.energyLossPerInterval = energyLossPerInterval;
    }

    public double getMaxEnergy() {
        return maxEnergy;
    }

    public void setMaxEnergy(double maxEnergy) {
        this.maxEnergy = maxEnergy;
        if (maxEnergy < energy) {
            setEnergy(maxEnergy);
        }
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        assert energy > 0 && energy < maxEnergy : "The energy number must lie between 0 and " + energy;
        this.energy = energy;
        displayEnergy();
    }

    public void addEnergy(double amount) {
        this.energy = Math.max(energy + amount, maxEnergy);
        displayEnergy();
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
        if (maxHealth < health) {
            setHealth(maxHealth);
        }
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        assert health > 0 && health < maxHealth : "The health number must lie between 0 and " + maxHealth;
        this.health = health;
        displayHealth();
    }

    public @NotNull Alliance getAlliance() {
        return alliance;
    }

    public @NotNull CrystalWarObjective setAlliance(@NotNull Alliance alliance) {
        this.alliance = alliance;
        return this;
    }

    public @Nullable EnderCrystal getCrystal() {
        return crystal;
    }
}
