package de.erethon.factions.region;

import com.google.gson.JsonObject;
import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.attributes.FactionAttribute;
import de.erethon.factions.building.attributes.FactionAttributeModifier;
import de.erethon.factions.data.FConfig;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.entity.FEntity;
import de.erethon.factions.entity.FLegalEntity;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.policy.FPolicy;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.util.FUtil;
import de.erethon.factions.war.RegionalWarTracker;
import io.papermc.paper.math.Position;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * @author Fyreum
 */
public class Region extends FLegalEntity {

    private final Factions plugin = Factions.get();

    private final RegionCache regionCache;
    /* Data */
    private final Set<Region> adjacentRegions = new HashSet<>();
    private final Set<BuildSite> buildSites = new HashSet<>();
    private Alliance alliance;
    private final Set<LazyChunk> chunks = new HashSet<>();
    private boolean claimable = true;
    private double damageReduction = 0.0;
    private double lastClaimingPrice;
    private Faction owner;
    private final RegionalWarTracker regionalWarTracker = new RegionalWarTracker(this);
    private final Map<String, RegionStructure> structures = new HashMap<>();
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
        claimable = config.getBoolean("claimable", claimable);
        damageReduction = config.getDouble("damageReduction", damageReduction);
        lastClaimingPrice = config.getDouble("lastClaimingPrice", lastClaimingPrice);
        owner = plugin.getFactionCache().getById(config.getInt("owner", -1));
        regionalWarTracker.load(config.getConfigurationSection("warTracker"));

