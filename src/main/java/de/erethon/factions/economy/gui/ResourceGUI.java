package de.erethon.factions.economy.gui;

import de.erethon.factions.Factions;
import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.economy.resource.ResourceCategory;
import de.erethon.factions.faction.Faction;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ResourceGUI extends EconomyGUI {

    private boolean showingCategories = true;
    private ResourceCategory currentCategory;

    public ResourceGUI(Player player, Faction faction) {
        this.player = player;
        this.faction = faction;
        this.inventory = Bukkit.createInventory(this, 27, Component.translatable("factions.gui.economy.resource.title"));
        Bukkit.getPluginManager().registerEvents(this, Factions.get());
        initializeItems();
    }

    @Override
    protected void initializeItems() {
        if (showingCategories) {
            showCategories();
        } else {
            showResources(currentCategory);
        }
    }

    private void showCategories() {
        inventory.clear();
        int slot = 11;
        for (ResourceCategory category : ResourceCategory.values()) {
            ItemStack categoryLabel = createGuiItem(Material.BOOK,
                    Component.text(category.getName()),
                    Component.translatable("factions.gui.economy.resource.category.description"));
            inventory.setItem(slot++, categoryLabel);
        }
        ItemStack back = createGuiItem(Material.ARROW, Component.translatable("factions.gui.back"));
        inventory.setItem(inventory.getSize() - 9, back);
    }

    private void showResources(ResourceCategory category) {
        inventory.clear();
        int slot = 0;
        ItemStack categoryLabel = createGuiItem(Material.BOOK,
                Component.text(category.getName()),
                Component.translatable("factions.gui.economy.resource.category.description"));
        inventory.setItem(slot++, categoryLabel);

        for (Resource resource : category.getResources()) {
            ItemStack item = createResourceItem(resource);
            inventory.setItem(slot++, item);
        }

        ItemStack back = createGuiItem(Material.ARROW, Component.translatable("factions.gui.back"));
        inventory.setItem(inventory.getSize() - 9, back);
    }

    private ItemStack createResourceItem(Resource resource) {
        Material material = getMaterialForResource(resource);
        int amount = faction.getStorage().getResource(resource);
        int limit = faction.getStorage().getResourceLimit(resource);
        double lastProduction = faction.getEconomy().getLastProduction(resource);
        double lastTotalConsumption = faction.getEconomy().getLastTotalConsumption(resource);

        List<Component> lore = new ArrayList<>();
        Component storage = Component.translatable("factions.gui.economy.resource.amount", Component.text(amount), Component.text(limit));
        Component production = Component.translatable("factions.gui.economy.resource.production", Component.text(lastProduction));
        Component consumption = Component.translatable("factions.gui.economy.resource.totalConsumption", Component.text(lastTotalConsumption));
        lore.add(storage);
        lore.add(Component.empty());
        lore.add(production);
        lore.add(consumption);

        return createGuiItem(material, resource.displayName(), lore.toArray(new Component[0]));
    }

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

    @EventHandler
    private void handleClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        if (showingCategories) {
            if (slot < ResourceCategory.values().length + 11) {
                currentCategory = ResourceCategory.values()[slot - 11];
                showingCategories = false;
                initializeItems();
            } else if (slot == inventory.getSize() - 9) {
                new EconomyGUI(player, faction).open();
            }
        } else {
            if (slot == inventory.getSize() - 9) {
                showingCategories = true;
                initializeItems();
            }
        }
    }
}