package de.erethon.factions.building.effects;

import de.erethon.factions.Factions;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.event.FPlayerCrossRegionEvent;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.jetbrains.annotations.NotNull;

public class SpeedBoost extends BuildingEffect implements Listener {

    private final AttributeModifier modifier;

    public SpeedBoost(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        modifier = new AttributeModifier(NamespacedKey.fromString("factions:building-speedbuff"), data.getDouble("amount", 0.1), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
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
    private void onRegionCross(FPlayerCrossRegionEvent event) {
        Region newRegion = event.getNewRegion();
        Region oldRegion = event.getOldRegion();
        if (oldRegion != site.getRegion() || newRegion != site.getRegion()) {
            return;
        }
        if (newRegion == site.getRegion() && event.getFPlayer().getFaction() == site.getRegion().getFaction()) {
            event.getFPlayer().getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addTransientModifier(modifier);
        } else {
            event.getFPlayer().getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
        }
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        FPlayer fPlayer = Factions.get().getFPlayerCache().getByPlayer(event.getPlayer());
        Region regionAtLogin = Factions.get().getRegionManager().getRegionByLocation(event.getPlayer().getLocation());
        if (regionAtLogin == site.getRegion() && fPlayer.getFaction() == site.getRegion().getFaction()) {
            fPlayer.getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addTransientModifier(modifier);
        }
    }



}
