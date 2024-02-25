package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.policy.FPolicy;
import org.jetbrains.annotations.NotNull;

public class AddPolicy extends BuildingEffect {

    private final FPolicy policy;

    public AddPolicy(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        policy = FPolicy.valueOf(data.getString("policy").toUpperCase());
    }

    @Override
    public void apply() {
        switch (policy.getScope()) {
            case FACTION -> faction.addPolicy(policy, false);
            case REGION -> site.getRegion().addPolicy(policy, false);
            case ALLIANCE -> site.getFaction().addPolicy(policy, false);
        }
    }

    @Override
    public void remove() {
        switch (policy.getScope()) {
            case FACTION -> faction.removePolicy(policy);
            case REGION -> site.getRegion().removePolicy(policy);
            case ALLIANCE -> site.getFaction().removePolicy(policy);
        }
    }
}
