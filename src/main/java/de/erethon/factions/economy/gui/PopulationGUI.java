package de.erethon.factions.economy.gui;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.faction.Faction;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PopulationGUI extends EconomyGUI {

    public PopulationGUI(Player player, Faction faction) {
        this.player = player;
        this.faction = faction;
        this.inventory = Bukkit.createInventory(this, 27, Component.translatable("factions.gui.economy.population.title"));
        Bukkit.getPluginManager().registerEvents(this, Factions.get());
        initializeItems();
    }

    protected void initializeItems() {
        int slot = 10;
        for (PopulationLevel level : PopulationLevel.values()) {
            double happiness = faction.getHappiness(level);
            int population = faction.getPopulation(level);
            Map<PopulationLevel, Map<Resource, Double>> resources = faction.getEconomy().getResourceSatisfaction();
            Set<Resource> unhappyResources = new HashSet<>();
            Set<Resource> happyResources = new HashSet<>();
            for (Map.Entry<Resource, Double> entry : resources.get(level).entrySet()) {
                if (entry.getValue() < 0.2) {
                    unhappyResources.add(entry.getKey());
                } else {
                    happyResources.add(entry.getKey());
                }
            }

            Component missing = Component.empty();
            for (Resource resource : unhappyResources) {
                missing = missing.append(resource.displayName()).append(Component.text(", "));
            }
            Component happy = Component.empty();
            for (Resource resource : happyResources) {
                happy = happy.append(resource.displayName()).append(Component.text(", "));
            }

            ItemStack item = createGuiItem(Material.PLAYER_HEAD,
                    level.displayName(),
                    Component.translatable("factions.gui.population.count", Component.text(population)),
                    Component.translatable("factions.gui.population.happiness", Component.text(String.format("%.2f", happiness))),
                    Component.empty(),
                    Component.translatable("factions.gui.population.housing", Component.text(faction.getAttributeValue("housing_" + level.name().toLowerCase(), 0))),
                    Component.empty(),
                    Component.translatable("factions.economy.population.resources.satisfied", happy),
                    Component.translatable("factions.economy.population.resources.dissatisfied", missing)
            );

            inventory.setItem(slot++, item);
        }

        // Back button
        inventory.setItem(26, createGuiItem(Material.ARROW,
                Component.translatable("factions.gui.back")));
    }

    @EventHandler
    private void handleClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        event.setCancelled(true);
        if (event.getSlot() == 26) {
            new EconomyGUI(player, faction).open();
        }
    }
}