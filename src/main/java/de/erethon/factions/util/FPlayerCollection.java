package de.erethon.factions.util;

import de.erethon.bedrock.player.PlayerUtil;
import de.erethon.bedrock.player.PlayerWrapper;
import de.erethon.factions.Factions;
import de.erethon.factions.player.FPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author Fyreum
 */
public class FPlayerCollection implements Iterable<UUID> {

    private final Set<UUID> uuids = new HashSet<>();

    public FPlayerCollection(Collection<?> players) {
        for (Object player : players) {
            if (player instanceof OfflinePlayer offlinePlayer) {
                this.uuids.add(offlinePlayer.getUniqueId());
            } else if (player instanceof UUID uuid) {
                this.uuids.add(uuid);
            } else if (player instanceof String string) {
                if (PlayerUtil.isValidUUID(string)) {
                    this.uuids.add(UUID.fromString(string));
                } else {
                    this.uuids.add(PlayerUtil.getUniqueIdFromName(string));
                }
            } else if (player instanceof PlayerWrapper fPlayer) {
                this.uuids.add(fPlayer.getUniqueId());
            }
        }
    }

    public boolean add(@NotNull FPlayer fPlayer) {
        return uuids.add(fPlayer.getUniqueId());
    }

    public boolean add(@NotNull UUID uuid) {
        return uuids.add(uuid);
    }

    public boolean remove(@NotNull FPlayer fPlayer) {
        return uuids.remove(fPlayer.getUniqueId());
    }

    public boolean remove(@NotNull UUID uuid) {
        return uuids.remove(uuid);
    }

    public boolean contains(@NotNull FPlayer fPlayer) {
        return uuids.contains(fPlayer.getUniqueId());
    }

    public boolean contains(@NotNull UUID uuid) {
        return uuids.contains(uuid);
    }

    public boolean isEmpty() {
        return uuids.isEmpty();
    }

    public int size() {
        return uuids.size();
    }

    /* Iterable */

    @NotNull
    @Override
    public Iterator<UUID> iterator() {
        return uuids.iterator();
    }

    @Override
    public void forEach(Consumer<? super UUID> action) {
        uuids.forEach(action);
    }

    @Override
    public Spliterator<UUID> spliterator() {
        return uuids.spliterator();
    }

    /* Getters */

    public @NotNull Set<UUID> getUniqueIds() {
        return uuids;
    }

    public @NotNull Set<FPlayer> getFPlayers() {
        Set<FPlayer> players = new HashSet<>();
        for (UUID uuid : uuids) {
            FPlayer fPlayer = Factions.get().getFPlayerCache().getByUniqueIdIfCached(uuid);
            if (fPlayer == null) {
                continue;
            }
            players.add(fPlayer);
        }
        return players;
    }

    public @NotNull Set<OfflinePlayer> getOfflinePlayers() {
        Set<OfflinePlayer> players = new HashSet<>();
        for (UUID uuid : uuids) {
            players.add(Bukkit.getOfflinePlayer(uuid));
        }
        return players;
    }

    public @NotNull Set<Player> getOnlinePlayers() {
        Set<Player> players = new HashSet<>();
        for (UUID uuid : uuids) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            players.add(player);
        }
        return players;
    }

    public @NotNull Set<FPlayer> getOnlineFPlayers() {
        Set<FPlayer> players = new HashSet<>();
        for (UUID uuid : uuids) {
            FPlayer fPlayer = Factions.get().getFPlayerCache().getByUniqueIdIfCached(uuid);
            if (fPlayer == null || !fPlayer.isOnline()) {
                continue;
            }
            players.add(fPlayer);
        }
        return players;
    }
}
