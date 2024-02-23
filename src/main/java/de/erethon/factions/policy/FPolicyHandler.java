package de.erethon.factions.policy;

import de.erethon.factions.entity.FLegalEntity;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public interface FPolicyHandler {

    void apply(@NotNull FLegalEntity entity);

    void remove(@NotNull FLegalEntity entity);

}
