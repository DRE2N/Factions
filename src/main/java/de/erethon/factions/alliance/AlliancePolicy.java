package de.erethon.factions.alliance;

import de.erethon.factions.alliance.policies.ReduceTaxes;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public enum AlliancePolicy implements AlliancePolicyHandler {

    REDUCE_TAXES(Component.translatable("factions.policy.reduceTaxes"), PolicyType.POSITIVE, new ReduceTaxes()),

    ;

    private final Component displayName;
    private final PolicyType type;
    private final AlliancePolicyHandler handler;

    AlliancePolicy(@NotNull Component displayName, @NotNull PolicyType type, @NotNull AlliancePolicyHandler handler) {
        this.displayName = displayName;
        this.type = type;
        this.handler = handler;
    }

    @Override
    public void apply(@NotNull Alliance alliance) {
        handler.apply(alliance);
    }

    @Override
    public void remove(@NotNull Alliance alliance) {
        handler.remove(alliance);
    }

    /* Getters */

    public @NotNull Component getDisplayName() {
        return displayName;
    }

    public @NotNull PolicyType getType() {
        return type;
    }

    /* Classes */

    public enum PolicyType {
        POSITIVE,
        NEGATIVE,
        NEUTRAL
    }

}
