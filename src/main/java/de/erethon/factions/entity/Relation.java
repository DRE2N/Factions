package de.erethon.factions.entity;

import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.faction.Faction;
import net.kyori.adventure.text.Component;
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
    ALLY(NamedTextColor.DARK_PURPLE, true, false),
    /**
     * Both parties are in separate alliances and factions.
     */
    ENEMY(NamedTextColor.RED, false, true),
    /**
     * Both parties are in the same faction.
     */
    FACTION(NamedTextColor.GREEN, true, false),
    /**
     * The second party is not part of any alliance or faction.
     */
    NEUTRAL(NamedTextColor.GRAY, true, true),
    /**
     * Both parties are part of the same alliance, but are in separate factions.
     */
    PROTECTED_ALLY(NamedTextColor.LIGHT_PURPLE, false, false),
    /**
     * The first party does not participate.
     */
    SPECTATOR(NamedTextColor.GRAY, false, false)

    ;

    private final TextColor color;
    private final boolean build, attack;

    Relation(TextColor color, boolean build, boolean attack) {
        this.color = color;
        this.build = build;
        this.attack = attack;
    }

    public @NotNull TextColor getColor() {
        return color;
    }

    public @NotNull Component color(@NotNull String string) {
        return Component.text().color(color).content(string).build();
    }

    public @NotNull Component color(@NotNull Component component) {
        return component.color(color);
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
