package de.erethon.factions.player;

import de.erethon.aergia.util.TickUtil;
import de.erethon.bedrock.user.UserCache;
import de.erethon.factions.util.FLogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public class FPlayerCache extends UserCache<FPlayer> {

    public FPlayerCache(@NotNull JavaPlugin plugin) {
        super(plugin);
        setUnloadAfter(TickUtil.SECOND * 300);
    }

    @Override
    protected FPlayer getNewInstance(@NotNull OfflinePlayer offlinePlayer) {
        Player player = offlinePlayer.getPlayer();
        if (player != null) {
            FLogger.PLAYER.log("Loading player '" + player.getUniqueId() + "' (" + player.getName() + ")");
            return new FPlayer(player);
        } else if (offlinePlayer.hasPlayedBefore()) {
            FLogger.PLAYER.log("Loading player '" + offlinePlayer.getUniqueId() + "' (" + offlinePlayer.getName() + ")");
            return new FPlayer(offlinePlayer.getUniqueId());
        }
        return null;
    }
}
