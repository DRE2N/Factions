package de.erethon.factions.war;

import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.entity.Relation;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionType;
import de.erethon.factions.war.objective.CrystalWarObjective;
import de.erethon.factions.war.objective.WarObjective;
import de.erethon.factions.war.objective.WarObjectiveManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * @author Fyreum
 */
public class WarListener implements Listener {

    final Factions plugin = Factions.get();

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        WarPhase currentWarPhase = plugin.getWarPhaseManager().getCurrentWarPhase();
        if (!currentWarPhase.isAllowPvP()) {
            return;
        }
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(event.getPlayer());
        for (WarObjective objective : plugin.getWarObjectiveManager().getObjectives().values()) {
            boolean inRange = event.getTo().distance(objective.getLocation()) <= objective.getRadius();
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

    private boolean isSpectator(FPlayer fPlayer) {
        return fPlayer.getAlliance() == null || fPlayer.getPlayer().getGameMode() != GameMode.SURVIVAL;
    }

    @EventHandler
    public void onKill(PlayerDeathEvent event) {
        ServerPlayer sKilled = ((CraftPlayer) event.getPlayer()).getHandle();
        LivingEntity sKiller = sKilled.getCombatTracker().getKiller();
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
        // Update war score
        Alliance krAlliance = fKiller.getAlliance();
        if (krAlliance == null) {
            return;
        }
        Region region = fKilled.getLastRegion();
        if (region == null || region.getType().isWarGround()) {
            return;
        }
        krAlliance.getWarScores().getOrCreate(region).addKill();
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        WarObjectiveManager objectiveManager = plugin.getWarObjectiveManager();
        WarObjective objective = objectiveManager.getByEntity(event.getEntity());
        if (!(objective instanceof CrystalWarObjective crystal)) {
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
    public void onEntityDamage(EntityDamageByBlockEvent event) {
        WarObjectiveManager objectiveManager = plugin.getWarObjectiveManager();
        WarObjective objective = objectiveManager.getByEntity(event.getEntity());
        if (!(objective instanceof CrystalWarObjective)) {
            return;
        }
        event.setCancelled(true);
    }
}