        ConfigurationSection structuresSection = config.getConfigurationSection("structures");
        if (structuresSection != null) {
            for (String key : structuresSection.getKeys(false)) {
                ConfigurationSection section = structuresSection.getConfigurationSection(key);
                if (section == null) {
                    FLogger.ERROR.log("Unknown region structure in region '" + id + "' found: " + key);
                    continue;
                }
                RegionStructure structure = RegionStructure.deserialize(this, section);
                structures.put(structure.getName(), structure);
            }
            if (!structures.isEmpty()) {
                FLogger.REGION.log("Loaded " + structures.size() + " structures in region '" + id + "'");
            }
        }
        type = RegionType.getByName(config.getString("type", type.name()), type);
        if (type.isWarGround() && plugin.getWar() != null) {
            plugin.getWar().registerRegion(regionalWarTracker);
        }
    }

    @Override
    protected void serializeData() {
        FLogger.REGION.log("Saving region '" + id + "'...");
        config.set("adjacentRegions", adjacentRegions.stream().map(Region::getId).toList());
        config.set("alliance", alliance == null ? null : alliance.getId());
        config.set("chunks", chunks.stream().map(LazyChunk::toString).toList());
        config.set("claimable", claimable);
        config.set("damageReduction", damageReduction);
        config.set("lastClaimingPrice", lastClaimingPrice);
        config.set("owner", owner == null ? null : owner.getId());
        config.set("warTracker", regionalWarTracker.serialize());
        Map<String, Object> serializedStructures = new HashMap<>(structures.size());
        structures.forEach((name, structure) -> serializedStructures.put(String.valueOf(serializedStructures.size()), structure.serialize()));
        config.set("structures", serializedStructures);
        config.set("type", type.name());
        config.set("buildsites", buildSites.stream().map(BuildSite::getUUIDString).toList());
        for (BuildSite buildSite : buildSites) {
            try {
                buildSite.save();
            } catch (IOException e) {
                FLogger.REGION.log("Failed to save build site " + buildSite.getUuid() + " for region " + id + ": " + e.getMessage());
            }
        }
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
        if (owner != null && other.getOwner() != null && !FUtil.isAdjacent(owner, other.getOwner())) {
            owner.removeAdjacentFaction(other.getOwner());
        }
        return true;
    }

    public boolean isAdjacentRegion(@NotNull Region other) {
        assert other != this : "Region cannot be adjacent to itself";
        return adjacentRegions.contains(other);
    }

    public @NotNull Set<BuildSite> getBuildSites() {
        return buildSites;
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
        return isOwned() ? owner.getName() : FMessage.GENERAL_WILDERNESS.getMessage() + (alliance == null ? "" : " (" + alliance.getName() + ")");
    }

    public void setOwner(@Nullable Faction owner) {
        this.owner = owner;
    }

    public @NotNull RegionalWarTracker getRegionalWarTracker() {
        return regionalWarTracker;
    }

    public @NotNull Map<String, RegionStructure> getStructures() {
        return structures;
    }

    public <T extends RegionStructure> @NotNull Map<String, T> getStructures(@NotNull Class<T> type) {
        Map<String, T> filtered = new HashMap<>();
        for (RegionStructure structure : structures.values()) {
            if (type.isInstance(structure)) {
                filtered.put(structure.getName(), (T) structure);
            }
        }
        return filtered;
    }

    public @Nullable RegionStructure getStructure(@NotNull String name) {
        return structures.get(name);
    }

    public <T extends RegionStructure> @Nullable T getStructure(@NotNull String name, @NotNull Class<T> type) {
        RegionStructure structure = getStructure(name);
        return type.isInstance(structure) ? (T) structure : null;
    }

    @SuppressWarnings("UnstableApiUsage")
    public @Nullable RegionStructure getStructureAt(@NotNull Position position) {
        for (RegionStructure structure : structures.values()) {
            if (structure.containsPosition(position)) {
                return structure;
            }
        }
        return null;
    }

    @SuppressWarnings("UnstableApiUsage")
    public @NotNull List<RegionStructure> getStructuresAt(@NotNull Position position) {
        List<RegionStructure> found = new ArrayList<>();
        for (RegionStructure structure : structures.values()) {
            if (structure.containsPosition(position)) {
                found.add(structure);
            }
        }
        return found;
    }

    @SuppressWarnings("UnstableApiUsage")
    public <T extends RegionStructure> @NotNull List<T> getStructuresAt(@NotNull Position position, @NotNull Class<T> type) {
        List<T> found = new ArrayList<>();
        for (RegionStructure structure : structures.values()) {
            if (structure.containsPosition(position) && type.isInstance(structure)) {
                found.add((T) structure);
            }
        }
        return found;
    }

    public void addStructure(@NotNull RegionStructure structure) {
        structures.put(structure.getName(), structure);
    }

    public void removeStructure(@NotNull RegionStructure structure) {
        structures.remove(structure.getName());
    }

    public @NotNull RegionType getType() {
        return type;
    }

    public void setType(@NotNull RegionType type) {
        this.type = type;
        if (type.isWarGround()) {
            plugin.getWar().registerRegion(regionalWarTracker);
        } else {
            plugin.getWar().unregisterRegion(regionalWarTracker);
        }
    }

    @Override
    public @NotNull Component asComponent(@NotNull FEntity viewer) {
        Component component = Component.text(getName());
        Component hoverMessage = Component.translatable("factions.region.info.header", "factions.region.info.header", Component.text(getName()));
        hoverMessage = hoverMessage.append(Component.translatable("factions.region.info.header", "factions.region.info.header", Component.text(getName())));
        hoverMessage = hoverMessage.append(Component.translatable("factions.region.info.type","factions.region.info.type", Component.text(getType().getName())));
        hoverMessage = hoverMessage.append(Component.translatable("factions.region.info.owner", "factions.region.info.owner", Component.text(getDisplayOwner())));
        hoverMessage = hoverMessage.append(Component.translatable("factions.region.info.buildings", "factions.region.info.buildings", Component.text(buildSites.size())));
        hoverMessage = hoverMessage.append(Component.translatable("factions.general.clickHints.region"));
        component = component.hoverEvent(HoverEvent.showText(hoverMessage));
        component = component.clickEvent(ClickEvent.runCommand("/f region info " + getId()));
        return component;
    }

    @Override
    public @NotNull Iterable<? extends Audience> audiences() {
        Set<Player> players = new HashSet<>();
        plugin.getFPlayerCache().forEach(player -> {
            if (player.getLastRegion() == this) {
                players.add(player.getPlayer());
            }
        });
        return players;
    }

    public @NotNull Iterable<? extends Audience> friendlyAudiences() {
        Set<Player> players = new HashSet<>();
        plugin.getFPlayerCache().forEach(player -> {
            if (player.getLastRegion() == this) {
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
        json.addProperty("owner", owner == null ? -1 : owner.getId());
        json.addProperty("alliance", alliance == null ? -1 : alliance.getId());
        json.addProperty("claimable", claimable);
        return json;
    }

}
