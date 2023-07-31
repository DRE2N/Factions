package de.erethon.factions.war;

import de.erethon.factions.data.FMessage;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public enum WarPhase {

    CAPITAL(FMessage.WAR_PHASE_CAPITAL_DISPLAY_NAME, FMessage.WAR_PHASE_CAPITAL_ANNOUNCEMENT),
    REGULAR(FMessage.WAR_PHASE_REGULAR_DISPLAY_NAME, FMessage.WAR_PHASE_REGULAR_ANNOUNCEMENT),
    SCORING(FMessage.WAR_PHASE_SCORING_DISPLAY_NAME, FMessage.WAR_PHASE_SCORING_ANNOUNCEMENT),
    PEACE(FMessage.WAR_PHASE_PEACE_DISPLAY_NAME, FMessage.WAR_PHASE_PEACE_ANNOUNCEMENT);

    private final FMessage displayName, announcementMessage;

    WarPhase(@NotNull FMessage displayName, @NotNull FMessage announcementMessage) {
        this.displayName = displayName;
        this.announcementMessage = announcementMessage;
    }

    public boolean isAllowPvP() {
        return this != PEACE;
    }

    public boolean isOpenCapital() {
        return this == CAPITAL;
    }

    public boolean isInfluencingScoring() {
        return this == SCORING || this == CAPITAL;
    }

    public @NotNull FMessage getDisplayName() {
        return displayName;
    }

    public @NotNull FMessage getAnnouncementMessage() {
        return announcementMessage;
    }

}
