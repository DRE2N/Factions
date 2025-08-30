package de.erethon.factions.integrations;

import de.erethon.aergia.player.EPlayer;
import de.erethon.aergia.ui.UIComponent;
import de.erethon.aergia.ui.UIUpdater;
import de.erethon.aergia.ui.event.UICreateEvent;
import de.erethon.aergia.util.TickUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public class UIFactionsListener implements Listener {

    public static final String FACTIONS_INFO_ID = "factionsInfo";
    public static final String REGION_DISPLAY_ID = "regionDisplay";

    final Factions plugin = Factions.get();

    @EventHandler
    public void onUICreate(UICreateEvent event) {
        applyToUIUpdater(event.getUIUpdater());
    }

    public void applyToUIUpdater(@NotNull UIUpdater uiUpdater) {
        uiUpdater.getBossBar().getCenter().add(UIComponent.reactivatable(p -> {
                    FPlayer fPlayer = getFPlayer(p);
                    Region region = fPlayer.getLastRegion();
                    if (region == null) {
                        return Component.text().color(NamedTextColor.GRAY).decorate(TextDecoration.BOLD).content(FMessage.GENERAL_WILDERNESS.getMessage()).build();
                    }
                    int minLevel = region.getLowerLevelBound();
                    int maxLevel = region.getUpperLevelBound();
                    Component level = Component.empty();
                    if (minLevel != -1 && maxLevel != -1) {
                        level = Component.text().color(NamedTextColor.GRAY).decorate(TextDecoration.BOLD).content(" [" + minLevel + "-" + maxLevel + "] ").build();
                    }
                    return level.append(Component.text().color(region.hasAlliance() ? region.getAlliance().getColor() : NamedTextColor.GRAY).decorate(TextDecoration.BOLD).content(fPlayer.getDisplayRegionWithOwner()).build());
                },
                TickUtil.SECOND*6, REGION_DISPLAY_ID));
        uiUpdater.getActionBar().getCenter().add(UIComponent.reactivatable(p -> Component.empty(), TickUtil.SECOND * 4, FACTIONS_INFO_ID));
    }

    private FPlayer getFPlayer(EPlayer ePlayer) {
        return plugin.getFPlayerCache().getByPlayer(ePlayer.getPlayer());
    }
}
