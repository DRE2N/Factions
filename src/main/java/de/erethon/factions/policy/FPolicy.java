package de.erethon.factions.policy;

import de.erethon.factions.policy.handlers.ReduceTaxes;
import de.erethon.factions.entity.FLegalEntity;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public enum FPolicy implements FPolicyHandler {

    REDUCE_TAXES(Component.translatable("factions.policy.reduceTaxes"), PolicyType.POSITIVE, new ReduceTaxes()),

    ;

    private final Component displayName;
    private final PolicyType type;
    private final FPolicyHandler handler;

    FPolicy(@NotNull Component displayName, @NotNull PolicyType type, @NotNull FPolicyHandler handler) {
        this.displayName = displayName;
        this.type = type;
        this.handler = handler;
    }

    @Override
    public void apply(@NotNull FLegalEntity entity) {
        handler.apply(entity);
    }

    @Override
    public void remove(@NotNull FLegalEntity entity) {
        handler.remove(entity);
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
