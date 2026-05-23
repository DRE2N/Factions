package de.erethon.factions.war;

import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.entity.Relation;
import de.erethon.factions.event.FPlayerCrossRegionEvent;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.WarRegion;
import de.erethon.factions.war.structure.CrystalWarStructure;
import de.erethon.factions.war.structure.WarStructure;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.Location;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Fyreum
 */
public class WarListener implements Listener {

    final Factions plugin = Factions.get();
    private final Map<UUID, PendingWarRespawn> pendingWarRespawns = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        WarPhase currentWarPhase = plugin.getCurrentWarPhase();
        // Check for null, if for some reason this breaks again
        if (currentWarPhase == null || !currentWarPhase.isAllowPvP()) {
            return;
        }
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(event.getPlayer());
        Region region = fPlayer.getCurrentRegion();
        if (region == null || !(region instanceof WarRegion warRegion)) {
            return;
        }
        for (WarStructure objective : warRegion.getStructures(WarStructure.class).values()) {
            boolean inRange = objective.containsPlayerPosition(event.getTo());
            if (objective.isActive(fPlayer)) {
                if (inRange) {
                    continue;
                }
                if (isSpectator(fPlayer)) {
                    objective.onSpectatorExit(fPlayer);
                } else {
                    objective.onExit(fPlayer);
                }
            } else if (objective.isSpectator(fPlayer)) {
                if (inRange) {
                    continue;
                }
                objective.onSpectatorExit(fPlayer);
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
        for (WarStructure objective : List.copyOf(fPlayer.getActiveWarObjectives())) {
            objective.onExit(fPlayer);
        }
        fPlayer.getActiveWarObjectives().clear();
    }

    private boolean isSpectator(FPlayer fPlayer) {
        return fPlayer.getAlliance() == null || fPlayer.getPlayer().getGameMode() == GameMode.SPECTATOR;
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
        Region deathRegion = fKilled.getCurrentRegion();
        if (deathRegion != null && deathRegion.getType().isWarGround() && fKilled.getRelation(fKiller) != Relation.ENEMY) {
            return;
        }
        prepareWarRespawn(event, fKilled, deathRegion);
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
        if (plugin.getCurrentWarPhase().isInfluencingScoring() && fKiller.getAlliance() != null) {
            plugin.getWar().getScore().add(fKiller.getAlliance(), 1, WarScoreType.PLAYER_KILL);
        }
        if (deathRegion instanceof WarRegion warRegion && fKiller.getAlliance() == warRegion.getAlliance()) {
            warRegion.getRegionalWarTracker().addContribution(fKiller.getFaction(), "defense_kills", 1);
        }
        // Update stats for assisting players - needs Papyrus patch again
        List<CombatEntry> entries = sKilled.getCombatTracker().entries;
        if (entries.size() > 1) {
            for (int i = 0; i < entries.size() - 1; i++) {
                Entity attacker = entries.get(i).source().getDirectEntity();
                if (attacker == null || !(attacker.getBukkitEntity() instanceof Player player)) {
                    continue;
                }
                FPlayer fAssist = plugin.getFPlayerCache().getByPlayer(player);
                fAssist.getWarStats().assists++;
                fKiller = fAssist;
            }
        }
        // Update war score if necessary
        if (!plugin.getCurrentWarPhase().isInfluencingScoring()) {
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
        if (deathRegion instanceof WarRegion warRegion && fKiller.getAlliance() == warRegion.getAlliance()) {
            warRegion.getRegionalWarTracker().addKill(krAlliance);
        }
    }

    private void prepareWarRespawn(PlayerDeathEvent event, FPlayer fKilled, Region deathRegion) {
        if (deathRegion == null || !deathRegion.getType().isWarGround() || !plugin.getCurrentWarPhase().isAllowPvP()) {
            return;
        }
        Alliance alliance = fKilled.getAlliance();
        if (alliance == null) {
            return;
        }
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);

        WarRegion waypointRegion = plugin.getWar().getNearestWaypoint(event.getPlayer().getLocation(), alliance);
        if (waypointRegion != null) {
            Location waypoint = waypointRegion.getRegionalWarTracker().getWaypointSpawn();
            if (waypoint != null) {
                pendingWarRespawns.put(event.getPlayer().getUniqueId(), new PendingWarRespawn(waypoint,
                        FMessage.WAR_RESPAWN_WAYPOINT.message(waypointRegion.getName()), waypointRegion));
                return;
            }
        }
        Faction faction = fKilled.getFaction();
        if (faction != null && faction.getFHome() != null) {
            pendingWarRespawns.put(event.getPlayer().getUniqueId(), new PendingWarRespawn(faction.getFHome(),
                    FMessage.WAR_RESPAWN_FACTION_HOME.message(), null));
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        PendingWarRespawn pending = pendingWarRespawns.remove(event.getPlayer().getUniqueId());
        if (pending == null) {
            return;
        }
        event.setRespawnLocation(pending.location());
        event.getPlayer().sendMessage(pending.message());
        if (pending.waypointRegion() != null) {
            FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(event.getPlayer());
            pending.waypointRegion().getRegionalWarTracker().addContribution(fPlayer.getFaction(), "waypoint_uses_enabled", 1);
        }
    }

    private record PendingWarRespawn(Location location, Component message, WarRegion waypointRegion) {
    }

    @EventHandler(ignoreCancelled = true)
    public void onCrystalDamage(EntityDamageByEntityEvent event) {
        CrystalWarStructure crystal = getCrystalObjective(event.getEntity());
        if (crystal == null) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!plugin.getCurrentWarPhase().isAllowCapture()) {
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
        CrystalWarStructure crystal = getCrystalObjective(event.getEntity());
        if (crystal == null) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCrystalInteract(PlayerInteractEntityEvent event) {
        CrystalWarStructure crystal = getCrystalObjective(event.getRightClicked());
        if (crystal == null) {
            return;
        }
        event.setCancelled(true);
    }

    private CrystalWarStructure getCrystalObjective(org.bukkit.entity.Entity entity) {
        Region region = plugin.getRegionManager().getRegionByLocation(entity.getLocation());
        if (region == null || !(region instanceof WarRegion warRegion)) {
            return null;
        }
        String name = entity.getPersistentDataContainer().get(WarStructure.NAME_KEY, PersistentDataType.STRING);
        return name == null ? null : warRegion.getStructure(name, CrystalWarStructure.class);
    }
}
