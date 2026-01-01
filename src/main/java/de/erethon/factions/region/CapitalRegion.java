package de.erethon.factions.region;

import de.erethon.factions.faction.Faction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * = Theanor
 *
 * @author Malfrador
 */
public class CapitalRegion extends WarRegion {

    private Faction owner;

    protected CapitalRegion(@NotNull RegionCache regionCache, @NotNull File file, int id, @NotNull String name, @Nullable String description) {
        super(regionCache, file, id, name, description);
    }

    protected CapitalRegion(@NotNull RegionCache regionCache, @NotNull File file) throws NumberFormatException {
        super(regionCache, file);
    }

    @Override
    public void load() {
        super.load();
        owner = plugin.getFactionCache().getById(config.getInt("owner", -1));
    }

    @Override
    protected void serializeData() {
        super.serializeData();
        config.set("owner", owner == null ? null : owner.getId());
    }

    @Override
    public @Nullable Faction getOwner() {
        return owner;
    }

    @Override
    public @Nullable Faction getFaction() {
        return owner;
    }

    @Override
    public boolean isOwned() {
        return owner != null;
    }

    public void setOwner(@Nullable Faction owner) {
        this.owner = owner;
    }
}

