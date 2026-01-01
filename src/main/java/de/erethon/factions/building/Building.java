package de.erethon.factions.building;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.economy.FStorage;
import de.erethon.factions.economy.FactionLevel;
import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.player.FPlayerCache;
import de.erethon.factions.region.ClaimableRegion;
import de.erethon.factions.region.LazyChunk;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionManager;
import de.erethon.factions.region.RegionType;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.util.FTutorial;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.print.attribute.IntegerSyntax;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author Malfrador
 */
public class Building {

    private final Factions plugin = Factions.get();
    private  BuildingManager manager;

    public static final String YAML = ".yml";

    private final File file;
    protected final FileConfiguration config;

    private String id;
    private final List<Component> description = new ArrayList<>();
    private boolean isCoreRequired;
    private boolean isWarBuilding;
    private boolean allowOverlap;
    private boolean isUnique;
    private int size;
    private Map<Resource, Integer> unlockCost = new HashMap<>();
    private final List<BlockRequirement> requiredBlocks = new ArrayList<>();
    private final Map<PopulationLevel, Integer> requiredPopulation = new HashMap<>();
    private final Set<RegionType> requiredRegionTypes = new HashSet<>();
    private Biome requiredBiome;
    private Map<String, Integer> requiredBuildings = new HashMap<>(); // String with ids because the other buildings might not be loaded yet.
    private final Set<BuildingEffectData> effects = new HashSet<>();
    private final Set<String> requiredSections = new HashSet<>();
    private final Set<Material> blocksOfInterest = new HashSet<>();
    private FactionLevel requiredLevel = FactionLevel.HAMLET;
    private Material icon = Material.CHEST;

    public Building(@NotNull File file, BuildingManager manager) {
        this.file = file;
        this.manager = manager;
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        id = file.getName().replace(YAML, "");
        load();
    }

    public void build(@NotNull Player player, @NotNull Faction faction, @NotNull ClaimableRegion region, @NotNull Location center) {
        pay(faction);
        new BuildSite(this, region, getCorner1(center), getCorner2(center), center);
        MessageUtil.sendMessage(player, FMessage.BUILDING_SITE_CREATED.getMessage());
        FTutorial.showHint(player, "building.first_build_site");
    }

