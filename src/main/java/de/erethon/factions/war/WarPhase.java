package de.erethon.factions.war;

import de.erethon.factions.data.FMessage;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public enum WarPhase {

    ACTIVE(true, true, false, FMessage.WAR_PHASE_ACTIVE_DISPLAY_NAME, FMessage.WAR_PHASE_ACTIVE_ANNOUNCEMENT),
    CAPITAL(true, false, true, FMessage.WAR_PHASE_CAPITAL_DISPLAY_NAME, FMessage.WAR_PHASE_CAPITAL_ANNOUNCEMENT),
    INACTIVE(false, false, false, FMessage.WAR_PHASE_INACTIVE_DISPLAY_NAME, FMessage.WAR_PHASE_INACTIVE_ANNOUNCEMENT),
    PASSIVE(true, false, false, FMessage.WAR_PHASE_PASSIVE_DISPLAY_NAME, FMessage.WAR_PHASE_PASSIVE_ANNOUNCEMENT);

    private final boolean allowAttacks, openWarZones, openCapital;
    private final FMessage displayName, announcementMessage;

    WarPhase(boolean allowAttacks, boolean openWarZones, boolean openCapital, @NotNull FMessage displayName, @NotNull FMessage announcementMessage) {
        this.allowAttacks = allowAttacks;
        this.openWarZones = openWarZones;
        this.openCapital = openCapital;
        this.displayName = displayName;
        this.announcementMessage = announcementMessage;
    }

    public boolean isAllowAttacks() {
        return allowAttacks;
    }

    public boolean isOpenWarZones() {
        return openWarZones;
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
