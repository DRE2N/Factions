package de.erethon.factions.alliance;

import de.erethon.factions.entity.FLegalEntity;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FLogger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Base of every faction and region.
 *
 * @author Fyreum
 */
public class Alliance extends FLegalEntity {

    private final Set<Region> coreRegions = new HashSet<>();
    private final Set<Faction> factions = new HashSet<>();
    private String shortName;

    protected Alliance(@NotNull File file, int id, @NotNull String name, @Nullable String description) {
        super(file, id, name, description);
    }

    protected Alliance(@NotNull File file) throws NumberFormatException {
        super(file);
    }

    @Override
    public void load() {
        for (int regionId : config.getIntegerList("coreRegions")) {
            Region region = plugin.getRegionManager().getRegionById(regionId);
            if (region == null) {
                FLogger.ERROR.log("Unknown region ID in alliance '" + id + "' found: " + regionId);
                continue;
            }
            this.coreRegions.add(region);
        }
        for (int factionId : config.getIntegerList("factions")) {
            Faction faction = plugin.getFactionCache().getById(factionId);
            if (faction == null) {
                FLogger.ERROR.log("Unknown faction ID in alliance '" + id + "' found: " + factionId);
                continue;
            }
            this.factions.add(faction);
        }
        this.shortName = config.getString("shortName");
    }

    @Override
    protected void serializeData() {
        saveEntities("coreRegions", coreRegions);
        saveEntities("factions", factions);
        config.set("shortName", shortName);
    }

    @Override
    public @Nullable Alliance getAlliance() {
        return this;
    }

    /**
     * @return always null
     * @deprecated there is no point in using this method, as it does not provide anything
     */
    @Override
    @Deprecated
    @Contract("-> null")
    public @Nullable Faction getFaction() {
        return null;
    }

    /* Getters and setters */

    public @Nullable String getShortName() {
        return shortName;
    }

    public @NotNull String getDisplayShortName() {
        return shortName == null || shortName.isEmpty() ? name : shortName;
    }

    public void setShortName(@Nullable String shortName) {
        this.shortName = shortName;
    }

    @Override
    public boolean matchingName(@NotNull String name) {
        return super.matchingName(name) || this.shortName.equalsIgnoreCase(name);
    }

    public @NotNull Set<Region> getCoreRegions() {
        return coreRegions;
    }

    public @NotNull Set<Faction> getFactions() {
        return factions;
    }

    public void addFaction(@NotNull Faction faction) {
        factions.add(faction);
    }

    public void removeFaction(@NotNull Faction faction) {
        factions.remove(faction);
    }
}