    public Set<RequirementFail> checkRequirements(@NotNull Player player, @Nullable Faction faction, @NotNull Location loc) {
        Set<RequirementFail> fails = new HashSet<>();
        FPlayerCache playerCache = plugin.getFPlayerCache();
        RegionManager board = plugin.getRegionManager();
        FPlayer fPlayer = playerCache.getByPlayer(player);
        if (faction == null) {
            fails.add(RequirementFail.NOT_IN_FACTION);
            return fails;
        }
        if (!faction.isPrivileged(fPlayer)) {
            fails.add(RequirementFail.NO_PERMISSION);
        }
        Region rg = fPlayer.getCurrentRegion();
        if (rg == null) {
            fails.add(RequirementFail.NOT_IN_REGION);
            return fails;
        }
        if (!(rg instanceof ClaimableRegion claimableRg)) {
            fails.add(RequirementFail.NOT_IN_REGION);
            return fails;
        }
        // If the faction does not own the region and the building is not a war building
        if (claimableRg.getOwner() != faction && !isWarBuilding()) {
            fails.add(RequirementFail.NOT_IN_REGION);
        }
        boolean isBorder = false;
        LazyChunk chunk = new LazyChunk(player.getChunk());
        int x = chunk.getX();
        int z = chunk.getZ();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = z - 1; j <= z + 1; j++) {
                LazyChunk c = new LazyChunk(i, j);
                if (board.getRegionByChunk(c.asBukkitChunk(player.getWorld())) != rg) {
                    isBorder = true;
                }
            }
        }
        // If the building area overlaps with another region
        if (isBorder) {
            //MessageUtil.sendMessage(player, FMessage.ERROR_BUILDING_TOO_CLOSE_BORDER.getMessage());
            fails.add(RequirementFail.TOO_CLOSE_TO_BORDER);
        }
        boolean isInOtherBuilding = false;
        BuildSite overlappingSite = null;
        if (manager == null) {
            Factions.log("BuildingManager is null somehow. Plugin: " + plugin);
            return fails;
        }
        for (BuildSite site : claimableRg.getBuildSites()) {
            if (manager.hasOverlap(getCorner1(loc), getCorner2(loc), site)) {
                isInOtherBuilding = true;
                overlappingSite = site;
            }
        }
        // If the building overlaps with an existing building
        if (isInOtherBuilding && !(allowOverlap || overlappingSite.getBuilding().isAllowOverlap())) {
            fails.add(RequirementFail.OVERLAPPING_BUILDING);
        }
        // If the building is unique and the faction already has a build site for this building
        if (isUnique && hasBuildSiteAlready(faction)) {
            fails.add(RequirementFail.UNIQUE_BUILDING);
        }
        // If the region is not of the correct RegionType
        if (!hasRequiredType(rg)) {
            fails.add(RequirementFail.WRONG_REGION_TYPE);
        }
        // If the building requires other buildings to be built first in this faction
        if (!hasRequiredBuildings(faction)) {
            fails.add(RequirementFail.REQUIRED_BUILDING_MISSING);
        }
        // If the building requires a certain amount of population at a specific level
        if (!hasRequiredPopulation(rg)) {
            fails.add(RequirementFail.REQUIRED_POPULATION);
        }
        // If the faction can not afford the unlock costs.
        if (!canPay(faction)) {
            fails.add(RequirementFail.NOT_ENOUGH_RESOURCES);
        }
        return fails;
    }

    public boolean canPay(@NotNull Faction faction) {
        FStorage storage = faction.getStorage();
        boolean canPay = true;
        for (Map.Entry<Resource, Integer> resource : getUnlockCost().entrySet()) {
            if (!storage.canAfford(resource.getKey(), resource.getValue())) {
                canPay = false;
            }
        }
        return canPay;
    }

    public void pay(@NotNull Faction faction) {
        FStorage storage = faction.getStorage();
        for (Map.Entry<Resource, Integer> resource : getUnlockCost().entrySet()) {
            storage.removeResource(resource.getKey(), resource.getValue());
        }
    }

    public boolean hasRequiredBuildings(@NotNull Faction faction) {
        BuildingManager buildingManager = plugin.getBuildingManager();
        Map<String, Integer> buildings = new HashMap<>();
        if (getRequiredBuildings().isEmpty()) {
            return true;
        }
        if (faction.getFactionBuildings().isEmpty()) {
            return false;
        }
        for (BuildSite bs : faction.getFactionBuildings()) {
            if (!bs.isFinished()) {
                continue;
            }
            buildings.put(bs.getBuilding().getId(), buildings.getOrDefault(bs.getBuilding().getId(), 0) + 1);
        }
        for (String id : requiredBuildings.keySet()) {
            if (!buildings.containsKey(id)) {
                return false;
            }
            if (buildings.get(id) < getRequiredBuildings().get(id)) {
                return false;
            }
        }
        return true;
    }

    public boolean hasRequiredPopulation(@NotNull Region region) {
        Set<PopulationLevel> pop = new HashSet<>();
        Faction f = region.getOwner();
        if (f == null) {
            return false;
        }
        if (getRequiredPopulation().isEmpty()) {
            return true;
        }
        if (f.getPopulation().isEmpty()) {
            return false;
        }
        boolean requirements = true;
        for (PopulationLevel level : getRequiredPopulation().keySet()) {
            if (f.getPopulation().get(level) < getRequiredPopulation().get(level)) {
                requirements = false;
            }
        }
        return requirements;
    }

    public boolean hasRequiredType(@NotNull Region region) {
        return requiredRegionTypes.contains(region.getType()) || requiredRegionTypes.isEmpty();
    }

    /**
     * Displays a particle frame with the maximum building size
     * @param player the player who will see the particles
     * @param center the center location of the building
     * @param allowed true/false = green/red
     */
    public void displayFrame(@NotNull Player player, @NotNull Location center, boolean allowed) {
        List<Location> result = new ArrayList<>();
        World world = center.getWorld();
        int radius = getSize();
        int cx = center.getBlockX() + radius;
        int cy = center.getBlockY() + (radius * 2);
        int cz = center.getBlockZ() + radius;
        int cx2 = center.getBlockX() - radius;
        int cy2 = center.getBlockY() - (radius / 2); // don't go underground too much.
        int cz2 = center.getBlockZ() - radius;
        Location corner1 = new Location(world, cx, cy, cz);
        Location corner2 = new Location(world, cx2, cy2, cz2);
        double minX = Math.min(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());

        for (double x = minX; x <= maxX; x+=1) {
            for (double y = minY; y <= maxY; y+=1) {
                for (double z = minZ; z <= maxZ; z+=1) {
                    int components = 0;
                    if (x == minX || x == maxX) components++;
                    if (y == minY || y == maxY) components++;
                    if (z == minZ || z == maxZ) components++;
                    if (components >= 2) {
                        result.add(new Location(world, x, y, z));
                    }
                }
            }
        }
        for (Location loc : result) {
            if (allowed) {
                player.spawnParticle(Particle.DUST, loc, 5, new Particle.DustOptions(Color.LIME, 1));
            } else {
                player.spawnParticle(Particle.DUST, loc, 5, new Particle.DustOptions(Color.RED, 1));
            }
        }
    }

    public @NotNull Location getCorner1(@NotNull Location center) {
        World world = center.getWorld();
        int radius = getSize();
        int x = center.getBlockX() + radius;
        int y = center.getBlockY() + (radius * 2);
        int z = center.getBlockZ() + radius;
        return new Location(world, x, y, z);
    }

    public @NotNull Location getCorner2(@NotNull Location center) {
        World world = center.getWorld();
        int radius = getSize();
        int x = center.getBlockX() - radius;
        int y = center.getBlockY() - (radius / 2);
        int z = center.getBlockZ() - radius;
        return new Location(world, x, y, z);
    }

    public void setId(@NotNull String identifier) {
        this.id = identifier;
    }

    public boolean isCoreRequired() {
        return isCoreRequired;
    }

    public void setCoreRequired(boolean coreRequired) {
        isCoreRequired = coreRequired;
    }

    public boolean isWarBuilding() {
        return isWarBuilding;
    }

    public boolean isAllowOverlap() {
        return allowOverlap;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public @NotNull Map<Resource, Integer> getUnlockCost() {
        return unlockCost;
    }

    public void setUnlockCost(@NotNull Map<Resource, Integer> unlockCost) {
        this.unlockCost = unlockCost;
    }

    /**
     * Adds a requirement for a specific Material.
     *
     * @param material The Material to require
     * @param amount   The amount required
     */
    public void addRequiredBlocks(Material material, int amount) {
        requiredBlocks.add(new BlockRequirement(material, amount));
    }

    /**
     * Adds a requirement for blocks matching an FSetTag.
     *
     * @param tag    The FSetTag to require
     * @param amount The amount required
     */
    public void addRequiredBlocks(FSetTag tag, int amount) {
        requiredBlocks.add(new BlockRequirement(tag, amount));
    }

    /**
     * Gets all block requirements for this building.
     *
     * @return The list of BlockRequirements
     */
    public List<BlockRequirement> getBlockRequirements() {
        return requiredBlocks;
    }

    public @NotNull Map<PopulationLevel, Integer> getRequiredPopulation() {
        return requiredPopulation;
    }

    public @NotNull Map<String, Integer> getRequiredBuildings() {
        return requiredBuildings;
    }

    public @NotNull Set<RegionType> getRequiredRegionTypes() {
        return requiredRegionTypes;
    }

    public @NotNull Biome getRequiredBiome() {
        return requiredBiome;
    }

    public @NotNull Set<String> getRequiredSections() {
        return requiredSections;
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull List<Component> getDescription() {
        return description;
    }

    public @NotNull Set<BuildingEffectData> getEffects() {
        return effects;
    }

    public @NotNull Set<Material> getBlocksOfInterest() {
        return blocksOfInterest;
    }

    public @NotNull Material getIcon() {
        return icon;
    }

    public boolean isBuilt(@NotNull ClaimableRegion region) {
        for (BuildSite buildSite : region.getBuildSites()) {
            if (buildSite.getBuilding() == this && buildSite.isFinished() && !buildSite.isDestroyed()) {
                return true;
            }
        }
        return false;
    }

    public boolean isBuilt(@NotNull Faction faction) {
        for (BuildSite buildSite : faction.getFactionBuildings()) {
            if (buildSite.getBuilding() == this && buildSite.isFinished() && !buildSite.isDestroyed()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasBuildSiteAlready(@NotNull Faction faction) {
        for (BuildSite buildSite : faction.getFactionBuildings()) {
            if (buildSite.getBuilding() == this) {
                return true;
            }
        }
        return false;
    }

    public void load() {
        ConfigurationSection config = this.config;
        FLogger.BUILDING.log("Loading building " + id + "...");
        isCoreRequired = config.getBoolean("coreRequired", false);
        isWarBuilding = config.getBoolean("warBuilding", false);
        isUnique = config.getBoolean("unique", false);
        allowOverlap = config.getBoolean("allowOverlap", false);
        size = config.getInt("size");
        requiredLevel = FactionLevel.valueOf(config.getString("requiredLevel", "HAMLET"));
        if (config.contains("requiredBuildings")) {
            Set<String> cfgList = config.getConfigurationSection("requiredBuildings").getKeys(false);
            for (String s : cfgList) {
                requiredBuildings.put(s, config.getInt("requiredBuildings." + s));
            }
        }
        if (config.contains("requiredSections")) {
            requiredSections.addAll(config.getStringList("requiredSections"));
        }
        if (config.contains("icon")) {
            Material material = Material.getMaterial(config.getString("icon"));
            icon = material == null ? Material.BARRIER : material;
        }
        if (config.contains("blocksOfInterest")) {
            List<String> list = config.getStringList("blocksOfInterest");
            for (String s : list) {
                Material material = Material.getMaterial(s.toUpperCase());
                if (material != null) {
                    blocksOfInterest.add(material);
                }
            }
        }
        if (config.contains("requiredBlocks")) {
            Set<String> cfgList = config.getConfigurationSection("requiredBlocks").getKeys(false);
            for (String s : cfgList) {
                if (FSetTag.isValidTag(s.toUpperCase())) {
                    FSetTag tag = FSetTag.valueOf(s.toUpperCase());
                    int amount = config.getInt("requiredBlocks." + s);
                    requiredBlocks.add(new BlockRequirement(tag, amount));
                    continue;
                }
                Material material = Material.getMaterial(s.toUpperCase());
                if (material == null) {
                    FLogger.ERROR.log("Invalid material in requiredBlocks for building " + id + ": " + s);
                    continue;
                }
                int amount = config.getInt("requiredBlocks." + s);
                requiredBlocks.add(new BlockRequirement(material, amount));
            }
        }
        if (config.contains("unlockCost")) {
            Set<String> cfgList = config.getConfigurationSection("unlockCost").getKeys(false);
            for (String s : cfgList) {
                Resource resource = Resource.getById(s.toUpperCase());
                int mod = config.getInt("unlockCost." + s);
                unlockCost.put(resource, mod);
            }
        }
        if (config.contains("requiredPopulation")) {
            Set<String> cfgList = config.getConfigurationSection("requiredPopulation").getKeys(false);
            for (String s : cfgList) {
                PopulationLevel level = PopulationLevel.valueOf(s.toUpperCase());
                int mod = config.getInt("requiredPopulation." + s);
                requiredPopulation.put(level, mod);
            }
        }
        if (config.contains("requiredRegionTypes")) {
            Set<String> cfgList = config.getConfigurationSection("requiredRegionTypes").getKeys(false);
            for (String s : cfgList) {
                RegionType type = RegionType.valueOf(s.toUpperCase());
                requiredRegionTypes.add(type);
            }
        }
        if (config.contains("effects")) {
            ConfigurationSection effectsParentSection = config.getConfigurationSection("effects");
            if (effectsParentSection != null) {
                for (String effectKey : effectsParentSection.getKeys(false)) {
                    try {
                        ConfigurationSection section = effectsParentSection.getConfigurationSection(effectKey);
                        BuildingEffectData effect = new BuildingEffectData(section, effectKey);
                        FLogger.BUILDING.log("Loaded effect data " + effect + " with parentSection " + effectsParentSection.getCurrentPath() + " and key " + effectKey);
                        effects.add(effect);
                    } catch (Exception e) {
                        FLogger.ERROR.log("Failed to load effect data for '" + effectKey + "' in building " + this.id + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } else {
                FLogger.BUILDING.log("Building " + this.id + " has an 'effects' key, but it's not a valid section or is empty.");
            }
        }
        FLogger.BUILDING.log("Loaded building with size " + size);
        FLogger.BUILDING.log("Blocks: " + requiredBlocks.toString());
        FLogger.BUILDING.log("Effects: " + effects);
    }

    public void save() {

    }

}

