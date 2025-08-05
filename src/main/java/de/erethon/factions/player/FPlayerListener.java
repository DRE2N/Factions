package de.erethon.factions.player;

import de.erethon.aergia.ui.UIComponent;
import de.erethon.factions.Factions;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.event.FPlayerCrossRegionEvent;
import de.erethon.factions.integrations.UIFactionsListener;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionMode;
import de.erethon.factions.region.RegionType;
import de.erethon.factions.war.entities.LoggedOutPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.DamageType;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * @author Fyreum
 */
public class FPlayerListener implements Listener {

    final Factions plugin = Factions.get();
    final MiniMessage miniMessage = MiniMessage.miniMessage();
    final Component heading = miniMessage.deserialize("<gradient:red:dark_red><st>       </st></gradient><dark_gray>]<gray><st> </st> <#ad1c11>Erethon</#ad1c11> <st> </st><dark_gray>[<gradient:dark_red:red><st>       </st></gradient>");

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }
        Player player = event.getPlayer();
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayerIfCached(player);
        if (fPlayer == null) {
            return;
        }
        fPlayer.getAutomatedChunkManager().handle(event.getTo().getChunk());
        if (!updateLastRegion(fPlayer, event.getTo())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayerIfCached(player);
        if (fPlayer == null) {
            return;
        }
        Region region = fPlayer.getLastRegion();
        // If the player is in a warzone, spawn a LoggedOutPlayer entity so that the player can be killed
        if (region != null && (region.getMode() == RegionMode.PVP || region.getMode() == RegionMode.PVPVE)) {
            CraftWorld world = (CraftWorld) region.getWorld();
            ServerLevel level = world.getHandle();
            LoggedOutPlayer loggedOutPlayer = new LoggedOutPlayer(level, player);
            level.addFreshEntity(loggedOutPlayer);
        }
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayerIfCached(event.getPlayer());
        if (fPlayer == null) {
            return;
        }
        Region region = fPlayer.getCurrentRegion();
        if (region != null && (region.getType() == RegionType.WAR_ZONE || region.getType() == RegionType.CAPITAL)) {
            if (plugin.getFConfig().getCommandsDisabledInWarZone().contains(event.getMessage())) {
                fPlayer.sendMessage(Component.translatable("factions.war.commandDisabled"));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayerIfCached(event.getPlayer());
        if (fPlayer == null) {
            return;
        }
        updateLastRegion(fPlayer, event.getPlayer().getLocation());
    }

    private boolean updateLastRegion(FPlayer fPlayer, Location to) {
        Region region = plugin.getRegionManager().getRegionByLocation(to);
        if (region == fPlayer.getLastRegion()) {
            return true;
        }
        Region oldRegion = fPlayer.getLastRegion();
        if (pushBackIfInPvP(fPlayer, region, oldRegion, to)) {
            return false;
        }
        fPlayer.setLastRegion(region);
        UIComponent component = fPlayer.getUIBossBar().getCenter().getById(UIFactionsListener.REGION_DISPLAY_ID);
        if (component != null) {
            component.resetDuration();
        }
        Component tabHeader = heading.appendNewline();
        if (region != null) {
            tabHeader = tabHeader.append(Component.text(region.getName().replace("_", " ")).color(region.getType().getColor()));
            tabHeader = tabHeader.append(Component.text(" | ").color(NamedTextColor.DARK_GRAY));
            tabHeader = tabHeader.append(region.getMode().getName().color(region.getMode().getColor()));
        } else {
            tabHeader = tabHeader.append(FMessage.GENERAL_WILDERNESS.message().color(NamedTextColor.DARK_GRAY));
        }
        fPlayer.getPlayer().sendPlayerListHeader(tabHeader);
        if (oldRegion != null && oldRegion.getMode().isSafe()) {
            if (region != null && !region.getMode().isSafe()) {
                Title title = Title.title(
                        Component.empty(),
                        Component.translatable("factions.region.enteredPvP"),
                        Title.DEFAULT_TIMES
                );
                fPlayer.getPlayer().showTitle(title);
            }
        } else if (region != null && region.getMode().isSafe()) {
            Title title = Title.title(
                    Component.empty(),
                    Component.translatable("factions.region.enteredSafe"),
                    Title.DEFAULT_TIMES
            );
            fPlayer.getPlayer().showTitle(title);
        }
        new FPlayerCrossRegionEvent(fPlayer, fPlayer.getLastRegion(), region).callEvent();
        return true;
    }

    boolean pushBackIfInPvP(FPlayer fPlayer,Region newRegion, Region oldRegion, Location to) {
        if (newRegion == oldRegion) {
            return false;
        }
        if (newRegion == null || oldRegion == null) {
            return false;
        }
        if (newRegion.getMode().isSafe() && !oldRegion.getMode().isSafe()) {
            Player player = fPlayer.getPlayer();
            CraftPlayer craftPlayer = (CraftPlayer) player;
            net.minecraft.world.entity.player.Player nmsPlayer = craftPlayer.getHandle();
            if (nmsPlayer.combatTracker.getCombatDuration() > 0) {
                if (nmsPlayer.combatTracker.entries.isEmpty()) {
                    return false;
                }
                CombatEntry combatEntry = nmsPlayer.combatTracker.entries.getFirst();
                if (combatEntry != null && combatEntry.source().getEntity() instanceof Player) {
                    Title title = Title.title(
                            Component.empty(),
                            Component.translatable("factions.region.cannotLeavePvP"),
                            Title.DEFAULT_TIMES
                    );
                    player.showTitle(title);
                    player.playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_THROW, SoundCategory.RECORDS, 0.8f, 0.7f);
                    return true;
                }
            }
        }
        return false;
    }


}
