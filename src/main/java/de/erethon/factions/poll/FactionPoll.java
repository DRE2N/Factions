package de.erethon.factions.poll;

import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;

/**
 * @author Fyreum
 */
public abstract class FactionPoll<V> extends Poll<V> {

    protected final Faction faction;

    public FactionPoll(@NotNull String name, @NotNull Faction faction, @NotNull PollScope scope, @NotNull Collection<@NotNull V> subjects, @NotNull Function<V, ItemStack> subjectConverter) {
        this(name, faction, scope, subjects, subjectConverter, null);
    }

    public FactionPoll(@NotNull String name, @NotNull Faction faction, @NotNull PollScope scope, @NotNull Collection<@NotNull V> subjects, @NotNull Function<V, ItemStack> subjectConverter, @Nullable Comparator<V> comparator) {
        super(name, scope, subjects, subjectConverter, comparator);
        this.faction = faction;
    }

    @Override
    public boolean canParticipate(@NotNull FPlayer fPlayer) {
        return switch (scope) {
            case ADMIN -> faction.isAdmin(fPlayer);
            case MOD -> faction.isAdmin(fPlayer) || faction.isMod(fPlayer);
            case MEMBER -> faction.isMember(fPlayer);
            default -> true;
        };
    }
}
