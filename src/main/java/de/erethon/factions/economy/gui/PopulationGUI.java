package de.erethon.factions.economy.gui;

import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.faction.Faction;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class PopulationGUI extends EconomyGUI {

    public PopulationGUI(Player player, Faction faction) {
        super(player, faction);
        initializeItems();
    }

    protected void initializeItems() {
        int slot = 10;
        for (PopulationLevel level : PopulationLevel.values()) {
            double happiness = faction.getHappiness(level);
            int population = faction.getPopulation(level);

            ItemStack item = createGuiItem(Material.PLAYER_HEAD,
                    level.displayName(),
                    Component.translatable("factions.gui.population.count", Component.text(population)),
                    Component.translatable("factions.gui.population.happiness", Component.text(String.format("%.2f", happiness))),
                    Component.empty(),
                    Component.translatable("factions.gui.population.housing",
                            Component.text(faction.getAttributeValue("housing_" + level.name().toLowerCase(), 0)))
            );

            inventory.setItem(slot++, item);
        }

        // Back button
        inventory.setItem(26, createGuiItem(Material.ARROW,
                Component.translatable("factions.gui.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getSlot() == 26) {
            new EconomyGUI(player, faction).open();
        }
    }
}