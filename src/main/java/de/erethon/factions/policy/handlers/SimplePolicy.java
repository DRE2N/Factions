package de.erethon.factions.policy.handlers;

import de.erethon.factions.entity.FLegalEntity;
import de.erethon.factions.policy.FPolicyHandler;
import org.jetbrains.annotations.NotNull;

public class SimplePolicy implements FPolicyHandler {

    public static final SimplePolicy INSTANCE = new SimplePolicy();

    @Override
    public void apply(@NotNull FLegalEntity entity) {
    }

    @Override
    public void remove(@NotNull FLegalEntity entity) {
    }
}
