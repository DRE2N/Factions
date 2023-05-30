package de.erethon.factions.poll;

import de.erethon.factions.player.FPlayer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Fyreum
 */
public class PollBuilder<V> {

    private String name;
    private PollScope scope;
    private Collection<V> subjects;
    private Function<V, ItemStack> subjectConverter;
    private Comparator<V> comparator;
    private Function<FPlayer, Integer> votingWeight;
    private Predicate<FPlayer> canParticipate;
    private Consumer<TreeSet<Poll<V>.PollEntry>> onResult;

    public @NotNull PollBuilder<V> name(@NotNull String name) {
        this.name = name;
        return this;
    }

    public @NotNull PollBuilder<V> scope(@NotNull PollScope scope) {
        this.scope = scope;
        return this;
    }

    public @NotNull PollBuilder<V> subjects(@NotNull Collection<@NotNull V> subjects) {
        this.subjects = subjects;
        return this;
    }

    public @NotNull PollBuilder<V> subjectConverter(@NotNull Function<V, ItemStack> subjectConverter) {
        this.subjectConverter = subjectConverter;
        return this;
    }

    public @NotNull PollBuilder<V> comparator(@NotNull Comparator<V> comparator) {
        this.comparator = comparator;
        return this;
    }

    public @NotNull PollBuilder<V> votingWeight(@NotNull Function<FPlayer, Integer> votingWeight) {
        this.votingWeight = votingWeight;
        return this;
    }

    public @NotNull PollBuilder<V> canParticipate(@NotNull Predicate<FPlayer> canParticipate) {
        this.canParticipate = canParticipate;
        return this;
    }

    public @NotNull PollBuilder<V> onResult(@NotNull Consumer<TreeSet<Poll<V>.PollEntry>> onResult) {
        this.onResult = onResult;
        return this;
    }

    public @NotNull Poll<V> build() {
        return new Poll<>(name, scope, subjects, subjectConverter, comparator) {
            @Override
            public int getVotingWeight(@NotNull FPlayer fPlayer) {
                return votingWeight.apply(fPlayer);
            }

            @Override
            public boolean canParticipate(@NotNull FPlayer fPlayer) {
                return canParticipate == null || canParticipate.test(fPlayer);
            }

            @Override
            protected void onResult(@NotNull TreeSet<Poll<V>.PollEntry> results) {
                if (onResult != null) onResult.accept(results);
            }
        };
    }

}