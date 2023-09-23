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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * @author Fyreum
 */
public class CapturedRegionsPoll extends AlliancePoll<Region> {

    public static final Function<Region, ItemStack> REGION_CONVERTER = region -> {
        ItemStack itemStack = new ItemStack(region.getType().getIcon());
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(Component.text().color(region.getType().getColor()).append(region.name()).build());
        meta.lore(List.of(FMessage.GUI_POLL_REGION_TYPE_DISPLAY.itemMessage(region.getType().getName())));
        itemStack.setItemMeta(meta);
        return itemStack;
    };

    public CapturedRegionsPoll(@NotNull Alliance alliance) {
        super(FMessage.GENERAL_WAR_ZONES.getMessage(), alliance, PollScope.ADMIN, alliance.getUnconfirmedTemporaryRegions(), REGION_CONVERTER);
    }

    public CapturedRegionsPoll(@NotNull ConfigurationSection config) throws IllegalArgumentException {
        super(config, REGION_CONVERTER);
    }

    @Override
    public int getVotingWeight(@NotNull FPlayer fPlayer) {
        return fPlayer.getFaction().getMembers().size();
    }

    @Override
    protected @NotNull Object subjectToId(@NotNull Region subject) {
        return subject.getId();
    }

    @Override
    protected @Nullable Region idToSubject(@NotNull Object id) {
        return plugin.getRegionManager().getRegionById(NumberConversions.toInt(id));
    }

    @Override
    protected void onResult(@NotNull TreeSet<PollEntry> results) {
        int i = 0;
        int max = plugin.getFConfig().getWarCapturedRegionsPerBattle();
        for (Poll<Region>.PollEntry entry : results) {
            if (i++ >= max) {
                break;
            }
            alliance.persistTemporaryOccupy(entry.getSubject());
        }
        FLogger.WAR.log("Alliance '" + alliance.getId() + "' persistently occupied the regions: " + alliance.getTemporaryRegions().stream().map(FLegalEntity::getId).toList());
        alliance.getUnconfirmedTemporaryRegions().clear();
    }
}
