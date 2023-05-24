package de.erethon.factions.war.objective;

import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.WarBroadcastUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author Fyreum
 */
public class CrystalWarObjective extends TickingWarObjective {

    /* Settings */
    protected double damagePerTick;
    protected double maxHealth;
    /* Temporary */
    protected Alliance alliance;
    protected EnderCrystal crystal;
    protected double health;

    public CrystalWarObjective(@NotNull ConfigurationSection config) {
        super(config);
    }

    @Override
    public void tick() {
        damage(damagePerTick, null);
    }

    public void damage(double damage, @Nullable FPlayer damager) {
        health -= damage;
        if (health > 0) {
            return;
        }
        health = 0;
        destroy(damager);
    }

    public void destroy(@Nullable FPlayer damager) {
        Region region = plugin.getRegionManager().getRegionByLocation(location);
        String regionName = region != null ? region.getName() : FMessage.GENERAL_WILDERNESS.getMessage();
        if (damager != null) {
            WarBroadcastUtil.broadcast(FMessage.WAR_OBJECTIVE_DESYTROYED_BY_PLAYER, alliance.getDisplayShortName(), regionName, damager.getLastName());
        } else {
            WarBroadcastUtil.broadcast(FMessage.WAR_OBJECTIVE_DESYTROYED, alliance.getDisplayShortName(), regionName);
        }
        deactivate();
        location.createExplosion(4f, false, false);
    }

    /* Setup */

    @Override
    public void activate() {
        super.activate();
        crystal = location.getWorld().spawn(location, EnderCrystal.class, c -> c.getPersistentDataContainer().set(NAME_KEY, PersistentDataType.STRING, name));
    }

    @Override
    public void deactivate() {
        super.deactivate();
        crystal.remove();
    }

    /* Serialization */

    @Override
    public void load() {
        super.load();
        damagePerTick = config.getDouble("damagePerTick", 1);
        maxHealth = config.getDouble("maxHealth", 2000.0);
        health = maxHealth;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> serialized = super.serialize();
        serialized.put("maxHealth", maxHealth);
        return serialized;
    }

    /* Getters and setters */

    public double getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
        if (maxHealth < health) {
            health = maxHealth;
        }
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        assert health > 0 && health < maxHealth : "The health number must lie between 0 and " + maxHealth;
        this.health = health;
    }

    public @NotNull  Alliance getAlliance() {
        return alliance;
    }

    public @NotNull CrystalWarObjective setAlliance(@NotNull Alliance alliance) {
        this.alliance = alliance;
        return this;
    }

}
