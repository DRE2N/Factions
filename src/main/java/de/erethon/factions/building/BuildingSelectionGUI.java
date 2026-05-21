package de.erethon.factions.building;

import de.erethon.factions.Factions;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.ClaimableRegion;
import de.erethon.factions.region.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.translation.GlobalTranslator;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BuildingSelectionGUI implements InventoryHolder, Listener {

    private static final int PAGE_SIZE = 45;
    private static final int PREVIOUS_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;

    private Inventory inventory;
    private final Map<Integer, Building> buildingSlots = new HashMap<>();
    private final Factions plugin;
    private final FPlayer fPlayer;
    private final Faction faction;
    private List<Building> buildings = List.of();
    private ClaimableRegion region;
    private int page = 0;

    public BuildingSelectionGUI(@NotNull Player player) {
        plugin = Factions.get();
        this.fPlayer = plugin.getFPlayerCache().getByPlayer(player);
        this.faction = fPlayer.getFaction();
        if (!(fPlayer.getCurrentRegion() instanceof ClaimableRegion claimableRegion)) {
            player.sendMessage(FMessage.ERROR_REGION_NOT_FOUND.message());
            return;
        }
        this.region = claimableRegion;
        if (faction == null) {
            player.sendMessage(FMessage.ERROR_REGION_NOT_FOUND.message());
            return;
        }
        this.buildings = plugin.getBuildingManager().getBuildings();

        inventory = Bukkit.createInventory(this, 54, Component.translatable("factions.building.selection"));

        populateInventory();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void populateInventory() {
        inventory.clear();
        buildingSlots.clear();
        int start = page * PAGE_SIZE;
        int end = Math.min(buildings.size(), start + PAGE_SIZE);
        for (int index = start; index < end; index++) {
            int slot = index - start;
            Building building = buildings.get(index);
            ItemStack icon = createBuildingIcon(building);
            inventory.setItem(slot, icon);
            buildingSlots.put(slot, building);
        }
        inventory.setItem(49, createPageInfo());
        if (page > 0) {
            inventory.setItem(PREVIOUS_PAGE_SLOT, createNavigationItem(Material.ARROW, Component.translatable("factions.building.catalog.previous_page")));
        }
        if (end < buildings.size()) {
            inventory.setItem(NEXT_PAGE_SLOT, createNavigationItem(Material.ARROW, Component.translatable("factions.building.catalog.next_page")));
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

        lore.add(Component.translatable("factions.building.catalog.population",
                Component.text(building.getRequiredPopulation().isEmpty() ? "-" : building.getRequiredPopulation().entrySet().stream()
                        .map(entry -> entry.getKey().name().toLowerCase() + " " + entry.getValue())
                        .collect(java.util.stream.Collectors.joining(", ")))));
        if (!building.getUnlockCost().isEmpty()) {
            lore.add(Component.translatable("factions.building.catalog.cost",
                    Component.text(building.getUnlockCost().entrySet().stream()
                            .map(entry -> entry.getKey().getId() + " " + entry.getValue())
                            .collect(java.util.stream.Collectors.joining(", ")))));
        }
        lore.add(Component.empty());

        if (canBuild) {
            lore.add(Component.text("✓ ").color(NamedTextColor.GREEN)
                    .append(Component.translatable("factions.building.requirement.fulfilled")));
        } else {
            lore.add(Component.text("✗ ").color(NamedTextColor.RED)
                    .append(Component.translatable("factions.building.requirement.unfulfilled")));
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

    private ItemStack createNavigationItem(@NotNull Material material, @NotNull Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.color(NamedTextColor.GOLD));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPageInfo() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        int maxPage = Math.max(1, (int) Math.ceil(buildings.size() / (double) PAGE_SIZE));
        meta.displayName(Component.translatable("factions.building.catalog.page",
                Component.text(page + 1), Component.text(maxPage)).color(NamedTextColor.GRAY));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() != this) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory().getHolder() != this) {
            return;
        }
        int slot = event.getSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        if (slot == PREVIOUS_PAGE_SLOT && page > 0) {
            page--;
            populateInventory();
            return;
        }
        if (slot == NEXT_PAGE_SLOT && (page + 1) * PAGE_SIZE < buildings.size()) {
            page++;
            populateInventory();
            return;
        }
        Building building = buildingSlots.get(slot);
        if (building == null) {
            return;
        }
        fPlayer.getPlayer().closeInventory(InventoryCloseEvent.Reason.PLUGIN);
        HandlerList.unregisterAll(this);
        plugin.getServer().getScheduler().runTask(plugin, () ->
                BuildingDialogs.showBuildingDetails(fPlayer.getPlayer(), fPlayer, faction, region, building));

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
        if (inventory == null) {
            return;
        }
        player.openInventory(inventory);
    }
}
