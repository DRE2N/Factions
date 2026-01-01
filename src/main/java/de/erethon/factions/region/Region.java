package de.erethon.factions.region;

import com.google.gson.JsonObject;
import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.attributes.FactionAttribute;
import de.erethon.factions.building.attributes.FactionAttributeModifier;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.entity.FEntity;
import de.erethon.factions.entity.FLegalEntity;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.policy.FPolicy;
import de.erethon.factions.util.FLogger;
import io.papermc.paper.math.Position;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Base region class containing core region functionality.
 * Subclasses: {@link PvERegion}, {@link ClaimableRegion}, {@link WarRegion}, {@link CapitalRegion}
 *
 * @author Malfrador
 */
public class Region extends FLegalEntity {

    protected final Factions plugin = Factions.get();

    protected final RegionCache regionCache;
    /* Data */
    private final Set<Region> adjacentRegions = new HashSet<>();
    private Alliance alliance;
    private final Set<LazyChunk> chunks = new HashSet<>();
    private double damageReduction = 0.0;
    private RegionType type = RegionType.BARREN;
    private RegionMode mode = type.getDefaultMode();
    private Map<RegionPOIType, Set<RegionPOIContainer>> poiMap = new HashMap<>();

    protected Region(@NotNull RegionCache regionCache, @NotNull File file, int id, @NotNull String name, @Nullable String description) {
        super(file, id, name, description);
        this.regionCache = regionCache;
        saveData();
    }

    protected Region(@NotNull RegionCache regionCache, @NotNull File file) throws NumberFormatException {
        super(file);
        this.regionCache = regionCache;
        if (name == null) {
            this.name = FMessage.GENERAL_REGION_DEFAULT_NAME_PREFIX.getMessage(String.valueOf(id));
        }
    }

    /**
     * Removes the region everywhere and tries to delete the region file.
     * See {@link File#delete()} for possible Exceptions.
     *
     * @return true if the file is successfully deleted, false otherwise
     */
    public boolean delete() {
        FLogger.REGION.log("Deleting region '" + id + "'...");
        Faction owner = getOwner();
        if (owner != null) {
            owner.removeRegion(this);
        }
        regionCache.removeRegion(this);
        return file.delete();
    }

    /* Serialization */

    @Override
    public void load() {
        FLogger.REGION.log("Loading region '" + id + "'...");
        for (int regionId : config.getIntegerList("adjacentRegions")) {
            Region region = plugin.getRegionManager().getRegionById(regionId);
            if (region == null) {
                FLogger.ERROR.log("Unknown region ID in region '" + id + "' found: " + regionId);
                continue;
            }
            this.adjacentRegions.add(region);
        }
        this.alliance = plugin.getAllianceCache().getById(config.getInt("alliance", -1));
        for (String string : config.getStringList("chunks")) {
            try {
                addChunk(new LazyChunk(string));
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                FLogger.ERROR.log("Illegal chunk in region '" + id + "' found: '" + string + "'");
            }
        }
        damageReduction = config.getDouble("damageReduction", damageReduction);
        type = RegionType.getByName(config.getString("type", type.name()), type);
        mode = RegionMode.getByName(config.getString("mode", type.getDefaultMode().name()), type.getDefaultMode());
    }

    @Override
    protected void serializeData() {
        FLogger.REGION.log("Saving region '" + id + "'...");
        config.set("adjacentRegions", adjacentRegions.stream().map(Region::getId).toList());
        config.set("alliance", alliance == null ? null : alliance.getId());
        config.set("chunks", chunks.stream().map(LazyChunk::toString).toList());
        config.set("damageReduction", damageReduction);
        config.set("type", type.name());
        config.set("mode", mode.name());
    }

    /* Getters and setters */

    public @NotNull RegionCache getRegionCache() {
        return regionCache;
    }

    public @NotNull UUID getWorldId() {
        return regionCache.getWorldId();
    }

