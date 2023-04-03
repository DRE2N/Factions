package de.erethon.factions.war;

import de.erethon.factions.data.FMessage;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public enum WarPhase {

    ACTIVE(true, true, FMessage.WAR_PHASE_ACTIVE_DISPLAY_NAME, FMessage.WAR_PHASE_ACTIVE_ANNOUNCEMENT),
    INACTIVE(false, false, FMessage.WAR_PHASE_INACTIVE_DISPLAY_NAME, FMessage.WAR_PHASE_INACTIVE_ANNOUNCEMENT),
    PASSIVE(true, false, FMessage.WAR_PHASE_PASSIVE_DISPLAY_NAME, FMessage.WAR_PHASE_PASSIVE_ANNOUNCEMENT);

    private final boolean allowAttacks, openWarZones;
    private final FMessage displayName, announcementMessage;

    WarPhase(boolean allowAttacks, boolean openWarZones, @NotNull FMessage displayName, @NotNull FMessage announcementMessage) {
        this.allowAttacks = allowAttacks;
        this.openWarZones = openWarZones;
        this.displayName = displayName;
        this.announcementMessage = announcementMessage;
    }

    public boolean isAllowAttacks() {
        return allowAttacks;
    }

    public boolean isOpenWarZones() {
        return openWarZones;
    }

    public @NotNull FMessage getDisplayName() {
        return displayName;
    }

    public @NotNull FMessage getAnnouncementMessage() {
        return announcementMessage;
    }

}
