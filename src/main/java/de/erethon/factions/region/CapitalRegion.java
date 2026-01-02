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

    protected CapitalRegion(@NotNull RegionCache regionCache, @NotNull File file, int id, @NotNull String name, @Nullable String description) {
        super(regionCache, file, id, name, description);
    }

    protected CapitalRegion(@NotNull RegionCache regionCache, @NotNull File file) throws NumberFormatException {
        super(regionCache, file);
    }

    @Override
    public void load() {
        super.load();
    }

    @Override
    protected void serializeData() {
        super.serializeData();
    }
}

