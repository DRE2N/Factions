package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.util.FLogger;
import de.erethon.hephaestus.Hephaestus;
import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemLibrary;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AdditionalEntityDrops extends BuildingEffect {

    private final HItemLibrary itemLibrary = Hephaestus.INSTANCE.getLibrary();
    private final HashMap<Map.Entry<HItem, Integer>, Integer> drops = new HashMap<>();
    private boolean friendlyOnly = false;
    private final Random random = new Random();

    public AdditionalEntityDrops(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        for (String drop : data.getStringList("drops")) {
            String[] split = drop.split(";");
            HItem item = itemLibrary.get(ResourceLocation.tryParse(split[0]));
            if (item == null) {
                FLogger.ERROR.log("Item " + split[0] + " not found in item library for AdditionalEntityDrops effect in building " + site.getBuilding().getId());
                continue;
            }
            int amount = Integer.parseInt(split[1]);
            int chance = Integer.parseInt(split[2]);
            drops.put(Map.entry(item, amount), chance);
        }
        friendlyOnly = data.getBoolean("friendlyOnly", false);
    }

    @Override
    public void onEntityKill(FPlayer player, EntityDeathEvent event) {
        if (friendlyOnly && player.getFaction() != null && !player.getFaction().equals(faction)) {
            return;
        }
        double chance = random.nextDouble();
        for (Map.Entry<HItem, Integer> entry : drops.keySet()) {
            HItem item = entry.getKey();
            int amount = entry.getValue();
            if (chance < (double) drops.get(entry) / 100) {
                ItemStack drop = item.rollRandomStack(amount).getBukkitStack();
                event.getDrops().add(drop);
            }
        }
    }
}
