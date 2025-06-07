package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.hephaestus.Hephaestus;
import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemLibrary;
import de.erethon.hephaestus.items.HItemStack;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class ItemConversion extends BuildingEffect {

    private final HItemLibrary itemLibrary = Hephaestus.INSTANCE.getLibrary();
    private final int buildingTicksPerItem;
    private int ticks = 0;
    HashMap<HItem, HItem> conversion = new HashMap<>();

    public ItemConversion(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        site.setRequiresOutputChest(true);
        site.setRequiresInputChest(true);
        buildingTicksPerItem = data.getInt("buildingTicksPerItem", 20);
    }

    @Override
    public void tick() {
        if (ticks++ < buildingTicksPerItem) {
            return;
        }
        ticks = 0;
        for (ItemStack stack : site.getInputItems()) {
            HItemStack item = itemLibrary.get(stack);
            if (item == null) {
                continue;
            }
            HItem result = conversion.get(item.getItem());
            if (result == null) {
                continue;
            }
            stack.setAmount(stack.getAmount() - 1);
            HItem hItem = itemLibrary.get(result.getKey());
            if (hItem == null) {
                continue;
            }
            site.getOutputItems().add(hItem.rollRandomStack().getBukkitStack());
        }
    }


}
