package de.erethon.factions.alliance;

import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public interface AlliancePolicyHandler {

    void apply(@NotNull Alliance alliance);

    void remove(@NotNull Alliance alliance);

}
