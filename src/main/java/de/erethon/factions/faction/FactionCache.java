package de.erethon.factions.faction;

import de.erethon.aergia.util.BroadcastUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.data.FConfig;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.entity.FEntityCache;
import de.erethon.factions.event.FPlayerFactionLeaveEvent;
import de.erethon.factions.event.FactionCreateEvent;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FException;
import de.erethon.factions.util.FLogger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.UUID;

/**
 * @author Fyreum
 */
public class FactionCache extends FEntityCache<Faction> {

    final Factions plugin = Factions.get();

    private BukkitTask kickTask;

    public FactionCache(@NotNull File folder) {
        super(folder);
    }

    @Override
    protected @Nullable Faction create(@NotNull File file) {
        try {
            return new Faction(file);
        } catch (NumberFormatException e) {
            FLogger.ERROR.log("Couldn't load faction file '" + file.getName() + "': Invalid ID");
            return null;
        }
    }

    public synchronized @NotNull Faction create(@NotNull FPlayer fPlayer, @NotNull Region coreRegion, @NotNull String name) throws FException {
        FException.throwIf(plugin.getFConfig().isNameForbidden(name), "Couldn't create faction: forbidden name", FMessage.ERROR_NAME_IS_FORBIDDEN, name);
        FException.throwIf(getByName(name) != null, "Couldn't create faction: name already in use", FMessage.ERROR_NAME_IN_USE, name);
        Faction faction = new Faction(fPlayer, coreRegion, generateId(), name, null);
        faction.addDefaultAttributes(); // Initialize default attributes
        cache.put(faction.getId(), faction);
        fPlayer.setFaction(faction);
        new FactionCreateEvent(faction, fPlayer).callEvent();
        BroadcastUtil.broadcast(FMessage.FACTION_INFO_CREATED.message(fPlayer.getLastName(), faction.getName()));
        return faction;
    }

    /**
     * Returns an unused faction id.
     *
     * @return an unused faction id
     */
    public synchronized int generateId() {
        int id = 0;
        while (getById(id) != null) {
            id++;
        }
        return id;
    }

    public void runKickTask() {
        if (kickTask != null) {
            kickTask.cancel();
        }
        kickTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::kickInactivePlayers, 200L, plugin.getFConfig().getInactiveKickTimer());
    }

    /**
     * Kick inactive players out of their faction and disband factions without other members left.
     */
    public void kickInactivePlayers() {
        FLogger.FACTION.log("Kicking inactive faction members...");
        FConfig fConfig = plugin.getFConfig();
        long kickAdminAfter = fConfig.getInactiveAdminKickDuration();
        long kickAfter = fConfig.getInactiveKickDuration();

        for (Faction faction : cache.values()) {
            for (UUID uuid : faction.getMembers()) {
                OfflinePlayer member = Bukkit.getOfflinePlayer(uuid);
                if (member.getLastSeen() + (faction.isAdmin(uuid) ? kickAdminAfter : kickAfter) > System.currentTimeMillis()) {
                    continue;
                }
                FPlayer fPlayer = plugin.getFPlayerCache().getByUniqueId(uuid);
                if (fPlayer == null) {
                    FLogger.ERROR.log("Couldn't kick inactive player '" + uuid + "': FPlayer is null");
                    continue;
                }
                faction.playerLeave(fPlayer, FPlayerFactionLeaveEvent.Reason.INACTIVE);
            }
        }
    }

    @Override
    public void loadAll() {
        super.loadAll();
        FLogger.INFO.log("Loaded " + cache.size() + " factions");
    }

    /* Getters */

    public @Nullable BukkitTask getKickTask() {
        return kickTask;
    }
}
