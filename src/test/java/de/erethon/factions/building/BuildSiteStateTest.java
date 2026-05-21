package de.erethon.factions.building;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class BuildSiteStateTest {

    @Test
    void expectedLifecycleStatesArePresent() {
        assertDoesNotThrow(() -> BuildSiteState.valueOf("PLACED"));
        assertDoesNotThrow(() -> BuildSiteState.valueOf("READY_FOR_REVIEW"));
        assertDoesNotThrow(() -> BuildSiteState.valueOf("DENIED"));
        assertDoesNotThrow(() -> BuildSiteState.valueOf("ACTIVE"));
        assertDoesNotThrow(() -> BuildSiteState.valueOf("DAMAGED"));
    }
}
