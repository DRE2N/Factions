package de.erethon.factions.region;

import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FConfig;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.entity.FLegalEntity;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.util.FactionUtil;
import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * @author Fyreum
 */
public class Region extends FLegalEntity {

    private final RegionCache regionCache;
    /* Data */
    private final Set<Region> adjacentRegions = new HashSet<>();
    private Alliance alliance;
    private final Set<LazyChunk> chunks = new HashSet<>();
    private boolean claimable = true;
    private double damageReduction = 0.0;
    private Faction owner;
    private RegionType type = RegionType.BARREN;

    protected Region(@NotNull RegionCache regionCache, @NotNull File file, int id, @NotNull String name, @Nullable String description) {
        super(file, id, name, description);
        this.regionCache = regionCache;
        saveData();
    }

    protected Region(@NotNull RegionCache regionCache, @NotNull File file) throws NumberFormatException {
        super(file);
        this.regionCache = regionCache;
    }

    /**
     * Removes the region everywhere and tries to delete the region file.
     * See {@link File#delete()} for possible Exceptions.
     *
     * @return true if the file is successfully deleted, false otherwise
     */
    public boolean delete() {
        FLogger.REGION.log("Deleting region '" + id + "'...");
        if (owner != null) {
            owner.removeRegion(this);
        }
        regionCache.removeRegion(this);
        return file.delete();
    }

    public double calculatePriceFor(@Nullable Faction faction) {
        FConfig cfg = plugin.getFConfig();
        double chunkIncrement = chunks.size() * cfg.getRegionPricePerChunk();
        double regionIncrement = faction == null ? 0 : cfg.getRegionPricePerRegionFactor() * Math.sqrt(faction.getRegions().size()) * cfg.getRegionPricePerRegion();
        double price = cfg.getRegionPriceBase() + chunkIncrement + regionIncrement;
        return Math.round(price * cfg.getRegionPriceTotalMultiplier());
    }

    /* Serialization */

    @Override
    public void load() {
        FLogger.REGION.log("Loading region '" + id + "'...");
        this.alliance = plugin.getAllianceCache().getById(config.getInt("alliance", -1));
        for (String string : config.getStringList("chunks")) {
            try {
                addChunk(new LazyChunk(string));
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                FLogger.ERROR.log("Illegal chunk in region '" + file.getName() + "' found: '" + string + "'");
            }
        }
        claimable = config.getBoolean("claimable", claimable);
        damageReduction = config.getDouble("damageReduction", damageReduction);
        owner = plugin.getFactionCache().getById(config.getInt("owner", -1));
        type = RegionType.getByName(config.getString("type", type.name()), type);
    }

    @Override
    protected void serializeData() {
        for (int regionId : config.getIntegerList("adjacentRegions")) {
            Region region = plugin.getRegionManager().getRegionById(regionId);
            if (region == null) {
                FLogger.ERROR.log("Unknown region ID in region '" + id + "' found: " + regionId);
                continue;
            }
            this.adjacentRegions.add(region);
        }
        config.set("alliance", alliance == null ? null : alliance.getId());
        config.set("chunks", chunks.stream().map(LazyChunk::toString).toList());
        config.set("claimable", claimable);
        config.set("damageReduction", damageReduction);
        config.set("owner", owner == null ? null : owner.getId());
        config.set("type", type.name());
    }

    /* Getters and setters */

    public @NotNull RegionCache getRegionCache() {
        return regionCache;
    }

    public @NotNull UUID getWorldId() {
        return regionCache.getWorldId();
    }

    public @NotNull Set<Region> getAdjacentRegions() {
        return adjacentRegions;
    }

    public boolean addAdjacentRegion(@NotNull Region other) {
        assert other != this : "Region cannot be adjacent to itself";
        if (!adjacentRegions.add(other) || other.adjacentRegions.add(this)) {
            return false;
        }
        // Mark owning factions as adjacent.
        if (owner != null && other.getOwner() != null) {
            owner.addAdjacentFaction(other.getOwner());
        }
        return true;
    }

    public boolean removeAdjacentRegion(@NotNull Region other) {
        if (!(adjacentRegions.remove(other) || other.adjacentRegions.remove(this))) {
            return false;
        }
        // Check whether another region ensures the adjacency and remove adjacent faction if not.
        if (owner != null && other.getOwner() != null && !FactionUtil.isAdjacent(owner, other.getOwner())) {
            owner.removeAdjacentFaction(other.getOwner());
        }
        return true;
    }

    public boolean isAdjacentRegion(@NotNull Region other) {
        assert other != this : "Region cannot be adjacent to itself";
        return adjacentRegions.contains(other);
    }

    @Override
    public @Nullable Alliance getAlliance() {
        return alliance;
    }

    public void setAlliance(@Nullable Alliance alliance) {
        this.alliance = alliance;
    }

    public @NotNull Set<LazyChunk> getChunks() {
        return chunks;
    }

    public boolean addChunk(@NotNull Chunk chunk) {
        return addChunk(new LazyChunk(chunk));
    }

    public boolean addChunk(@NotNull LazyChunk lazyChunk) {
        if (chunks.add(lazyChunk)) {
            regionCache.cacheChunkForRegion(lazyChunk, this);
            return true;
        }
        return false;
    }

    public boolean removeChunk(@NotNull Chunk chunk) {
        return removeChunk(new LazyChunk(chunk));
    }

    public boolean removeChunk(@NotNull LazyChunk lazyChunk) {
        if (chunks.remove(lazyChunk)) {
            regionCache.removeChunkForRegion(lazyChunk);
            return true;
        }
        return false;
    }

    public boolean isClaimable() {
        return claimable;
    }

    public void setClaimable(boolean claimable) {
        this.claimable = claimable;
    }

    public double getDamageReduction() {
        return damageReduction;
    }

    public void setDamageReduction(double damageReduction) {
        this.damageReduction = damageReduction;
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
        return isOwned() ? owner.getName() : FMessage.GENERAL_WILDERNESS.getMessage() + (alliance == null ? "" : " (" + alliance.getName() + ")");
    }

    public void setOwner(@Nullable Faction owner) {
        this.owner = owner;
    }

    public @NotNull RegionType getType() {
        return type;
    }

    public void setType(@NotNull RegionType type) {
        this.type = type;
    }

    /* Object methods */

    @Override
    public int hashCode() {
        return Objects.hash(getWorldId(), id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof Region other)) return false;
        return id == other.getId();
    }

    @Override
    public String toString() {
        return "Region[world=" + getWorldId() + ",id=" + id + "]";
    }
}
