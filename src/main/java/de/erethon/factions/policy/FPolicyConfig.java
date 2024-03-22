package de.erethon.factions.policy;

import de.erethon.bedrock.config.EConfig;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author Fyreum
 */
public class FPolicyConfig extends EConfig {

    public static final int CONFIG_VERSION = 1;

    public FPolicyConfig(@NotNull File file) {
        super(file, CONFIG_VERSION);
        initialize();
        load();
    }

    @Override
    public void initialize() {
        for (FPolicy policy : FPolicy.values()) {
            initValue("policy." + policy.name(), 1);
        }
        save();
    }

    @Override
    public void load() {
    }

    /* Getters and setters */

    public int getCost(@NotNull FPolicy policy) {
        return config.getInt("policy." + policy.name(), policy.getDefaultCost());
    }
}
