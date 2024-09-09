package de.erethon.factions.war.entities.caravans;

import de.erethon.factions.region.RegionStructure;

public record CaravanRoute(RegionStructure start, RegionStructure end, CaravanRouteNode[] nodes) {
}
