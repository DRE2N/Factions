package de.erethon.factions.region;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.data.FConfig;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.util.FLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * A region that can be claimed and owned by a faction.
 * Contains owner, claiming price, and build sites.
 *
 * @author Malfrador
 */
public class ClaimableRegion extends Region {

    private final Set<BuildSite> buildSites = new HashSet<>();
    private double lastClaimingPrice;
    private Faction owner;

    protected ClaimableRegion(@NotNull RegionCache regionCache, @NotNull File file, int id, @NotNull String name, @Nullable String description) {
        super(regionCache, file, id, name, description);
    }

    protected ClaimableRegion(@NotNull RegionCache regionCache, @NotNull File file) throws NumberFormatException {
        super(regionCache, file);
    }

    @Override
    public void load() {
        super.load();
        lastClaimingPrice = config.getDouble("lastClaimingPrice", lastClaimingPrice);
        owner = plugin.getFactionCache().getById(config.getInt("owner", -1));
    }

    @Override
    protected void serializeData() {
        super.serializeData();
        config.set("lastClaimingPrice", lastClaimingPrice);
        config.set("owner", owner == null ? null : owner.getId());
        config.set("buildsites", buildSites.stream().map(BuildSite::getUUIDString).toList());
        for (BuildSite buildSite : buildSites) {
            try {
                buildSite.save();
            } catch (IOException e) {
                FLogger.REGION.log("Failed to save build site " + buildSite.getUuid() + " for region " + getId() + ": " + e.getMessage());
            }
        }
    }

    /* Claiming and ownership */

    public double calculatePriceFor(@Nullable Faction faction) {
        FConfig cfg = plugin.getFConfig();
        double chunkIncrement = getChunks().size() * cfg.getRegionPricePerChunk();
        double regionIncrement = faction == null ? 0 : cfg.getRegionPricePerRegionFactor() * Math.sqrt(faction.getRegions().size()) * cfg.getRegionPricePerRegion();
        double price = cfg.getRegionPriceBase() + chunkIncrement + regionIncrement;
        return Math.round(price * cfg.getRegionPriceTotalMultiplier());
    }

    public @NotNull Set<BuildSite> getBuildSites() {
        return buildSites;
    }

    public double getLastClaimingPrice() {
        return lastClaimingPrice;
    }

    public void setLastClaimingPrice(double lastClaimingPrice) {
        this.lastClaimingPrice = lastClaimingPrice;
    }

    public @Nullable Faction getOwner() {
        return owner;
    }

    @Override
    public @Nullable Faction getFaction() {
        return owner;
    }

    public boolean isOwned() {
        return owner != null;
    }

    public @NotNull String getDisplayOwner() {
        return isOwned() ? owner.getName(true) : FMessage.GENERAL_WILDERNESS.getMessage() + (getAlliance() == null ? "" : " (" + getAlliance().getName(true) + ")");
    }

    public void setOwner(@Nullable Faction owner) {
        this.owner = owner;
    }
}

