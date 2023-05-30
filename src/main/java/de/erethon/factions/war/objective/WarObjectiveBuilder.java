package de.erethon.factions.war.objective;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public abstract class WarObjectiveBuilder<THIS extends WarObjectiveBuilder<THIS, TYPE>, TYPE extends WarObjective> {

    protected final YamlConfiguration data = new YamlConfiguration();

    public @NotNull THIS name(@NotNull String name) {
        data.set("name", name);
        return (THIS) this;
    }

    public @NotNull THIS location(@NotNull Location location) {
        data.set("location", location);
        return (THIS) this;
    }

    public @NotNull THIS radius(double radius) {
        data.set("radius", radius);
        return (THIS) this;
    }

    public abstract @NotNull TYPE build();

}
