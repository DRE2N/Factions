package de.erethon.factions.util;

import de.erethon.factions.faction.Faction;
import de.erethon.factions.region.Region;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public class FactionUtil {

    public static boolean isAdjacent(@NotNull Faction faction, @NotNull Faction other) {
        for (Region region : faction.getRegions()) {
            for (Region otherRegion : other.getRegions()) {
                if (region.isAdjacentRegion(otherRegion)) {
                    return true;
                }
            }
        }
        return false;
    }
}
