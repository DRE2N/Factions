package de.erethon.factions.event;

import de.erethon.factions.faction.Faction;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public abstract class FactionEvent extends Event {

    protected final Faction faction;

    public FactionEvent(@NotNull Faction faction) {
        this.faction = faction;
    }

    public @NotNull Faction getFaction() {
        return faction;
    }
}
