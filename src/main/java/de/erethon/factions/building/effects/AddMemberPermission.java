package de.erethon.factions.building.effects;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;

import java.util.HashSet;
import java.util.Set;

public class AddMemberPermission extends BuildingEffect {

    private final Set<String> permissions = new HashSet<>();

    public AddMemberPermission(BuildingEffectData data, BuildSite site) {
        super(data, site);
        permissions.addAll(data.getStringList("permissions"));
    }

    @Override
    public void apply() {
        faction.getAdditionalMemberPermissions().addAll(permissions);
    }

    @Override
    public void remove() {
        faction.getAdditionalMemberPermissions().removeAll(permissions);
    }
}
