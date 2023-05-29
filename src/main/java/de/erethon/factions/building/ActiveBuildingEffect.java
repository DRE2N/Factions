package de.erethon.factions.building;

import org.jetbrains.annotations.NotNull;

public record ActiveBuildingEffect(@NotNull BuildingEffect effect, @NotNull BuildSite site, long expiration) {

}
