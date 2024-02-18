package de.erethon.factions.entity;

import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.faction.Faction;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public enum Relation {

    /**
     * Both parties are part of the same alliance, but are either in separate factions or none.
     * If both parties are in separate factions, they've authorised each other to build on the opposing territories.
     */
    ALLY(true, false, NamedTextColor.DARK_PURPLE),
    /**
     * Both parties are in separate alliances and factions.
     */
    ENEMY(false, true, NamedTextColor.RED),
    /**
     * Both parties are in the same faction.
     */
    FACTION(true, false, NamedTextColor.GREEN),
    /**
     * One or both of the parties are not part of any alliance or faction.
     */
    NEUTRAL(true, true, NamedTextColor.WHITE),
    /**
     * Both parties are part of the same alliance, but are in separate factions.
     */
    PROTECTED_ALLY(false, false, NamedTextColor.DARK_PURPLE),

    ;

    private final boolean build, attack;
    private final TextColor color;

    Relation(boolean build, boolean attack, TextColor color) {
        this.build = build;
        this.attack = attack;
        this.color = color;
    }

    public boolean canBuild() {
        return build;
    }

    public boolean canAttack() {
        return attack;
    }

    public @NotNull TextColor getColor() {
        return color;
    }

    /* Statics */

    public static @NotNull Relation getRelation(@NotNull FEntity entity, FEntity other) {
        Alliance alliance = entity.getAlliance();
        Alliance allianceOther = other.getAlliance();
        if (alliance == null || allianceOther == null) {
            return NEUTRAL;
        }
        if (alliance != allianceOther) {
            return ENEMY;
        }
        Faction faction = entity.getFaction();
        Faction factionOther = other.getFaction();
        if (faction == null) {
            return factionOther == null ? ALLY : PROTECTED_ALLY;
        }
        if (factionOther == null) {
            return ALLY;
        }
        if (faction == factionOther) {
            return FACTION;
        }
        return faction.isAuthorisedBuilder(factionOther) ? ALLY : PROTECTED_ALLY;
    }
}