    public @NotNull World getWorld() {
        World world = Bukkit.getWorld(getWorldId());
        return world == null ? Bukkit.getWorlds().get(0) : world;
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
        Faction owner = getOwner();
        Faction otherOwner = other.getOwner();
        if (owner != null && otherOwner != null) {
            owner.addAdjacentFaction(otherOwner);
        }
        return true;
    }

    public boolean removeAdjacentRegion(@NotNull Region other) {
        if (!(adjacentRegions.remove(other) || other.adjacentRegions.remove(this))) {
            return false;
        }
        // Check whether another region ensures the adjacency and remove adjacent faction if not.
        Faction owner = getOwner();
        Faction otherOwner = other.getOwner();
        if (owner != null && otherOwner != null && !isAdjacentToFaction(owner, otherOwner)) {
            owner.removeAdjacentFaction(otherOwner);
        }
        return true;
    }

    private boolean isAdjacentToFaction(@NotNull Faction a, @NotNull Faction b) {
        for (Region region : a.getRegions()) {
            for (Region adjacent : region.getAdjacentRegions()) {
                if (adjacent.getOwner() == b) {
                    return true;
                }
            }
        }
        return false;
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

    public void addChunks(@NotNull Collection<LazyChunk> lazyChunks) {
        for (LazyChunk lazyChunk : lazyChunks) {
            addChunk(lazyChunk);
        }
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

    public double getDamageReduction() {
        return damageReduction;
    }

    public void setDamageReduction(double damageReduction) {
        this.damageReduction = damageReduction;
    }

    /**
     * Gets the owner of this region. Override in subclasses that support ownership.
     * @return the owning faction, or null if not owned or not ownable
     */
    public @Nullable Faction getOwner() {
        return null;
    }

    @Override
    public @Nullable Faction getFaction() {
        return getOwner();
    }

    /**
     * @return true if this region has an owner
     */
    public boolean isOwned() {
        return getOwner() != null;
    }

    /**
     * @return the display string for the owner
     */
    public @NotNull String getDisplayOwner() {
        Faction owner = getOwner();
        return owner != null ? owner.getName(true) : FMessage.GENERAL_WILDERNESS.getMessage() + (alliance == null ? "" : " (" + alliance.getName(true) + ")");
    }

    public @NotNull RegionType getType() {
        return type;
    }

    public void setType(@NotNull RegionType type) {
        this.type = type;
    }

    public @NotNull RegionMode getMode() {
        return mode;
    }

    public void setMode(@NotNull RegionMode mode) {
        this.mode = mode;
    }

    /**
     * Returns a set of positions for the given POI type.
     *
     * @param type The type of POI
     * @return A set of positions for the given POI type. May be empty.
     */
    public Set<RegionPOIContainer> getPOIs(@NotNull RegionPOIType type) {
        return poiMap.computeIfAbsent(type, k -> new HashSet<>());
    }

    /**
     * Adds a position to the POI set for the given type.
     * If the type does not exist, it will be created.
     * @param type The type of POI
     * @param container The POI to add
     */
    public void addPOI(@NotNull RegionPOIType type, @NotNull RegionPOIContainer container) {
        getPOIs(type).add(container);
    }

    /**
     * Removes a position from the POI set for the given type.
     * If the type does not exist, nothing happens.
     *
     * @param type The type of POI
     * @param container The position to remove
     */
    public void removePOI(@NotNull RegionPOIType type, @NotNull RegionPOIContainer container) {
        Set<RegionPOIContainer> containers = getPOIs(type);
        if (containers != null) {
            containers.remove(container);
        }
    }

    /**
     * Returns the nearest build site for the given type and position.
     * If no POI of the given type exists, null is returned.
     *
     * @param type The type of POI
     * @param position The position to search from
     * @return The nearest POI build site or null if none exists
     */
    @SuppressWarnings("UnstableApiUsage")
    public BuildSite getNearestPOISite(RegionPOIType type, Position position) {
        Set<RegionPOIContainer> containers = getPOIs(type);
        if (containers.isEmpty()) {
            return null;
        }
        RegionPOIContainer nearest = null;
        Vector pos = position.toVector();
        double nearestDistance = Double.MAX_VALUE;
        for (RegionPOIContainer container : containers) {
            double distance = container.pos().toVector().distance(pos);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = container;
            }
        }
        return nearest == null ? null : nearest.buildSite();
    }

    @Override
    public @NotNull Component asComponent(@NotNull FEntity viewer) {
        Component component = Component.text(getName());
        Component hoverMessage = Component.translatable("factions.region.info.header", "factions.region.info.header", Component.text(getName(true)));
        int lowerLevelBound = this instanceof PvERegion pve ? pve.getLowerLevelBound() : -1;
        int upperLevelBound = this instanceof PvERegion pve ? pve.getUpperLevelBound() : -1;
        hoverMessage = hoverMessage.append(Component.translatable("factions.region.info.header", "factions.region.info.header", Component.text(getName(true)), Component.text(lowerLevelBound == -1 ? "~" : lowerLevelBound + ""), Component.text(upperLevelBound == -1 ? "-" : upperLevelBound + ""))).append(Component.newline());
        hoverMessage = hoverMessage.append(Component.translatable("factions.region.info.type","factions.region.info.type", Component.text(getType().getName())));
        hoverMessage = hoverMessage.append(Component.translatable("factions.region.info.owner", "factions.region.info.owner", Component.text(getDisplayOwner())));
        int buildSiteCount = this instanceof ClaimableRegion cr ? cr.getBuildSites().size() : 0;
        hoverMessage = hoverMessage.append(Component.translatable("factions.region.info.buildings", "factions.region.info.buildings", Component.text(buildSiteCount)));
        hoverMessage = hoverMessage.append(Component.translatable("factions.general.clickHints.region"));
        component = component.hoverEvent(HoverEvent.showText(hoverMessage));
        component = component.clickEvent(ClickEvent.runCommand("/f region info " + getId()));
        return component;
    }

    @Override
    public @NotNull Iterable<? extends Audience> audiences() {
        Set<Player> players = new HashSet<>();
        plugin.getFPlayerCache().forEach(player -> {
            if (player.getCurrentRegion() == this) {
                players.add(player.getPlayer());
            }
        });
        return players;
    }

    public @NotNull Iterable<? extends Audience> friendlyAudiences() {
        Set<Player> players = new HashSet<>();
        plugin.getFPlayerCache().forEach(player -> {
            if (player.getCurrentRegion() == this) {
                if (player.getFaction() == null) {
                    return;
                }
                if (player.getFaction().getAlliance() == alliance) {
                    players.add(player.getPlayer());
                }
            }
        });
        return players;
    }

    @Override
    public double getAttributeValue(@NotNull String name, double def) {
        FactionAttribute attribute = getAttribute(name);
        Faction owner = getOwner();
        if (alliance == null) {
            return attribute == null ? def : attribute.getValue();
        }
        double value = (owner == null ? alliance : owner).getAttributeValue(name, def);
        if (attribute == null) {
            return value;
        }
        for (FactionAttributeModifier modifier : attribute.getModifiers()) {
            value = modifier.apply(value);
        }
        return value;
    }

    @Override
    public boolean hasPolicy(@NotNull FPolicy policy) {
        if (alliance != null && alliance.hasPolicy(policy)) {
            return true;
        }
        Faction owner = getOwner();
        if (owner != null && owner.hasPolicy(policy)) {
            return true;
        }
        return super.hasPolicy(policy);
    }

    /* Object methods */

    @Override
    public int hashCode() {
        return Objects.hash(getWorldId(), id);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Region other)) return false;
        return id == other.getId() && getWorldId().equals(other.getWorldId());
    }

    @Override
    public String toString() {
        return "Region[world=" + getWorldId() + ",id=" + id + "]";
    }

    public @NotNull JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("type", type.name());
        Faction owner = getOwner();
        json.addProperty("owner", owner == null ? -1 : owner.getId());
        json.addProperty("alliance", alliance == null ? -1 : alliance.getId());
        json.addProperty("claimable", this instanceof ClaimableRegion);
        return json;
    }
}
