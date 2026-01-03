package de.erethon.factions.integration;

import de.erethon.factions.Factions;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.util.FLogger;
import org.popcraft.bolt.BoltAPI;
import org.popcraft.bolt.source.SourceTypes;

/**
 * @author Fyreum
 */
public class BoltIntegration {

    private static boolean initialized = false;

    public static void setup(Factions plugin) {
        if (initialized) {
            return;
        }
        initialized = true;

        if (!plugin.getServer().getPluginManager().isPluginEnabled("Bolt")) {
            return;
        }

        BoltAPI bolt = plugin.getServer().getServicesManager().load(BoltAPI.class);

        if (bolt == null) {
            FLogger.INFO.log("Bolt not found, integration disabled.");
            return;
        }

        FLogger.INFO.log("Bolt found, register integration...");

        bolt.registerPlayerSourceResolver((source, uuid) -> {
            if (!SourceTypes.FACTION.equals(source.getType())) {
                return false;
            }

            final String factionName = source.getIdentifier();
            final Faction faction = plugin.getFactionCache().getByName(factionName);

            if (faction == null) {
                return false;
            }
            return faction.isAdmin(uuid) || faction.isMember(uuid);
        });
    }

}
