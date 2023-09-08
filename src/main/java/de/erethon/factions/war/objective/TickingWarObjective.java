package de.erethon.factions.war.objective;

import de.erethon.aergia.util.TickUtil;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import io.papermc.paper.math.Position;
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

    public TickingWarObjective(@NotNull Region region, @NotNull ConfigurationSection config) {
        super(region, config);
    }

    public TickingWarObjective(@NotNull Region region, @NotNull ConfigurationSection config, @NotNull Position a, @NotNull Position b) {
        super(region, config, a, b);
    }

    @Override
    protected void load(@NotNull ConfigurationSection config) {
        this.tickInterval = config.getLong("tickInterval", TickUtil.SECOND);
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

    /* Sub classes */

    public static abstract class Builder<THIS extends Builder<THIS, TYPE>, TYPE extends WarObjective> extends WarObjectiveBuilder<THIS, TYPE> {

        public @NotNull THIS tickInterval(long tickInterval) {
            data.set("tickInterval", tickInterval);
            return (THIS) this;
        }

    }
}
