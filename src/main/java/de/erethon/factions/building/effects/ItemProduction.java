package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.util.FLogger;
import de.erethon.hephaestus.HItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ItemProduction extends BuildingEffect {

    private final Map<HItem, Integer> production = new HashMap<>();
    private final int interval; // In seconds
    private int ticks = 0;

    public ItemProduction(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        for (String entry : data.getConfigurationSection("production").getKeys(false)) {
            ConfigurationSection section = data.getConfigurationSection("production." + entry);
            ResourceLocation id = ResourceLocation.tryParse(section.getString("id"));
            if (id == null) {
                FLogger.ERROR.log("Invalid item id in production effect for building " + site.getBuilding().getName() + ": " + section.getString("id"));
                continue;
            }
            HItem item = Main.itemLibrary.get(id);
            if (item == null) {
                FLogger.ERROR.log("Item not found in production effect for building " + site.getBuilding().getName() + ": " + section.getString("id"));
                continue;
            }
            production.put(item, section.getInt("amount"));
        }
        interval = data.getInt("interval", 60);
    }

    @Override
    public void tick() {
        ticks++;
        if (ticks >= interval * 20) {
            ticks = 0;
            produce();
        }
    }

    private void produce() {
        Inventory storage = site.getInventory();
        for (Map.Entry<HItem, Integer> entry : production.entrySet()) {
            storage.addItem(entry.getKey().getItem(entry.getValue()).getBukkitStack());
        }
    }
}
