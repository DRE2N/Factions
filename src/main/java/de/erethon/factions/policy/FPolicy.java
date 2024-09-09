package de.erethon.factions.policy;

import de.erethon.factions.Factions;
import de.erethon.factions.policy.handlers.IncreaseProduction;
import de.erethon.factions.policy.handlers.ReduceTaxes;
import de.erethon.factions.entity.FLegalEntity;
import de.erethon.factions.policy.handlers.SimplePolicy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import static de.erethon.factions.policy.FPolicy.PolicyScope.*;
import static de.erethon.factions.policy.FPolicy.PolicyType.*;

/**
 * @author Fyreum
 */
public enum FPolicy implements FPolicyHandler {

    REDUCE_TAXES(Component.translatable("factions.policy.reduceTaxes"), POSITIVE, FACTION, new ReduceTaxes()),
    INCREASE_RESOURCE_PRODUCTION(Component.translatable("factions.policy.increaseResourceProduction"), POSITIVE, FACTION, new IncreaseProduction()),
    CRYSTAL_CARRIER_HEALTH_BUFF(Component.translatable("factions.policy.crystalCarrierHealthBuff"), POSITIVE, ALLIANCE, SimplePolicy.INSTANCE),
    CRYSTAL_DAMAGE_REDUCTION(Component.translatable("factions.policy.crystalDamageReduction"), POSITIVE, ALLIANCE, SimplePolicy.INSTANCE),
    STRONGER_OBJECTIVE_GUARDS(Component.translatable("factions.policy.strongerObjectiveGuards"), POSITIVE, ALLIANCE, SimplePolicy.INSTANCE),
    OBJECTIVE_GUARDS_REGEN(Component.translatable("factions.policy.objectiveGuardsRegen"), POSITIVE, FACTION, SimplePolicy.INSTANCE),
    STRONGER_CARAVANS(Component.translatable("factions.policy.strongerCaravans"), POSITIVE, ALLIANCE, SimplePolicy.INSTANCE),

    ;

    private final Component displayName;
    private final PolicyType type;
    private final PolicyScope scope;
    private final FPolicyHandler handler;
    private final int defaultCost;

    FPolicy(@NotNull Component displayName, @NotNull PolicyType type, @NotNull PolicyScope scope, @NotNull FPolicyHandler handler) {
        this(displayName, type, scope, handler, 1);
    }

    FPolicy(@NotNull Component displayName, @NotNull PolicyType type, @NotNull PolicyScope scope, @NotNull FPolicyHandler handler, int cost) {
        this.displayName = displayName;
        this.type = type;
        this.handler = handler;
        this.scope = scope;
        this.defaultCost = cost;
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
        return displayName.color(type.getColor());
    }

    public @NotNull PolicyType getType() {
        return type;
    }

    public @NotNull PolicyScope getScope() {
        return scope;
    }

    public int getDefaultCost() {
        return defaultCost;
    }

    public int getCost() {
        return Factions.get().getFPolicyConfig().getCost(this);
    }

    /* Classes */

    public enum PolicyType {
        POSITIVE(NamedTextColor.GREEN),
        NEGATIVE(NamedTextColor.RED),
        NEUTRAL(NamedTextColor.GRAY);

        public final TextColor color;

        PolicyType(@NotNull TextColor color) {
            this.color = color;
        }

        public @NotNull TextColor getColor() {
            return color;
        }
    }

    public enum PolicyScope {
        GLOBAL,
        FACTION,
        REGION,
        ALLIANCE,
    }

}
