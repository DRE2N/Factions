package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import net.minecraft.server.Main;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class ItemConversion extends BuildingEffect {

    private final int buildingTicksPeritem;
    private int ticks = 0;
    //HashMap<HItem, HItem> conversion = new HashMap<>();

    public ItemConversion(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        buildingTicksPeritem = data.getInt("buildingTicksPeritem", 20);
    }

    @Override
    public void tick() {
        if (ticks++ < buildingTicksPeritem) {
            return;
        }
        ticks = 0;
        for (ItemStack stack : site.getInputItems()) {
            /*HItem item = Main.itemLibrary.get(stack);
            if (item == null) {
                continue;
            }
            HItem result = conversion.get(item);
            if (result == null) {
                continue;
            }
            stack.setAmount(stack.getAmount() - 1);
            HItem hItem = Main.itemLibrary.get(result.getId());
            if (hItem == null) {
                continue;
            }
            site.getOutputItems().add(hItem.getItem().getBukkitStack());*/
        }
    }


}
