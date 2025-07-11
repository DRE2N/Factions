package de.erethon.factions.building;

import com.destroystokyo.paper.MaterialSetTag;
import com.destroystokyo.paper.MaterialTags;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Malfrador
 */
public enum FSetTag {

    WALL(MaterialSetTag.STONE_BRICKS.getValues(), MaterialTags.COBBLESTONES.getValues(), MaterialSetTag.LOGS.getValues(), MaterialTags.SANDSTONES.getValues(), MaterialTags.RED_SANDSTONES.getValues(), MaterialSetTag.PLANKS.getValues(),
            new HashSet<>(Arrays.asList(Material.BRICKS, Material.STONE, Material.ANDESITE, Material.POLISHED_ANDESITE, Material.DIORITE, Material.POLISHED_DIORITE, Material.GRANITE, Material.POLISHED_GRANITE))),
    WINDOW(MaterialTags.GLASS.getValues(), MaterialTags.GLASS_PANES.getValues()),
    WARMTH(MaterialSetTag.CAMPFIRES.getValues(), new HashSet<>(Arrays.asList(Material.FURNACE, Material.BLAST_FURNACE, Material.FIRE))),
    CRAFTING(new HashSet<>(Arrays.asList(Material.CRAFTING_TABLE, Material.FLETCHING_TABLE, Material.STONECUTTER, Material.FURNACE, Material.BLAST_FURNACE, Material.CARTOGRAPHY_TABLE, Material.SMITHING_TABLE, Material.ENCHANTING_TABLE, Material.LECTERN, Material.SMOKER,
            Material.BREWING_STAND, Material.GRINDSTONE, Material.LOOM, Material.BEACON, Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL))),
    MARKET(MaterialSetTag.WOOL.getValues(), MaterialSetTag.FENCES.getValues()),
    DOORS(MaterialSetTag.DOORS.getValues()),
    CROPS(MaterialSetTag.CROPS.getValues()),
    FENCES(MaterialSetTag.FENCES.getValues(),MaterialSetTag.FENCE_GATES.getValues(), MaterialSetTag.WALLS.getValues()),
    FARMING(CROPS.getMaterials(), FENCES.getMaterials()),
    LIGHT(MaterialTags.TORCHES.getValues(), MaterialTags.LANTERNS.getValues(), new HashSet<>(Arrays.asList(Material.GLOWSTONE, Material.REDSTONE_LAMP, Material.SEA_LANTERN, Material.END_ROD))),
    LIGHT_AND_WARMTH(WARMTH.getMaterials(), LIGHT.getMaterials()),
    FLOWERS(MaterialSetTag.FLOWERS.getValues()),
    CHEAP_WINDOW(MaterialSetTag.WOODEN_FENCES.getValues()),
    ROOF(MaterialSetTag.STAIRS.getValues(), MaterialSetTag.SLABS.getValues()),
    FURNITURE(MaterialSetTag.STAIRS.getValues(), MaterialTags.LANTERNS.getValues(), MaterialTags.TRAPDOORS.getValues(), MaterialTags.TORCHES.getValues(), MaterialSetTag.SIGNS.getValues(), MaterialSetTag.FLOWER_POTS.getValues(), MaterialSetTag.BANNERS.getValues(),
            MaterialSetTag.WOOL_CARPETS.getValues(), MaterialSetTag.BEDS.getValues(), CRAFTING.getMaterials(), new HashSet<>(Arrays.asList(Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL,Material.DISPENSER, Material.DROPPER,  Material.LADDER,
            Material.BELL, Material.NOTE_BLOCK, Material.REDSTONE_LAMP, Material.JUKEBOX, Material.END_ROD))),
    WOOD_FARM_STUFF(MaterialSetTag.SAPLINGS.getValues(), MaterialSetTag.LEAVES.getValues());

    final Set<Material> materialSetTags = new HashSet<>();

    @SafeVarargs // Shouldn't be able to cause heap pollution
    FSetTag(Set<Material>... tags) {
        for (Set<Material> set : tags) {
            materialSetTags.addAll(set);
        }
    }

    public Set<Material> getMaterials() {
        return materialSetTags;
    }

    public boolean hasBlock(Material block) {
        return materialSetTags.contains(block);
    }
}