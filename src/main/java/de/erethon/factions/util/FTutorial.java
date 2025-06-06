package de.erethon.factions.util;

import de.erethon.factions.Factions;
import de.erethon.factions.player.FPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class FTutorial {

    private static final Factions plugin = Factions.get();
    private static final Component prefix = MiniMessage.miniMessage().deserialize("<purple><bold>?</bold></purple><dark_gray> |</dark_gray>");

    public static void showHint(Player player, String hint) {
        if (player == null) return;
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(player);
        showHint(fPlayer, hint);
    }

    public static void showHint(FPlayer fPlayer, String hint) {
        if (fPlayer == null || fPlayer.getPlayer() == null || fPlayer.hasSeenHint(hint)) return;
        fPlayer.sendMessage(prefix.append(Component.translatable("factions.tutorial.hint. " + hint)));
        fPlayer.getPlayer().playSound(fPlayer.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
        fPlayer.addSeenHint(hint);
    }
}
