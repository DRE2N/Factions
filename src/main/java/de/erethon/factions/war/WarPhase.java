package de.erethon.factions.war;

import de.erethon.factions.data.FMessage;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public enum WarPhase {

    CAPITAL(true, true, FMessage.WAR_PHASE_CAPITAL_DISPLAY_NAME, FMessage.WAR_PHASE_CAPITAL_ANNOUNCEMENT),
    REGULAR(true, false, FMessage.WAR_PHASE_REGULAR_DISPLAY_NAME, FMessage.WAR_PHASE_REGULAR_ANNOUNCEMENT),
    PEACE(false, false, FMessage.WAR_PHASE_PEACE_DISPLAY_NAME, FMessage.WAR_PHASE_PEACE_ANNOUNCEMENT);

    private final boolean allowPvP, openCapital;
    private final FMessage displayName, announcementMessage;

    WarPhase(boolean allowPvP, boolean openCapital, @NotNull FMessage displayName, @NotNull FMessage announcementMessage) {
        this.allowPvP = allowPvP;
        this.openCapital = openCapital;
        this.displayName = displayName;
        this.announcementMessage = announcementMessage;
    }

    public boolean isAllowPvP() {
        return allowPvP;
    }

    public boolean isOpenCapital() {
        return openCapital;
    }

    public @NotNull FMessage getDisplayName() {
        return displayName;
    }

    public @NotNull FMessage getAnnouncementMessage() {
        return announcementMessage;
    }

}
