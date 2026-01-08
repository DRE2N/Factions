package de.erethon.factions.building;

import com.destroystokyo.paper.MaterialSetTag;
import com.destroystokyo.paper.MaterialTags;
import de.erethon.factions.Factions;
import de.erethon.factions.util.FLogger;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages building tags loaded from configuration.
 * Tags can reference Minecraft/Bukkit tags or individual materials.
 *
 * @author Malfrador
 */
public class BuildingTagManager {

    private final Factions plugin;
    private final Map<String, FSetTag> tags = new HashMap<>();
    private final File configFile;

    public BuildingTagManager(Factions plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "buildingTags.yml");
    }

    /**
     * Loads tags from the buildingTags.yml configuration file.
     * Creates default configuration if it doesn't exist.
     */
    public void load() {
        tags.clear();

        if (!configFile.exists()) {
            createDefaultConfig();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        ConfigurationSection tagsSection = config.getConfigurationSection("tags");
        if (tagsSection == null) {
            FLogger.ERROR.log("No 'tags' section found in buildingTags.yml");
            return;
        }

        for (String tagName : tagsSection.getKeys(false)) {
            ConfigurationSection tagSection = tagsSection.getConfigurationSection(tagName);
            if (tagSection == null) {
                FLogger.WARN.log("Invalid tag configuration for: " + tagName);
                continue;
            }

            Set<Material> materials = new HashSet<>();

            // Load from Minecraft/Bukkit tags
            if (tagSection.contains("minecraftTags")) {
                List<String> minecraftTags = tagSection.getStringList("minecraftTags");
                for (String tagRef : minecraftTags) {
                    Set<Material> tagMaterials = resolveMinecraftTag(tagRef);
                    if (tagMaterials != null) {
                        materials.addAll(tagMaterials);
                    } else {
                        FLogger.WARN.log("Unknown Minecraft tag '" + tagRef + "' in building tag '" + tagName + "'");
                    }
                }
            }

            // Load individual materials
            if (tagSection.contains("materials")) {
                List<String> materialNames = tagSection.getStringList("materials");
                for (String materialName : materialNames) {
                    Material material = Material.getMaterial(materialName.toUpperCase());
                    if (material != null) {
                        materials.add(material);
                    } else {
                        FLogger.WARN.log("Unknown material '" + materialName + "' in building tag '" + tagName + "'");
                    }
                }
            }

            // Load reference to other building tags
            if (tagSection.contains("references")) {
                List<String> references = tagSection.getStringList("references");
                for (String ref : references) {
                    FSetTag referencedTag = tags.get(ref.toUpperCase());
                    if (referencedTag != null) {
                        materials.addAll(referencedTag.getMaterials());
                    } else {
                        FLogger.WARN.log("Referenced tag '" + ref + "' not found (referenced by '" + tagName + "'). Make sure it's defined before this tag.");
                    }
                }
            }

            if (materials.isEmpty()) {
                FLogger.WARN.log("Building tag '" + tagName + "' has no valid materials");
            }

            FSetTag tag = new FSetTag(tagName.toUpperCase(), materials);
            tags.put(tagName.toUpperCase(), tag);
            FLogger.INFO.log("Loaded building tag '" + tagName + "' with " + materials.size() + " materials");
        }

        FLogger.INFO.log("Loaded " + tags.size() + " building tags");
    }

    /**
     * Resolves a Minecraft/Bukkit tag name to a set of materials.
     * Supports MaterialSetTag, MaterialTags, and Bukkit Tags.
     */
    @Nullable
    private Set<Material> resolveMinecraftTag(String tagName) {
        tagName = tagName.toUpperCase();

        // Try MaterialSetTag (Paper)
        try {
            Field field = MaterialSetTag.class.getDeclaredField(tagName);
            Object value = field.get(null);
            if (value instanceof MaterialSetTag) {
                return new HashSet<>(((MaterialSetTag) value).getValues());
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Not a MaterialSetTag, continue
        }

        // Try MaterialTags (Paper)
        try {
            Field field = MaterialTags.class.getDeclaredField(tagName);
            Object value = field.get(null);
            if (value instanceof Tag) {
                return ((Tag<Material>) value).getValues();
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Not a MaterialTag, continue
        }

        // Try Bukkit Tag
        try {
            Field field = Tag.class.getDeclaredField(tagName);
            Object value = field.get(null);
            if (value instanceof Tag) {
                return ((Tag<Material>) value).getValues();
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Not a Bukkit Tag
        }

        return null;
    }

    /**
     * Creates the default buildingTags.yml configuration with all original tags.
     */
    private void createDefaultConfig() {
        YamlConfiguration config = new YamlConfiguration();

        config.set("tags.WALL.minecraftTags", List.of(
                "STONE_BRICKS", "COBBLESTONES", "LOGS", "SANDSTONES", "RED_SANDSTONES",
                "PLANKS", "WALLS"
        ));
        config.set("tags.WALL.materials", List.of(
                "BRICKS", "STONE", "ANDESITE", "POLISHED_ANDESITE", "DIORITE",
                "POLISHED_DIORITE", "GRANITE", "POLISHED_GRANITE", "SAND", "GRAVEL",
                "CLAY", "DEEPSLATE", "POLISHED_DEEPSLATE", "DEEPSLATE_BRICKS",
                "DEEPSLATE_TILES", "BLACKSTONE", "POLISHED_BLACKSTONE",
                "CHISELED_POLISHED_BLACKSTONE", "GILDED_BLACKSTONE",
                "POLISHED_BLACKSTONE_BRICKS", "CRACKED_DEEPSLATE_BRICKS",
                "CRACKED_DEEPSLATE_TILES", "CRACKED_POLISHED_BLACKSTONE_BRICKS"
        ));

        config.set("tags.WINDOW.minecraftTags", List.of("GLASS", "GLASS_PANES", "WOODEN_FENCES"));

        config.set("tags.WARMTH.minecraftTags", List.of("CAMPFIRES"));
        config.set("tags.WARMTH.materials", List.of("FURNACE", "BLAST_FURNACE", "FIRE"));

        config.set("tags.CRAFTING.materials", List.of(
                "CRAFTING_TABLE", "FLETCHING_TABLE", "STONECUTTER", "FURNACE",
                "BLAST_FURNACE", "CARTOGRAPHY_TABLE", "SMITHING_TABLE",
                "ENCHANTING_TABLE", "LECTERN", "SMOKER", "BREWING_STAND",
                "GRINDSTONE", "LOOM", "BEACON", "ANVIL", "CHIPPED_ANVIL",
                "DAMAGED_ANVIL", "BEEHIVE"
        ));

        config.set("tags.MARKET.minecraftTags", List.of("WOOL", "FENCES"));

        config.set("tags.DOORS.minecraftTags", List.of("DOORS"));

        config.set("tags.CROPS.minecraftTags", List.of("CROPS"));

        config.set("tags.FENCES.minecraftTags", List.of("FENCES", "FENCE_GATES", "WALLS"));

        config.set("tags.FARMING.references", List.of("CROPS", "FENCES"));

        config.set("tags.LIGHT.minecraftTags", List.of("TORCHES", "LANTERNS"));
        config.set("tags.LIGHT.materials", List.of("GLOWSTONE", "REDSTONE_LAMP", "SEA_LANTERN", "END_ROD"));

        config.set("tags.LIGHT_AND_WARMTH.references", List.of("WARMTH", "LIGHT"));

        config.set("tags.FLOWERS.minecraftTags", List.of("FLOWERS"));

        config.set("tags.ROOF.minecraftTags", List.of("STAIRS", "SLABS"));

        config.set("tags.FURNITURE.minecraftTags", List.of(
                "STAIRS", "LANTERNS", "TRAPDOORS", "TORCHES", "SIGNS",
                "FLOWER_POTS", "BANNERS", "WOOL_CARPETS", "BEDS"
        ));
        config.set("tags.FURNITURE.references", List.of("CRAFTING"));
        config.set("tags.FURNITURE.materials", List.of(
                "CHEST", "TRAPPED_CHEST", "BARREL", "DISPENSER", "DROPPER",
                "LADDER", "BELL", "NOTE_BLOCK", "REDSTONE_LAMP", "JUKEBOX", "END_ROD"
        ));

        config.set("tags.WOOD_FARM_STUFF.minecraftTags", List.of("SAPLINGS", "LEAVES"));

        try {
            config.save(configFile);
            FLogger.INFO.log("Created default buildingTags.yml configuration");
        } catch (IOException e) {
            FLogger.ERROR.log("Failed to create default buildingTags.yml: " + e.getMessage());
        }
    }

    /**
     * Gets a building tag by name.
     */
    @Nullable
    public FSetTag getTag(@NotNull String name) {
        return tags.get(name.toUpperCase());
    }

    /**
     * Checks if a tag with the given name exists.
     */
    public boolean isValidTag(@NotNull String name) {
        return tags.containsKey(name.toUpperCase());
    }

    /**
     * Gets all registered building tags.
     */
    @NotNull
    public Map<String, FSetTag> getAllTags() {
        return new HashMap<>(tags);
    }

    /**
     * Reloads the building tags from the configuration file.
     */
    public void reload() {
        load();
    }

    /**
     * Prints all tags and their materials to the log.
     */
    public void printNicely() {
        StringBuilder sb = new StringBuilder();
        for (FSetTag tag : tags.values()) {
            sb.append("Tag ").append(tag.getName()).append(": ").append("\n");
            for (Material material : tag.getMaterials()) {
                sb.append("  - ").append(material.name()).append("\n");
            }
            sb.append("\n");
        }
        Factions.log(sb.toString());
    }
}

