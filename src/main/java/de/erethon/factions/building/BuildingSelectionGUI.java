package de.erethon.factions.building;

import de.erethon.factions.Factions;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Bukkit;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BuildingSelectionGUI implements InventoryHolder, Listener {

    private Inventory inventory;
    private final Map<Integer, Building> buildingSlots = new HashMap<>();
    private final FPlayer fPlayer;
    private final Faction faction;
    private final Region region;

    public BuildingSelectionGUI(@NotNull Player player) {
        Factions plugin = Factions.get();
        this.fPlayer = plugin.getFPlayerCache().getByPlayer(player);
        this.faction = fPlayer.getFaction();
        this.region = fPlayer.getLastRegion();
        if (region == null || faction == null) {
            player.sendMessage(FMessage.ERROR_REGION_NOT_FOUND.message());
            return;
        }

        inventory = Bukkit.createInventory(this, 54, Component.translatable("factions.building.selection"));

        List<Building> availableBuildings = BuildingManager.getUnlockedBuildingsForPlacement(fPlayer, faction, region);
        populateInventory(availableBuildings);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void populateInventory(List<Building> buildings) {
        int slot = 0;
        for (Building building : buildings) {
            ItemStack icon = createBuildingIcon(building);
            inventory.setItem(slot, icon);
            buildingSlots.put(slot, building);
            slot++;
        }
    }

    private ItemStack createBuildingIcon(Building building) {
        ItemStack icon = new ItemStack(building.getIcon());
        ItemMeta meta = icon.getItemMeta();
        Component name = Component.translatable("factions.building.buildings." + building.getId() + ".name");

        Set<RequirementFail> fails = building.checkRequirements(fPlayer.getPlayer(), faction, fPlayer.getPlayer().getLocation());
        boolean canBuild = fails.isEmpty();
        name = name.color(canBuild ? NamedTextColor.GREEN : NamedTextColor.RED);
        meta.displayName(name);

        List<Component> lore = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            String key = "factions.building.buildings." + building.getId() + ".description." + i;
            Component line = GlobalTranslator.translator().translate(
                    Component.translatable(key),
                    fPlayer.getPlayer().locale()
            );

            if (line == null || line.equals(Component.empty())) {
                break;
            }
            lore.add(line);
        }

        lore.add(Component.empty());

        if (canBuild) {
            lore.add(Component.text("✓ ").color(NamedTextColor.GREEN)
                    .append(Component.translatable("factions.building.requirements.fulfilled")));
        } else {
            lore.add(Component.text("✗ ").color(NamedTextColor.RED)
                    .append(Component.translatable("factions.building.requirements.unfulfilled")));
            // Add each failed requirement
            for (RequirementFail fail : fails) {
                lore.add(Component.text("  • ").color(NamedTextColor.RED)
                        .append(Component.translatable(fail.getTranslationKey())));
            }
        }

        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        Building building = buildingSlots.get(slot);
        if (building == null) {
            return;
        }
        if (!building.checkRequirements(fPlayer.getPlayer(), faction, fPlayer.getPlayer().getLocation()).isEmpty()) {
            fPlayer.sendMessage(Component.translatable("factions.building.requirements.unfulfilled"));
            return;
        }
        new BuildSitePlacer(building, fPlayer, region, faction);
        fPlayer.getPlayer().closeInventory(InventoryCloseEvent.Reason.PLUGIN);
        HandlerList.unregisterAll(this);

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

    public void open(@NotNull Player player) {
        player.openInventory(inventory);
    }
}