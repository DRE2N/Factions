package de.erethon.factions.building;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.economy.FStorage;
import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.player.FPlayerCache;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionManager;
import de.erethon.factions.region.RegionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Building {

    Factions plugin = Factions.get();
    BuildingManager manager = plugin.getBuildingManager();

    public static final String YAML = ".yml";

    private File file;
    private FileConfiguration config;

    private String id;
    private Component name;
    private List<Component> description;
    private boolean isCoreRequired;
    private boolean isWarBuilding;
    private int size;
    private Map<Resource, Integer> unlockCost = new HashMap<>();
    private Map<Material, Integer> requiredBlocks = new HashMap<>();
    private Map<FSetTag, Integer> requiredBlockTypes = new HashMap<>();
    private Map<PopulationLevel, Integer> requiredPopulation = new HashMap<>();
    private Set<RegionType> requiredRegionTypes = new HashSet<>();
    private Biome requiredBiome;
    private List<String> requiredBuildings = new ArrayList<>(); // String with ids because the other buildings might not be loaded yet.
    private Set<BuildingEffect> effects = new HashSet<>();
    Material icon;


    public Building(File file) {
        this.file = file;
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException exception) {
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        id = file.getName().replace(YAML, "");
        load();
    }


    public void build(Player p, Faction faction, Region rg, Location center) {
        pay(faction);
        new BuildSite(this, rg, getCorner1(center), getCorner2(center), center);
        MessageUtil.sendMessage(p, FMessage.BUILDING_SITE_CREATED.getMessage());
    }

    public boolean checkRequirements(Player p, Faction faction, Location loc) {
        FPlayerCache playerCache = plugin.getFPlayerCache();
        RegionManager board = plugin.getRegionManager();
        FPlayer fPlayer = playerCache.getByPlayer(p);
        if (faction == null) {
            MessageUtil.sendMessage(p, FMessage.ERROR_PLAYER_IS_NOT_IN_A_FACTION.getMessage());
            return false;
        }
        if (!faction.isPrivileged(fPlayer)) {
            MessageUtil.sendMessage(p, FMessage.ERROR_NO_PERMISSION.getMessage());
            return false;
        }
        Region rg = fPlayer.getLastRegion();
        if (rg == null) {
            MessageUtil.sendMessage(p, FMessage.ERROR_REGION_NOT_FOUND.getMessage());
            return false;
        }
        // If the faction does not own the region and the building is not a war building
        if (rg.getOwner() != faction && !isWarBuilding()) {
            MessageUtil.sendMessage(p, FMessage.ERROR_REGION_NOT_FOUND.getMessage());
            return false;
        }
        /*boolean isBorder = false;
        LazyChunk chunk = new LazyChunk(p.getChunk());
        for (Chunk c : chunk.getFastChunksAround(p.getWorld())) {
            if (board.getRegionByChunk(c, rg) != rg) {
                isBorder = true;
            }
        }
        // If the building area overlaps with another region
        if (isBorder) {
            MessageUtil.sendMessage(p, FMessage.ERROR_BUILDING_TOO_CLOSE_BORDER.getMessage());
            return false;
        }*/
        boolean isInOtherBuilding = false;
        for (BuildSite site : rg.getBuildSites()) {
            if (manager.hasOverlap(getCorner1(loc), getCorner2(loc), site)) {
                isInOtherBuilding = true;
            }
        }
        // If the building overlaps with an existing building
        if (isInOtherBuilding) {
            MessageUtil.sendMessage(p, FMessage.ERROR_BUILDING_BLOCKED.getMessage());
            return false;
        }
        // If the region is not of the correct RegionType
        if (!hasRequiredType(rg)) {
            MessageUtil.sendMessage(p, FMessage.ERROR_BUILDING_REQUIRED_TYPE.getMessage());
            return false;
        }
        // If the building requires other buildings to be built first in this faction
        if (!hasRequiredBuilding(faction)) {
            MessageUtil.sendMessage(p, FMessage.ERROR_BUILDING_REQUIRED_FACTION.getMessage());
            return false;
        }
        // If the building requires a certain amount of population at a specific level
        if (!hasRequiredPopulation(rg)) {
            MessageUtil.sendMessage(p, FMessage.ERROR_BUILDING_POPULATION.getMessage());
            return false;
        }
        // If the faction can not afford the unlock costs.
        if (!canPay(faction)) {
            MessageUtil.sendMessage(p, FMessage.ERROR_BUILDING_NOT_ENOUGH_RESOURCES.getMessage());
            return false;
        }
        return true;
    }

    public boolean canPay(Faction f) {
        FStorage storage = f.getStorage();
        boolean canPay = true;
        for (Map.Entry<Resource, Integer> resource : getUnlockCost().entrySet()) {
            if (!storage.canAfford(resource.getKey(), resource.getValue())) {
                canPay = false;
            }
        }
        return canPay;
    }

    public void pay(Faction faction) {
        FStorage storage = faction.getStorage();
        for (Map.Entry<Resource, Integer> resource : getUnlockCost().entrySet()) {
            storage.removeResource(resource.getKey(), resource.getValue());
        }
    }

    public boolean hasRequiredBuilding(Faction f) {
        BuildingManager buildingManager = plugin.getBuildingManager();
        Set<Building> buildings = new HashSet<>();
        if (getRequiredBuildings() == null || getRequiredBuildings().isEmpty()) {
            return true;
        }
        if (f.getFactionBuildings() == null || f.getFactionBuildings().isEmpty()) {
            return false;
        }
        for (BuildSite bs : f.getFactionBuildings()) {
            if (!bs.isFinished()) {
                continue;
            }
            buildings.add(bs.getBuilding());
        }
        Set<Building> required = new HashSet<>();
        for (String s : requiredBuildings) {
            required.add(buildingManager.getByID(s));
        }
        return buildings.containsAll(required);
    }

    public boolean hasRequiredPopulation(Region rg) {
        Set<PopulationLevel> pop = new HashSet<>();
        Faction f = rg.getOwner();
        if (f == null) {
            return false;
        }
        if (getRequiredPopulation() == null || getRequiredPopulation().isEmpty()) {
            return true;
        }
        if (f.getPopulation() == null || f.getPopulation().isEmpty()) {
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

    public boolean hasRequiredType(Region rg) {
        return requiredRegionTypes.contains(rg.getType());
    }

    /**
     * Displays a particle frame with the maximum building size
     * @param player the player who will see the particles
     * @param center the center location of the building
     * @param allowed true/false = green/red
     */
    public void displayFrame(Player player, Location center, boolean allowed) {
        BukkitRunnable particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                List<Location> result = new ArrayList<>();
                World world = center.getWorld();
                int radius = getSize();
                int cx = center.getBlockX() + radius;
                int cy = center.getBlockY() + (radius * 2);
                int cz = center.getBlockZ() + radius;
                int cx2 = center.getBlockX() - radius;
                int cy2 = center.getBlockY() - (radius / 2); // don't go underground  too much.
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
                        player.spawnParticle(Particle.REDSTONE, loc, 5, new Particle.DustOptions(Color.LIME, 3));
                    } else {
                        player.spawnParticle(Particle.REDSTONE, loc, 5, new Particle.DustOptions(Color.RED, 3));
                    }
                }
            }
        };
        particleTask.runTaskTimer(plugin, 0, 20);
        BukkitRunnable cancel = new BukkitRunnable() {
            @Override
            public void run() {
                particleTask.cancel();
            }
        };
        cancel.runTaskLater(plugin, 200);
    }

    public Location getCorner1(Location center) {
        World world = center.getWorld();
        int radius = getSize();
        int x = center.getBlockX() + radius;
        int y = center.getBlockY() + (radius * 2);
        int z = center.getBlockZ() + radius;
        return new Location(world, x, y, z);
    }

    public Location getCorner2(Location center) {
        World world = center.getWorld();
        int radius = getSize();
        int x = center.getBlockX() - radius;
        int y = center.getBlockY() - (radius / 2);
        int z = center.getBlockZ() - radius;
        return new Location(world, x, y, z);
    }

    public void setId(String identifier) {
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


    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Map<Resource, Integer> getUnlockCost() {
        return unlockCost;
    }

    public void setUnlockCost(Map<Resource, Integer> unlockCost) {
        this.unlockCost = unlockCost;
    }

    public Map<Material, Integer> getRequiredBlocks() {
        return requiredBlocks;
    }

    public Map<PopulationLevel, Integer> getRequiredPopulation() {
        return requiredPopulation;
    }

    public void setRequiredBlocks(Map<Material, Integer> requiredBlocks) {
        this.requiredBlocks = requiredBlocks;
    }

    public List<String> getRequiredBuildings() {
        return requiredBuildings;
    }


    public String getId() {
        return id;
    }

    public List<Component> getDescription() {
        return description;
    }

    public Component getName() {
        return name;
    }

    public Set<BuildingEffect> getEffects() {
        return effects;
    }

    public Material getIcon() {
        return icon;
    }

    public boolean isBuilt(Region rg) {
        for (BuildSite buildSite : rg.getBuildSites()) {
            if (buildSite.getBuilding() == this && buildSite.isFinished() && !buildSite.isDestroyed()) {
                return true;
            }
        }
        return false;
    }

    public boolean isBuilt(Faction faction) {
        for (BuildSite buildSite : faction.getFactionBuildings()) {
            if (buildSite.getBuilding() == this && buildSite.isFinished() && !buildSite.isDestroyed()) {
                return true;
            }
        }
        return false;
    }

    public void load() {
        ConfigurationSection config = this.config;
        name = MiniMessage.miniMessage().deserialize(config.getString("name"));
        MessageUtil.log("Loading building " + name + "...");
        isCoreRequired = config.getBoolean("coreRequired");
        size = config.getInt("size");
        for (String s : config.getStringList("description")) {
            description.add(MiniMessage.miniMessage().deserialize(s));
        }
        requiredBuildings = (List<String>) config.getList("requiredBuildings");
        if (config.contains("icon")) {

            Material material = Material.getMaterial(config.getString("icon"));
            if (material == null) {
                icon = Material.BARRIER;
            } else {
                icon = material;
            }
        }
        if (config.contains("requiredCategories")) {
            Set<String> cfgList = config.getConfigurationSection("requiredCategories").getKeys(false);
            for (String s : cfgList) {
                FSetTag tag = FSetTag.valueOf(s);
                int amount = config.getInt("requiredCategories." + s);
                requiredBlockTypes.put(tag, amount);
            }
        }
        if (config.contains("requiredBlocks")) {
            Set<String> cfgList = config.getConfigurationSection("requiredBlocks").getKeys(false);
            for (String s : cfgList) {
                Material material = Material.getMaterial(s);
                int amount = config.getInt("requiredBlocks." + s);
                requiredBlocks.put(material, amount);
            }
        }
        if (config.contains("unlockCost")) {
            Set<String> cfgList = config.getConfigurationSection("unlockCost").getKeys(false);
            for (String s : cfgList) {
                Resource resource = Resource.getByID(s);
                int mod = config.getInt("unlockCost." + s);
                unlockCost.put(resource, mod);
            }
        }
        if (config.contains("requiredPopulation")) {
            Set<String> cfgList = config.getConfigurationSection("requiredPopulation").getKeys(false);
            for (String s : cfgList) {
                PopulationLevel level = PopulationLevel.valueOf(s);
                int mod = config.getInt("requiredPopulation." + s);
                requiredPopulation.put(level, mod);
            }
        }
        if (config.contains("requiredRegionTypes")) {
            Set<String> cfgList = config.getConfigurationSection("requiredRegionTypes").getKeys(false);
            for (String s : cfgList) {
                RegionType type = RegionType.valueOf(s);
                requiredRegionTypes.add(type);
            }
        }
        if (config.contains("effects")) {
            for (String key : config.getConfigurationSection("effects").getKeys(false)) {
                try {
                    effects.add(new BuildingEffect().fromConfigSection(config.getConfigurationSection("effects." + key)));
                } catch (NullPointerException ex) {
                    MessageUtil.log("There was an error loading effect " + key + " (Building: " + name + ")");
                    MessageUtil.log(ex.toString());
                }
            }
        }
        MessageUtil.log("Loaded building with size " + size);
        MessageUtil.log("Blocks: " + requiredBlocks.toString());
        MessageUtil.log("Effects: " + effects.toString());

    }

    public void save() {

    }

}