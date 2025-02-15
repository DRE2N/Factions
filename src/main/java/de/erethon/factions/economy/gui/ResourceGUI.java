package de.erethon.factions.economy.gui;

import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.economy.resource.ResourceCategory;
import de.erethon.factions.faction.Faction;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ResourceGUI extends EconomyGUI {

    public ResourceGUI(Player player, Faction faction) {
        super(player, faction);
    }

    @Override
    protected void initializeItems() {
        int row = 1;
        for (ResourceCategory category : ResourceCategory.values()) {
            int slot = row * 9 + 1;

            // Create category label
            ItemStack categoryLabel = createGuiItem(Material.BOOK,
                    Component.text(category.getName()),
                    Component.translatable("factions.gui.economy.resource.category.description"));
            inventory.setItem(slot++, categoryLabel);

            // Add resources for this category
            for (Resource resource : category.getResources()) {
                ItemStack item = createResourceItem(resource);
                if (slot % 9 == 8) {
                    // Skip to next row if we're at the end
                    row++;
                    slot = row * 9 + 1;
                }
                inventory.setItem(slot++, item);
            }
            row++;
        }

        // Add back button
        ItemStack back = createGuiItem(Material.BARRIER,
                Component.translatable("factions.gui.back"));
        inventory.setItem(inventory.getSize() - 1, back);
    }

    private ItemStack createResourceItem(Resource resource) {
        Material material = getMaterialForResource(resource);
        int amount = faction.getStorage().getResource(resource);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.translatable("factions.gui.economy.resource.amount", Component.text(String.valueOf(amount))));

        ItemStack item = createGuiItem(material, resource.displayName(), lore.toArray(new Component[0]));
        return item;
    }

    // Move this to custom models eventually
    private Material getMaterialForResource(Resource resource) {
        return switch (resource) {
            case GRAIN -> Material.WHEAT;
            case FRUIT -> Material.APPLE;
            case VEGETABLES -> Material.CARROT;
            case FISH -> Material.COD;
            case COWS -> Material.BEEF;
            case PIGS -> Material.PORKCHOP;
            case SHEEP -> Material.MUTTON;
            case CHICKENS -> Material.CHICKEN;
            case BREAD -> Material.BREAD;
            case MEAT -> Material.COOKED_BEEF;
            case CHEESE -> Material.MILK_BUCKET;
            case BEER -> Material.POTION;
            case WINE -> Material.HONEY_BOTTLE;
            case IRON -> Material.IRON_INGOT;
            case GOLD -> Material.GOLD_INGOT;
            case COAL -> Material.COAL;
            case STONE -> Material.STONE;
            case WOOD -> Material.OAK_LOG;
            case TOOLS -> Material.IRON_AXE;
            case CLOTH -> Material.WHITE_WOOL;
            case SILK -> Material.STRING;
            case SPICES -> Material.BROWN_DYE;
            case SALT -> Material.SUGAR;
            case JEWELRY -> Material.DIAMOND;
            case FURNITURE -> Material.CRAFTING_TABLE;
            case BRICKS -> Material.BRICKS;
            case GLASS -> Material.GLASS;
            case BOOKS -> Material.BOOK;
            case PAPER -> Material.PAPER;
            case INK -> Material.INK_SAC;
            case CANDLES -> Material.CANDLE;
            case MITHRIL -> Material.NETHERITE_INGOT;
            case HORSES -> Material.SADDLE;
            case SIEGE_EQUIPMENT -> Material.DISPENSER;
            case WEAPONS -> Material.IRON_SWORD;
            case ARMOR -> Material.IRON_CHESTPLATE;
        };
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getSlot() == inventory.getSize() - 1) {
            new EconomyGUI(player, faction).open();
        }
    }
}