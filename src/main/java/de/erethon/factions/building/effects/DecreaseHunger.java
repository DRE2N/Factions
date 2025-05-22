package de.erethon.factions.building.effects;

import de.erethon.factions.Factions;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.player.FPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public class DecreaseHunger extends BuildingEffect implements Listener {

    private final double modifier;

    public DecreaseHunger(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        modifier = data.getDouble("modifier", 0.5d);
    }

    @Override
    public void apply() {
        Bukkit.getPluginManager().registerEvents(this, Factions.get());
    }

    @Override
    public void remove() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onFoodLevelChanger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        FPlayer fPlayer = Factions.get().getFPlayerCache().getByPlayer(player);
        if (fPlayer.getFaction() != site.getRegion().getFaction() || fPlayer.getCurrentRegion() != site.getRegion()) {
            return;
        }
        int from = player.getFoodLevel();
        int to = event.getFoodLevel();
        if (to > from) {
            return;
        }
        // If the food level changed more than 1, decrease it by the modifier
        // Else cancel the event with a chance of the modifier.
        int diff = from - to;
        if (diff >= 2) {
            event.setFoodLevel(from - (int) (diff * modifier));
            return;
        }
        if (Math.random() < modifier) {
            event.setCancelled(true);
        }
    }
}
