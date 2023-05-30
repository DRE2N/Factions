package de.erethon.factions.poll.polls;

import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.entity.FLegalEntity;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.poll.AlliancePoll;
import de.erethon.factions.poll.Poll;
import de.erethon.factions.poll.PollScope;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FLogger;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * @author Fyreum
 */
public class CapturedRegionsPoll extends AlliancePoll<Region> {

    private static final Function<Region, ItemStack> CONVERTER = region -> {
        ItemStack itemStack = new ItemStack(region.getType().getIcon());
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(Component.text().color(region.getType().getColor()).append(region.name()).build());
        meta.lore(List.of(FMessage.GUI_POLL_REGION_TYPE_DISPLAY.itemMessage(region.getType().getName())));
        itemStack.setItemMeta(meta);
        return itemStack;
    };

    public CapturedRegionsPoll(@NotNull Alliance alliance) {
        super(FMessage.GENERAL_WAR_ZONES.getMessage(), alliance, PollScope.ADMIN, alliance.getUnconfirmedTemporaryRegions(), CONVERTER);
    }

    @Override
    public int getVotingWeight(@NotNull FPlayer fPlayer) {
        return fPlayer.getFaction().getMembers().size();
    }

    @Override
    protected void onResult(@NotNull TreeSet<PollEntry> results) {
        int i = 0;
        int max = plugin.getFConfig().getWarCapturedRegionsPerBattle();
        for (Poll<Region>.PollEntry entry : results) {
            if (i++ >= max) {
                break;
            }
            alliance.getTemporaryRegions().add(entry.getSubject());
        }
        FLogger.WAR.log("Alliance '" + alliance.getId() + "' temporarily acquired the region: " + alliance.getTemporaryRegions().stream().map(FLegalEntity::getId).toList());
        alliance.getUnconfirmedTemporaryRegions().clear();
    }
}
