package de.erethon.factions.war.objective;

import de.erethon.aergia.util.TickUtil;
import de.erethon.factions.player.FPlayer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author Fyreum
 */
public abstract class TickingWarObjective extends WarObjective {

    protected long tickInterval;
    protected BukkitTask task;

    public TickingWarObjective(@NotNull ConfigurationSection config) {
        super(config);
    }

    @Override
    public void load() {
        super.load();
        tickInterval = config.getLong("tickInterval", TickUtil.SECOND);
    }

    @Override
    public void onEnter(@NotNull FPlayer fPlayer) {
        super.onEnter(fPlayer);
        if (task != null) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::tick, tickInterval, tickInterval);
    }

    public abstract void tick();

    /* Serialization */

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> serialized = super.serialize();
        serialized.put("tickInterval", tickInterval);
        return serialized;
    }

    /* Getters and setters */

    public long getTickInterval() {
        return tickInterval;
    }

    public void setTickInterval(long tickInterval) {
        this.tickInterval = tickInterval;
    }

    public boolean isTicking() {
        return task != null;
    }
}
