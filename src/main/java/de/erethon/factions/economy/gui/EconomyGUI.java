package de.erethon.factions.economy.gui;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.economy.report.EconomyCycleReport;
import de.erethon.factions.economy.report.ResourceFlowReport;
import de.erethon.factions.faction.Faction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EconomyGUI implements InventoryHolder, Listener {

    protected Faction faction;
    protected Player player;
    protected Inventory inventory;


    public EconomyGUI(){
    }

    public EconomyGUI(Player player, Faction faction) {
        this.player = player;
        this.faction = faction;
        this.inventory = Bukkit.createInventory(this, 27, Component.translatable("factions.gui.economy.title"));
        Bukkit.getPluginManager().registerEvents(this, Factions.get());
        initializeItems();
    }

    protected void initializeItems() {
        EconomyCycleReport report = faction.getEconomy().getLastReport();
        int totalPopulation = 0;
        for (PopulationLevel level : PopulationLevel.values()) {
            totalPopulation += faction.getPopulation(level);
        }
        long fullResources = report.resources().values().stream().filter(ResourceFlowReport::storageFull).count();
        // Main menu items
        ItemStack population = createGuiItem(Material.VILLAGER_SPAWN_EGG,
                Component.translatable("factions.gui.economy.population.title"),
                Component.translatable("factions.gui.economy.population.description"),
                Component.translatable("factions.gui.economy.population.total", Component.text(totalPopulation)),
                Component.translatable("factions.gui.economy.population.unrest", Component.text(String.format("%.1f", faction.getUnrestLevel()))));

        ItemStack resources = createGuiItem(Material.CHEST,
                Component.translatable("factions.gui.economy.resources.title"),
                Component.translatable("factions.gui.economy.resources.description"),
                Component.translatable("factions.gui.economy.resources.full", Component.text(fullResources)));

        ItemStack unlocks = createGuiItem(Material.KNOWLEDGE_BOOK,
                Component.translatable("factions.gui.economy.unlocks.title"),
                Component.translatable("factions.gui.economy.unlocks.description"));

        ItemStack warnings = createGuiItem(fullResources > 0 ? Material.REDSTONE_TORCH : Material.LANTERN,
                Component.translatable("factions.gui.economy.warnings.title"),
                fullResources > 0
                        ? Component.translatable("factions.gui.economy.warnings.storage_full", Component.text(fullResources)).color(NamedTextColor.RED)
                        : Component.translatable("factions.gui.economy.warnings.none"));

        inventory.setItem(11, population);
        inventory.setItem(13, warnings);
        inventory.setItem(15, resources);
        inventory.setItem(22, unlocks);
    }

    protected ItemStack createGuiItem(Material material, Component name, Component... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        List<Component> loreList = new ArrayList<>();
        for (Component component : lore) {
            loreList.add(component.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        meta.lore(loreList);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    private void handleClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        event.setCancelled(true);
        if (event.getSlot() == 11) {
            new PopulationGUI(player, faction).open();
        } else if (event.getSlot() == 22) {
            EconomyDialogs.showUnlocks(player, faction);
        } else if (event.getSlot() == 15) {
            new ResourceGUI(player, faction).open();
        }
    }

    public void open() {
        if (faction == null) {
            MessageUtil.sendMessage(player, "<red>Internal error. Faction  not found.");
            return;
        }
        if (faction.getAlliance() == null) {
            MessageUtil.sendMessage(player, "<red>Internal error. Alliance not found.");
            return;
        }
        player.openInventory(inventory);
    }


    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        HandlerList.unregisterAll(this);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
