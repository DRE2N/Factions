package de.erethon.factions.building;

public enum RequirementFail {
    NOT_IN_FACTION("factions.building.requirement.not_in_faction"),
    NOT_IN_REGION("factions.building.requirement.not_in_region"),
    NO_PERMISSION("factions.building.requirement.no_permission"),
    TOO_CLOSE_TO_BORDER("factions.building.requirement.too_close_to_border"),
    OVERLAPPING_BUILDING("factions.building.requirement.overlapping_building"),
    REQUIRED_BUILDING_MISSING("fxl.building.requirement.required_building_missing"),
    REQUIRED_POPULATION("factions.building.requirement.required_population"),
    WRONG_REGION_TYPE("factions.building.requirement.wrong_region_type"),
    UNIQUE_BUILDING("factions.building.requirement.unique_building"),
    NOT_ENOUGH_RESOURCES("factions.building.requirement.not_enough_resources");

    private final String translationKey;

    RequirementFail(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }

}
