package de.erethon.factions.region.protection;

import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.entity.Relation;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionType;
import de.erethon.factions.region.RegionStructure;
import de.erethon.factions.util.FLogger;
import net.kyori.adventure.util.TriState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;

import java.util.List;

/**
 * @author Fyreum
 */
public class EntityProtectionListener implements Listener {

    final Factions plugin = Factions.get();

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (plugin.getFConfig().isExcludedWorld(event.getDamager().getWorld())) {
            return;
        }
        Player attacker = getDamageSource(event.getDamager());
        if (attacker == null) {
            return;
        }
        Entity damaged = event.getEntity();
        if (!(damaged instanceof Player damagedPlayer)) {
            forbidIfInProtectedTerritory(event, attacker, damaged, FMessage.PROTECTION_CANNOT_ATTACK_FACTION, FMessage.PROTECTION_CANNOT_DESTROY_FACTION);
            return;
        }
        FPlayer fAttacker = plugin.getFPlayerCache().getByPlayer(attacker);
        FPlayer fDefender = plugin.getFPlayerCache().getByPlayer(damagedPlayer);
        Relation relation = fAttacker.getRelation(fDefender);
        if (!relation.canAttack()) {
            event.setCancelled(true);
            fAttacker.sendActionBarMessage(FMessage.PROTECTION_CANNOT_ATTACK_PLAYER.message(fDefender.getDisplayMembership()));
            return;
        }
        if (relation != Relation.ENEMY) {
            return;
        }
        if (!plugin.getWarPhaseManager().getCurrentWarPhase().isAllowPvP()) {
            event.setCancelled(true);
            fAttacker.sendActionBarMessage(FMessage.PROTECTION_CANNOT_ATTACK_IN_CURRENT_PHASE.message(fDefender.getDisplayMembership()));
            return;
        }
        Alliance dAlliance = fDefender.getAlliance();
        Region defenderRegion = fDefender.getLastRegion();
        if (defenderRegion == null || defenderRegion.getAlliance() != dAlliance) {
            return;
        }
        double damageReduction = defenderRegion.getDamageReduction();
        if (damageReduction <= 0) {
            return;
        }
        event.setDamage(Math.max(event.getDamage() * (1 - damageReduction), 0));
        FLogger.DEBUG.log("Reduced damage for '" + fAttacker.getLastName() + "' by " + damageReduction + " at: " + defenderRegion);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTame(EntityTameEvent event) {
        if (event.getOwner() instanceof Player player) {
            forbidIfInProtectedTerritory(event, player, event.getEntity(), FMessage.PROTECTION_CANNOT_TAME_FACTION);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        forbidIfInProtectedTerritory(event, getDamageSource(event.getRemover()), event.getEntity(), FMessage.PROTECTION_CANNOT_ATTACK_FACTION, FMessage.PROTECTION_CANNOT_DESTROY_FACTION);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        forbidIfInProtectedTerritory(event, event.getPlayer(), event.getEntity(), FMessage.PROTECTION_CANNOT_BUILD_FACTION);
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        forbidIfInProtectedTerritory(event, event.getPlayer(), event.getRightClicked(), FMessage.PROTECTION_CANNOT_EQUIP_FACTION);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerShearEntity(PlayerShearEntityEvent event) {
        forbidIfInProtectedTerritory(event, event.getPlayer(), event.getEntity(), FMessage.PROTECTION_CANNOT_SHEAR_FACTION);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerLeashEntity(PlayerLeashEntityEvent event) {
        forbidIfInProtectedTerritory(event, event.getPlayer(), event.getEntity(), FMessage.PROTECTION_CANNOT_LEASH_FACTION);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerUnLeashEntity(PlayerUnleashEntityEvent event) {
        forbidIfInProtectedTerritory(event, event.getPlayer(), event.getEntity(), FMessage.PROTECTION_CANNOT_UNLEASH_FACTION);
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleDamage(VehicleDamageEvent event) {
        forbidIfInProtectedTerritory(event, getDamageSource(event.getAttacker()), event.getVehicle(), FMessage.PROTECTION_CANNOT_ATTACK_FACTION);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        if (event.getAffectedEntities().isEmpty()) {
            return;
        }
        if (!(event.getPotion().getShooter() instanceof Player player)) {
            return;
        }
        forbidIfInProtectedTerritory(event, player, event.getAffectedEntities().iterator().next(), FMessage.PROTECTION_CANNOT_SPLASH_POTION_FACTION);
    }

    @EventHandler(ignoreCancelled = true)
    public void onLingeringPotionSplash(LingeringPotionSplashEvent event) {
        if (event.getAreaEffectCloud().getSource() instanceof Player player) {
            forbidIfInProtectedTerritory(event, player, event.getAreaEffectCloud(), FMessage.PROTECTION_CANNOT_SPLASH_POTION_FACTION);
        }
    }

    private void forbidIfInProtectedTerritory(Cancellable event, Player attacker, Entity target, FMessage forbidMessage) {
        forbidIfInProtectedTerritory(event, attacker, target, forbidMessage, null);
    }

    private void forbidIfInProtectedTerritory(Cancellable event, Player attacker, Entity target, FMessage forbidMessage, FMessage livingForbidMessage) {
        if (attacker == null) {
            return;
        }
        FPlayer fAttacker = plugin.getFPlayerCache().getByPlayer(attacker);
        if (fAttacker.isBypassRaw()) {
            return;
        }
        Region region = plugin.getRegionManager().getRegionByChunk(target.getChunk());
        if (region == null) {
            return;
        }
        List<RegionStructure> structures = region.getStructuresAt(target.getLocation());
        TriState structureState = TriState.NOT_SET;
        // First structure that returns a state other than NOT_SET will be used.
        for (RegionStructure structure : structures) {
            structureState = structure.canAttack(fAttacker, target);
            if (structureState != TriState.NOT_SET) {
                break;
            }
        }
        if (structureState == TriState.TRUE) {
            return;
        }
        boolean living = target instanceof LivingEntity && target.getType() != EntityType.ARMOR_STAND;
        if (structureState == TriState.FALSE) {
            event.setCancelled(true);
            fAttacker.sendActionBarMessage((living && livingForbidMessage != null ? livingForbidMessage : forbidMessage).message(region.getDisplayOwner()));
            return;
        }
        if (region.getType() == RegionType.WAR_ZONE && plugin.getWarPhaseManager().getCurrentWarPhase().isAllowPvP()) {
            return;
        }
        Relation relation = fAttacker.getRelation(region);
        if (living ? relation.canAttack() : region.getType().isAllowsBuilding() && relation.canBuild()) {
            return;
        }
        event.setCancelled(true);
        fAttacker.sendActionBarMessage((living && livingForbidMessage != null ? livingForbidMessage : forbidMessage).message(region.getDisplayOwner()));
    }

    private Player getDamageSource(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        } else if (damager instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player player) {
                return player;
            }
        }
        return null;
    }

}
