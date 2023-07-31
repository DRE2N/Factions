package de.erethon.factions.war.objective;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FBroadcastUtil;
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

    public CrystalWarObjective(@NotNull ConfigurationSection config) {
        super(config);
    }

    @Override
    public void tick() {
        if (energy <= 0) {
            return;
        }
        setEnergy(Math.max(energy - energyLossPerInterval, 0));
        Region region = plugin.getRegionManager().getRegionByLocation(location);
        if (region == null) {
            return;
        }
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
        Region region = plugin.getRegionManager().getRegionByLocation(location);
        String regionName = region != null ? region.getName() : FMessage.GENERAL_WILDERNESS.getMessage();
        if (damager != null) {
            FBroadcastUtil.broadcastWar(FMessage.WAR_OBJECTIVE_DESYTROYED_BY_PLAYER, alliance.getDisplayShortName(), regionName, damager.getLastName());
        } else {
            FBroadcastUtil.broadcastWar(FMessage.WAR_OBJECTIVE_DESYTROYED, alliance.getDisplayShortName(), regionName);
        }
        deactivate();
        location.createExplosion(4f, false, false);
    }

    /* Setup */

    @Override
    public void activate() {
        super.activate();
        crystal = location.getWorld().spawn(location, EnderCrystal.class, c -> c.getPersistentDataContainer().set(NAME_KEY, PersistentDataType.STRING, name));
        crystal.addPassenger(energyDisplay = location.getWorld().spawn(location, TextDisplay.class, this::displayEnergy));
        energyDisplay.addPassenger(healthDisplay = location.getWorld().spawn(location, TextDisplay.class, this::displayHealth));
    }

    @Override
    public void deactivate() {
        super.deactivate();
        healthDisplay.remove();
        energyDisplay.remove();
        crystal.remove();
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
    public void load() {
        super.load();
        energyLossPerInterval = config.getDouble("energyLossPerInterval", 5.0);
        maxEnergy = config.getDouble("maxEnergy", 100.0);
        maxHealth = config.getDouble("maxHealth", 2000.0);
        scorePerInterval = config.getDouble("scorePerInterval", 1.0);
        energy = maxEnergy;
        health = maxHealth;
    }

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

    public @NotNull  Alliance getAlliance() {
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
