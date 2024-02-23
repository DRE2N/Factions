package de.erethon.factions.war;

import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.entity.Relation;
import de.erethon.factions.event.FPlayerCrossRegionEvent;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.war.objective.CrystalWarObjective;
import de.erethon.factions.war.objective.WarObjective;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * @author Fyreum
 */
public class WarListener implements Listener {

    final Factions plugin = Factions.get();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        WarPhase currentWarPhase = plugin.getWarPhaseManager().getCurrentWarPhase();
        if (!currentWarPhase.isAllowPvP()) {
            return;
        }
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(event.getPlayer());
        Region region = fPlayer.getLastRegion();
        if (region == null) {
            return;
        }
        for (WarObjective objective : region.getStructures(WarObjective.class).values()) {
            boolean inRange = objective.containsPosition(event.getTo());
            if (objective.isActive(fPlayer)) {
                if (inRange) {
                    continue;
                }
                if (isSpectator(fPlayer)) {
                    objective.onSpectatorExit(fPlayer);
                } else {
                    objective.onExit(fPlayer);
                }
            } else if (inRange) {
                if (isSpectator(fPlayer)) {
                    objective.onSpectatorEnter(fPlayer);
                } else {
                    objective.onEnter(fPlayer);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCrossRegion(FPlayerCrossRegionEvent event) {
        FPlayer fPlayer = event.getFPlayer();
        if (!fPlayer.hasActiveWarObjectives()) {
            return;
        }
        // Remove previous objectives
        for (WarObjective objective : fPlayer.getActiveWarObjectives()) {
            objective.onExit(fPlayer);
        }
        fPlayer.getActiveWarObjectives().clear();
    }

    private boolean isSpectator(FPlayer fPlayer) {
        return fPlayer.getAlliance() == null || fPlayer.getPlayer().getGameMode() != GameMode.SURVIVAL;
    }

    @EventHandler
    public void onKill(PlayerDeathEvent event) {
        ServerPlayer sKilled = ((CraftPlayer) event.getPlayer()).getHandle();
        LivingEntity sKiller = sKilled.getKillCredit();
        if (sKiller == null || !(sKiller.getBukkitLivingEntity() instanceof Player killer)) {
            return;
        }
        FPlayer fKilled = plugin.getFPlayerCache().getByPlayer(event.getPlayer());
        FPlayer fKiller = plugin.getFPlayerCache().getByPlayer(killer);
        if (fKilled.getLastRegion() != null && fKilled.getLastRegion().getType().isWarGround() && fKilled.getRelation(fKiller) != Relation.ENEMY) {
            return;
        }
        // Update stats for the killed player
        WarStats kdStats = fKilled.getWarStats();
        kdStats.deaths++;
        kdStats.killStreak = 0;
        // Update stats for the killer
        WarStats krStats = fKiller.getWarStats();
        krStats.kills++;
        if (++krStats.killStreak > krStats.highestKillStreak) {
            krStats.highestKillStreak = krStats.killStreak;
        }
        // Update stats for assisting players
        List<CombatEntry> entries = sKilled.getCombatTracker().entries;
        if (entries.size() > 1) {
            for (int i = 0; i < entries.size() - 1; i++) {
                Entity attacker = entries.get(i).source().getDirectEntity();
                if (attacker == null || !(attacker.getBukkitEntity() instanceof Player player)) {
                    continue;
                }
                FPlayer fAssist = plugin.getFPlayerCache().getByPlayer(player);
                fAssist.getWarStats().assists++;
            }
        }
        // Update war score if necessary
        if (!plugin.getWarPhaseManager().getCurrentWarPhase().isInfluencingScoring()) {
            return;
        }
        Alliance krAlliance = fKiller.getAlliance();
        if (krAlliance == null) {
            return;
        }
        Region region = fKilled.getLastRegion();
        if (region == null || region.getType().isWarGround()) {
            return;
        }
        region.getRegionalWarTracker().addKill(krAlliance);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCrystalDamage(EntityDamageByEntityEvent event) {
        CrystalWarObjective crystal = getCrystalObjective(event.getEntity());
        if (crystal == null) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(player);
        Alliance cAlliance = crystal.getAlliance();
        Alliance pAlliance = fPlayer.getAlliance();
        if (pAlliance == null || cAlliance == pAlliance) {
            fPlayer.sendMessage(FMessage.PROTECTION_CANNOT_ATTACK_WAR_OBJECTIVE.message(cAlliance.getDisplayShortName()));
            return;
        }
        crystal.damage(event.getDamage(), fPlayer);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCrystalDamage(EntityDamageByBlockEvent event) {
        CrystalWarObjective crystal = getCrystalObjective(event.getEntity());
        if (crystal == null) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCrystalInteract(PlayerInteractEntityEvent event) {
        CrystalWarObjective crystal = getCrystalObjective(event.getRightClicked());
        if (crystal == null) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getHand());
        if (item.getType() != Material.NETHER_STAR) {
            return;
        }
        item.setAmount(item.getAmount() - 1);
        player.getInventory().setItem(event.getHand(), item.getAmount() == 0 ? null : item);
        crystal.addEnergy(20); // todo: Make energy configurable
    }

    @EventHandler
    public void onCrystalDrop(EntityDropItemEvent event) {
        CrystalWarObjective crystal = getCrystalObjective(event.getEntity());
        if (crystal == null) {
            return;
        }
        event.setCancelled(true);
    }

    private CrystalWarObjective getCrystalObjective(org.bukkit.entity.Entity entity) {
        Region region = plugin.getRegionManager().getRegionByLocation(entity.getLocation());
        if (region == null) {
            return null;
        }
        String name = entity.getPersistentDataContainer().get(WarObjective.NAME_KEY, PersistentDataType.STRING);
        return name == null ? null : region.getStructure(name, CrystalWarObjective.class);
    }
}
