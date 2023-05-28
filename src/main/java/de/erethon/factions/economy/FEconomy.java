package de.erethon.factions.economy;

import de.erethon.factions.building.BuildSite;
import de.erethon.factions.faction.Faction;

public class FEconomy {

    private final Faction faction;
    private final FStorage storage;

    public FEconomy(Faction faction, FStorage storage) {
        this.faction = faction;
        this.storage = storage;
    }

    public void income() {
        for (BuildSite site : faction.getFactionBuildings()) {
            if (!site.isFinished() || site.isDestroyed()) {
                continue;
            }

        }
    }
}
