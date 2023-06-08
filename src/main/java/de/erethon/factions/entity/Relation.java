package de.erethon.factions.entity;

import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.faction.Faction;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public enum Relation {

    /**
     * Both parties are part of the same alliance, but are either in separate factions or none.
     * If both parties are in separate factions, they've authorised each other to build on the opposing territories.
     */
    ALLY(true, false),
    /**
     * Both parties are in separate alliances and factions.
     */
    ENEMY(false, true),
    /**
     * Both parties are in the same faction.
     */
    FACTION(true, false),
    /**
     * The second party is not part of any alliance or faction.
     */
    NEUTRAL(true, true),
    /**
     * Both parties are part of the same alliance, but are in separate factions.
     */
    PROTECTED_ALLY(false, false),
    /**
     * The first party does not participate.
     */
    SPECTATOR(false, false)

    ;

    private final boolean build, attack;

    Relation(boolean build, boolean attack) {
        this.build = build;
        this.attack = attack;
    }

    public boolean canBuild() {
        return build;
    }

    public boolean canAttack() {
        return attack;
    }

    /* Statics */

    public static @NotNull Relation getRelation(@NotNull FEntity entity, FEntity other) {
        Alliance alliance = entity.getAlliance();
        Alliance allianceOther = other.getAlliance();
        if (alliance == null) {
            return allianceOther == null ? NEUTRAL : SPECTATOR;
        }
        if (allianceOther == null) {
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
