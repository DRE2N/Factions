package de.erethon.factions.poll;

import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.player.FPlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;

/**
 * @author Fyreum
 */
public abstract class AlliancePoll<V> extends Poll<V> {

    protected final Alliance alliance;

    public AlliancePoll(@NotNull String name, @NotNull Alliance alliance, @NotNull PollScope scope,
                        @NotNull Collection<V> subjects, @NotNull Function<V, ItemStack> subjectConverter) {
        this(name, alliance, scope, subjects, subjectConverter, null);
    }

    public AlliancePoll(@NotNull String name, @NotNull Alliance alliance, @NotNull PollScope scope,
                        @NotNull Collection<V> subjects, @NotNull Function<V, ItemStack> subjectConverter,
                        @Nullable Comparator<V> comparator) {
        super(name, scope, subjects, subjectConverter, comparator);
        this.alliance = alliance;
    }

    public AlliancePoll(@NotNull ConfigurationSection config, @NotNull Function<V, ItemStack> subjectConverter) throws IllegalArgumentException {
        super(config, subjectConverter);
        this.alliance = plugin.getAllianceCache().getById(config.getInt("alliance"));
        assert alliance != null : "Illegal alliance in poll '" + name + "' found: " + config.getInt("alliance");
    }

    @Override
    public boolean canParticipate(@NotNull FPlayer fPlayer) {
        return switch (scope) {
            case ADMIN -> fPlayer.getAlliance() == alliance && fPlayer.isAdmin();
            case MOD -> fPlayer.getAlliance() == alliance && fPlayer.isMod();
            case MEMBER -> fPlayer.getAlliance() == alliance;
            default -> true;
        };
    }
}
