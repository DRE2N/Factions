package de.erethon.factions.war.objective;

import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FBroadcastUtil;
import io.papermc.paper.math.Position;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    protected double energyLossOnDamage;
    protected double energyLossPerInterval;
    protected double maxEnergy;
    /* Temporary */
    protected Alliance alliance;
    protected EnderCrystal crystal;
    protected double energy;
    protected TextDisplay energyDisplay;
    protected Location crystalLocation;

    public CrystalWarObjective(@NotNull Region region, @NotNull ConfigurationSection config) {
        super(region, config);
    }

    public CrystalWarObjective(@NotNull Region region, @NotNull ConfigurationSection config, @NotNull Position a, @NotNull Position b) {
        super(region, config, a, b);
    }

    @Override
    protected void load(@NotNull ConfigurationSection config) {
        this.energyLossOnDamage = config.getDouble("energyLossOnDamage", 10.0);
        this.energyLossPerInterval = config.getDouble("energyLossPerInterval", 1.0);
        this.maxEnergy = config.getDouble("maxEnergy", 600.0);
        this.energy = maxEnergy;

        World world = region.getWorld();
        int x = (getXRange().getMaximumInteger() + getXRange().getMinimumInteger()) / 2,
                y = getYRange().getMaximumInteger(),
                z = (getZRange().getMaximumInteger() + getZRange().getMinimumInteger()) / 2;

        while (world.getType(x, y, z).isEmpty()) {
            if (--y < yRange.getMinimumInteger()) {
                break;
            }
        }
        crystalLocation = new Location(world, x, y + 1, z);
    }

    @Override
    public void tick() {
        removeEnergy(energyLossPerInterval, null);
    }

    public void damage(double damage, @Nullable FPlayer damager) {
        removeEnergy(energyLossOnDamage, damager);
    }

    public void destroy(@Nullable FPlayer damager) {
        if (damager != null) {
            FBroadcastUtil.broadcastWar(FMessage.WAR_OBJECTIVE_DESYTROYED_BY_PLAYER, alliance.getDisplayShortName(), region.getName(), damager.getLastName());
        } else {
            FBroadcastUtil.broadcastWar(FMessage.WAR_OBJECTIVE_DESYTROYED, alliance.getDisplayShortName(), region.getName());
        }
        deactivate();
        deleteStructure();
        crystalLocation.createExplosion(4f, false, false);
    }

    /* Setup */

    @Override
    public void activate() {
        super.activate();
        World world = crystalLocation.getWorld();
        crystal = world.spawn(crystalLocation, EnderCrystal.class, c -> c.getPersistentDataContainer().set(NAME_KEY, PersistentDataType.STRING, name));
        crystal.addPassenger(energyDisplay = world.spawn(crystalLocation, TextDisplay.class, this::displayEnergy));
    }

    @Override
    public void deactivate() {
        super.deactivate();
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
        displayPercentage(display, maxEnergy, energy);
    }

    private void displayPercentage(TextDisplay display, double max, double current) {
        int colored = (int) (20 * (1 / max * current));
        display.text(Component.text().color(NamedTextColor.BLUE).content("█".repeat(colored))
                .append(Component.text().color(NamedTextColor.GRAY).content("█".repeat(20 - colored))).build());
    }

    /* Serialization */

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> serialized = super.serialize();
        serialized.put("energyLossOnDamage", energyLossOnDamage);
        serialized.put("energyLossPerInterval", energyLossPerInterval);
        serialized.put("maxEnergy", maxEnergy);
        return serialized;
    }

    /* Getters and setters */

    public double getEnergyLossOnDamage() {
        return energyLossOnDamage;
    }

    public void setEnergyLossOnDamage(double energyLossOnDamage) {
        this.energyLossOnDamage = energyLossOnDamage;
    }

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
        assert energy > 0 && energy <= maxEnergy : "The energy number must be greater than 0 and less or equal to " + maxEnergy;
        this.energy = energy;
        displayEnergy();
    }

    public void addEnergy(double amount) {
        this.energy = Math.min(energy + amount, maxEnergy);
        displayEnergy();
    }

    public void removeEnergy(double amount, @Nullable FPlayer causingPlayer) {
        energy -= amount;
        displayEnergy();
        if (energy <= 0) {
            destroy(causingPlayer);
        }
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
