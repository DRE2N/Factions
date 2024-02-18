package de.erethon.factions.entity;

import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.faction.Faction;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Fyreum
 */
public interface FEntity extends ForwardingAudience {

    @Nullable Alliance getAlliance();

    default boolean hasAlliance() {
        return getAlliance() != null;
    }

    @Nullable Faction getFaction();

    default boolean hasFaction() {
        return getFaction() != null;
    }

    default @NotNull Relation getRelation(@NotNull FEntity other) {
        return Relation.getRelation(this, other);
    }

    default @NotNull String getDisplayMembership() {
        return hasFaction() ? getFaction().getDisplayShortName() : (hasAlliance() ? getAlliance().getDisplayShortName() : FMessage.GENERAL_LONER.getMessage());
    }

    Component asComponent(FEntity viewer);

}
