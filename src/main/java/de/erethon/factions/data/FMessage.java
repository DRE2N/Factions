package de.erethon.factions.data;

import de.erethon.bedrock.config.Message;
import de.erethon.bedrock.config.MessageHandler;
import de.erethon.factions.Factions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public enum FMessage implements Message {

    ALLIANCE_INFO_FACTION_CANNOT_AFFORD_DAILY_TAXES("alliance.info.factionCannotAffordDailyTaxes"),
    ALLIANCE_INFO_NEW_POLL("alliance.info.newPoll"),
    ALLIANCE_INFO_REGION_LOST("alliance.info.regionLost"),
    ALLIANCE_INFO_PREFIX("alliance.info.prefix"),

    ACM_ADDED_CHUNK("acm.addedChunk"),
    ACM_ADDED_CHUNKS("acm.addedChunks"),
    ACM_CHUNK_ALREADY_REGION("acm.chunkAlreadyRegion"),
    ACM_DEACTIVATED("acm.deactivated"),
    ACM_ILLEGAL_RADIUS("acm.illegalRadius"),
    ACM_PREFIX("acm.prefix"),
    ACM_NOT_SELECTED("acm.notSelected"),
    ACM_RADIUS_SELECTED("acm.radiusSelected"),
    ACM_REMOVED_CHUNK("acm.removedChunk"),
    ACM_REMOVED_CHUNKS("acm.removedChunks"),
    ACM_SELECTED("acm.selected"),
    ACM_SHAPE("acm.shape"),
    ACM_UNSELECTED("acm.unselected"),
    ACM_TRANSFERRED_CHUNK("acm.transferredChunk"),
    ACM_TRANSFERRED_CHUNKS("acm.transferredChunks"),

    BUILDING_SITE_CREATED("buildingSite.created"),

    CMD_ALLIANCE_CHOOSE_ON_COOLDOWN("cmd.alliance.choose.onCooldown"),
    CMD_ALLIANCE_CHOOSE_SUCCESS("cmd.alliance.choose.success"),
    CMD_ALLIANCE_SHOW_DESCRIPTION("cmd.show.description"),
    CMD_ALLIANCE_SHOW_HEADER("cmd.show.header"),
    CMD_ALLIANCE_SHOW_LONG_NAME("cmd.show.longName"),
    CMD_ALLIANCE_SHOW_MEMBERS("cmd.show.members"),
    CMD_ALLIANCE_SHOW_MONEY("cmd.show.money"),
    CMD_ALLIANCE_SHOW_SEPARATOR("cmd.show.separator"),
    CMD_ALLIANCE_SHOW_SHORT_NAME("cmd.show.shortName"),
    CMD_AUTHORISE_ADDED("cmd.authorise.added"),
    CMD_AUTHORISE_REMOVED("cmd.authorise.removed"),
    CMD_CREATE_WAR_FLAG_SUCCESS("cmd.createWarFlag.success"),
    CMD_DECLINE_SUCCESS("cmd.decline.success"),
    CMD_DISBAND_CONFIRMATION_REQUIRED("cmd.disband.confirmationRequired"),
    CMD_JOIN_NOT_INVITED("cmd.join.notInvited"),
    CMD_JOIN_ON_COOLDOWN("cmd.join.onCooldown"),
    CMD_INVITE_SUCCESS("cmd.invite.success"),
    CMD_JOIN_SUCCESS("cmd.join.success"),
    CMD_KICK_SUCCESS("cmd.kick.success"),
    CMD_LEAVE_CONFIRMATION_REQUIRED("cmd.leave.confirmationRequired"),
    CMD_LEAVE_SUCCESS("cmd.leave.success"),
    CMD_OBJECTIVE_CREATE_SUCCESS("cmd.objective.create.success"),
    CMD_PORTAL_ADD_CONDITION_SUCCESS("cmd.portal.addCondition.success"),
    CMD_PORTAL_CREATE_SUCCESS("cmd.portal.create.success"),
    CMD_PORTAL_REMOVE_CONDITION_SUCCESS("cmd.portal.removeCondition.success"),
    CMD_PORTAL_SET_LOCATION_SUCCESS("cmd.portal.setLocation.success"),
    CMD_POS1_SUCCESS("cmd.pos1.success"),
    CMD_POS2_SUCCESS("cmd.pos2.success"),
    CMD_REGION_ADD_CHUNK_SUCCESS("cmd.region.addChunk.success"),
    CMD_REGION_ADD_NEIGHBOUR_SUCCESS("cmd.region.addNeighbour.success"),
    CMD_REGION_ALLIANCE_SUCCESS("cmd.region.alliance.success"),
    CMD_REGION_CREATE_SUCCESS("cmd.region.create.success"),
    CMD_REGION_DAMAGE_REDUCTION_SUCCESS("cmd.region.damageReduction.success"),
    CMD_REGION_DELETE_CONFIRMATION_REQUIRED("cmd.region.delete.confirmationRequired"),
    CMD_REGION_DELETE_FAILED("cmd.region.delete.failed"),
    CMD_REGION_DELETE_SUCCESS("cmd.region.delete.success"),
    CMD_REGION_DESCRIPTION_SUCCESS("cmd.region.description.success"),
    CMD_REGION_INFO_ADJACENT_REGIONS("cmd.region.info.adjacentRegions"),
    CMD_REGION_INFO_CHUNKS("cmd.region.info.chunks"),
    CMD_REGION_INFO_DESCRIPTION("cmd.region.info.description"),
    CMD_REGION_INFO_HEADER("cmd.region.info.header"),
    CMD_REGION_INFO_ID("cmd.region.info.id"),
    CMD_REGION_INFO_TYPE("cmd.region.info.type"),
    CMD_REGION_INFO_OWNER("cmd.region.info.owner"),
    CMD_REGION_INFO_PRICE("cmd.region.info.price"),
    CMD_REGION_INFO_WAR_VALUE("cmd.region.info.warValue"),
    CMD_REGION_NAME_SUCCESS("cmd.region.name.success"),
    CMD_REGION_REMOVE_CHUNK_SUCCESS("cmd.region.removeChunk.success"),
    CMD_REGION_REMOVE_NEIGHBOUR_SUCCESS("cmd.region.removeNeighbour.success"),
    CMD_REGION_SPLIT_SUCCESS("cmd.region.split.success"),
    CMD_REGION_STATUS_ALLIANCE_HEADER("cmd.region.status.allianceHeader"),
    CMD_REGION_STATUS_CAPTURE_CAP("cmd.region.status.captureCap"),
    CMD_REGION_STATUS_HEADER("cmd.region.status.header"),
    CMD_REGION_STATUS_KILLS("cmd.region.status.kills"),
    CMD_REGION_STATUS_LEADER("cmd.region.status.leader"),
    CMD_REGION_STATUS_SCORE("cmd.region.status.score"),
    CMD_REGION_STRUCTURE_CREATE_SUCCESS("cmd.region.structure.create.success"),
    CMD_REGION_STRUCTURE_LIST_EMPTY("cmd.region.structure.list.empty"),
    CMD_REGION_STRUCTURE_LIST_TYPE("cmd.region.structure.list.type"),
    CMD_REGION_TYPE_SUCCESS("cmd.region.type.success"),
    CMD_RELOAD_ALL_END("cmd.reload.allEnd"),
    CMD_RELOAD_ALL_START("cmd.reload.allStart"),
    CMD_RELOAD_CACHES("cmd.reload.caches"),
    CMD_RELOAD_CONFIGS("cmd.reload.configs"),
    CMD_RELOAD_MESSAGES("cmd.reload.messages"),
    CMD_SHOW_ADMIN("cmd.show.admin"),
    CMD_SHOW_ALLIANCE("cmd.show.alliance"),
    CMD_SHOW_CORE_REGION("cmd.show.coreRegion"),
    CMD_SHOW_DESCRIPTION("cmd.show.description"),
    CMD_SHOW_HEADER("cmd.show.header"),
    CMD_SHOW_LEVEL("cmd.show.level"),
    CMD_SHOW_LONG_NAME("cmd.show.longName"),
    CMD_SHOW_MEMBERS("cmd.show.members"),
    CMD_SHOW_MONEY("cmd.show.money"),
    CMD_SHOW_SEPARATOR("cmd.show.separator"),
    CMD_SHOW_SHORT_NAME("cmd.show.shortName"),
    CMD_STATS_CAPTURING_TIME("cmd.stats.capturingTime"),
    CMD_STATS_HEADER("cmd.stats.header"),
    CMD_STATS_HIGHEST_KILL_STREAK("cmd.stats.highestKillStreak"),
    CMD_STATS_KD("cmd.stats.kd"),
    CMD_STATS_KILL_STREAK("cmd.stats.killStreak"),
    CMD_STATS_SEPARATOR("cmd.stats.separator"),
    CMD_VERSION_AUTHORS("cmd.version.authors"),
    CMD_VERSION_HEADER("cmd.version.header"),
    CMD_VERSION_STATUS("cmd.version.status"),
    CMD_WAR_HISTORY_END_DATE("cmd.warHistory.endDate"),
    CMD_WAR_HISTORY_ENTRY("cmd.warHistory.entry"),
    CMD_WAR_HISTORY_WINNER("cmd.warHistory.winner"),

    ERROR_ACM_SHAPE_NOT_FOUND("error.acmShapeNotFound"),
    ERROR_BUILDING_BLOCKED("error.buildingBlocked"),
    ERROR_BUILDING_REQUIRED_TYPE("error.buildingRequiredType"),
    ERROR_BUILDING_REQUIRED_FACTION("error.buildingRequiredFaction"),
    ERROR_BUILDING_POPULATION("error.buildingPopulation"),
    ERROR_BUILDING_NOT_ENOUGH_RESOURCES("error.buildingNotEnoughResources"),
    ERROR_CANNOT_AUTHORISE_SELF("error.cannotAuthoriseSelf"),
    ERROR_CANNOT_CHOOSE_ALLIANCE("error.cannotChooseAlliance"),
    ERROR_CANNOT_KICK_YOURSELF("error.cannotKickYourself"),
    ERROR_CANNOT_UNCLAIM_CORE_REGION("error.cannotUnclaimCoreRegion"),
    ERROR_CHUNK_ALREADY_A_REGION("error.chunkAlreadyARegion"),
    ERROR_CHUNK_OUTSIDE_THE_REGION("error.chunkOutsideTheRegion"),
    ERROR_DEBUG_MODE_NOT_ENABLED("error.debugModeNotEnabled"),
    ERROR_FACTION_ALREADY_OCCUPIED_REGION("error.factionAlreadyOccupiedRegion"),
    ERROR_FACTION_IS_FULL("error.factionIsFull"),
    ERROR_FACTION_HAS_NOT_ENOUGH_MONEY("error.factionHasNotEnoughMoney"),
    ERROR_FACTION_NOT_FOUND("error.factionNotFound"),
    ERROR_NAME_IN_USE("error.nameInUse"),
    ERROR_NAME_IS_FORBIDDEN("error.nameIsForbidden"),
    ERROR_NO_BANNER_ITEM_IN_HAND("error.noBannerItemInHand"),
    ERROR_NO_PERMISSION("error.noPermission"),
    ERROR_NO_SELECTION("error.noSelection"),
    ERROR_NOT_ENOUGH_MONEY("error.notEnoughMoney"),
    ERROR_NOT_PARSABLE_DOUBLE("error.notParsableDouble"),
    ERROR_NOT_PARSABLE_INTEGER("error.notParsableInteger"),
    ERROR_PERMITLESS_ALLIANCE("error.permitlessAlliance"),
    ERROR_PLAYER_ALREADY_IN_A_FACTION("error.playerAlreadyInAFaction"),
    ERROR_PLAYER_IS_NOT_IN_A_FACTION("error.playerIsNotInAFaction"),
    ERROR_PLAYER_IS_NOT_IN_AN_ALLIANCE("error.playerIsNotInAnAlliance"),
    ERROR_PLAYER_NOT_FOUND("error.playerNotFound"),
    ERROR_PLAYER_REGION_NULL("error.playerRegionNull"),
    ERROR_POLL_NOT_FOUND("error.pollNotFound"),
    ERROR_PORTAL_ALLIANCE_NOT_SETUP("error.portal.allianceNotSetup"),
    ERROR_PORTAL_CAPITAL_CLOSED("error.portal.capitalClosed"),
    ERROR_PORTAL_CONDITION_NOT_FOUND("error.portal.conditionNotFound"),
    ERROR_PORTAL_NOT_FOUND("error.portal.notFound"),
    ERROR_PORTAL_PVP_NOT_ALLOWED("error.portal.pvpNotAllowed"),
    ERROR_REGION_ALREADY_CLAIMED("error.regionAlreadyClaimed"),
    ERROR_REGION_ALREADY_OCCUPIED("error.regionAlreadyOccupied"),
    ERROR_REGION_IN_ANOTHER_WORLD("error.regionInAnotherWorld"),
    ERROR_REGION_IS_NOT_A_WARZONE("error.regionIsNotAWarzone"),
    ERROR_REGION_IS_NOT_CLAIMABLE("error.regionIsNotClaimable"),
    ERROR_REGION_IS_NOT_OCCUPIABLE("error.regionIsNotOccupiable"),
    ERROR_REGION_NOT_FOUND("error.regionNotFound"),
    ERROR_REGION_STRUCTURE_TYPE_NOT_FOUND("error.regionStructureTypeNotFound"),
    ERROR_REGION_STRUCTURE_WRONG_ARG_FORMAT("error.regionStructureWrongArgFormat"),
    ERROR_REGION_TYPE_NOT_FOUND("error.regionTypeNotFound"),
    ERROR_REGIONS_ALREADY_NEIGHBOURS("error.regionsAlreadyNeighbours"),
    ERROR_REGIONS_ARE_NOT_NEIGHBOURS("error.regionsAreNotNeighbours"),
    ERROR_SELECTION_IN_DIFFERENT_REGIONS("error.selectionInDifferentRegions"),
    ERROR_SELECTION_IN_DIFFERENT_WORLDS("error.selectionInDifferentWorlds"),
    ERROR_SENDER_IS_NO_PLAYER("error.senderIsNoPlayer"),
    ERROR_TARGET_IS_ALREADY_ADMIN("error.targetIsAlreadyAdmin"),
    ERROR_TARGET_IS_ALREADY_IN_THIS_FACTION("error.targetIsAlreadyInThisFaction"),
    ERROR_TARGET_IS_NOT_IN_A_FACTION("error.targetIsNotInAFaction"),
    ERROR_TARGET_IS_NOT_IN_THIS_FACTION("error.targetIsNotInThisFaction"),
    ERROR_TEXT_IS_TOO_LONG("error.textIsTooLong"),
    ERROR_WAR_OBJECTIVE_NOT_FOUND("error.warObjectiveNotFound"),
    ERROR_WAR_OBJECTIVE_TYPE_NOT_FOUND("error.warObjectiveTypeNotFound"),
    ERROR_WAR_PHASE_NOT_FOUND("error.warPhaseNotFound"),
    ERROR_WORLD_IS_REGIONLESS("error.worldIsRegionless"),
    ERROR_WRONG_DOUBLE_VALUE("error.wrongDoubleValue"),

    FACTION_INFO_ADDED_BUILDER_AUTHORITY("faction.info.addedBuilderAuthority"),
    FACTION_INFO_ADDED_BUILDER_AUTHORITY_OTHER("faction.info.addedBuilderAuthorityOther"),
    FACTION_INFO_ADMIN_CHANGED("faction.info.adminChanged"),
    FACTION_INFO_CREATED("faction.info.created"),
    FACTION_INFO_DESCRIPTION_CHANGED("faction.info.descriptionChanged"),
    FACTION_INFO_DISBANDED("faction.info.disbanded"),
    FACTION_INFO_INACTIVE_PLAYER_KICKED("faction.info.inactivePlayerKicked"),
    FACTION_INFO_LONG_NAME_CHANGED("faction.info.longNameChanged"),
    FACTION_INFO_NAME_CHANGED("faction.info.nameChanged"),
    FACTION_INFO_NEW_ADMIN("faction.info.newAdmin"),
    FACTION_INFO_NEW_POLL("faction.info.newPoll"),
    FACTION_INFO_PAYED_DAILY_TAXES("faction.info.payedDailyTaxes"),
    FACTION_INFO_PLAYER_GOT_KICKED("faction.info.playerGotKicked"),
    FACTION_INFO_PLAYER_JOINED("faction.info.playerJoined"),
    FACTION_INFO_PLAYER_LEFT("faction.info.playerLeft"),
    FACTION_INFO_PREFIX("faction.info.prefix"),
    FACTION_INFO_REGION_CLAIMED("faction.info.regionClaimed"),
    FACTION_INFO_REMOVED_BUILDER_AUTHORITY("faction.info.removedBuilderAuthority"),
    FACTION_INFO_REMOVED_BUILDER_AUTHORITY_OTHER("faction.info.removedBuilderAuthorityOther"),
    FACTION_INFO_SHORT_NAME_CHANGED("faction.info.shortNameChanged"),
    FACTION_INVITE_ACCEPT("faction.invite.accept"),
    FACTION_INVITE_ACCEPT_HOVER("faction.invite.acceptHover"),
    FACTION_INVITE_DECLINE("faction.invite.decline"),
    FACTION_INVITE_DECLINE_HOVER("faction.invite.declineHover"),
    FACTION_INVITE_MESSAGE("faction.invite.message"),

    GENERAL_DEFAULT_DESCRIPTION("general.defaultDescription"),
    GENERAL_FACTION("general.faction"),
    GENERAL_LONER("general.loner"),
    GENERAL_NO("general.no"),
    GENERAL_NONE("general.none"),
    GENERAL_REGION_DEFAULT_NAME_PREFIX("general.regionDefaultNamePrefix"),
    GENERAL_SPECTATOR("general.spectator"),
    GENERAL_UNKNOWN("general.unknown"),
    GENERAL_WAR_ZONES("general.warZones"),
    GENERAL_WILDERNESS("general.wilderness"),
    GENERAL_YES("general.yes"),

    GUI_POLL_NEXT_PAGE_INFO("gui.poll.nextPage.info"),
    GUI_POLL_NEXT_PAGE_NAME("gui.poll.nextPage.name"),
    GUI_POLL_PREVIOUS_PAGE_INFO("gui.poll.previousPage.info"),
    GUI_POLL_PREVIOUS_PAGE_NAME("gui.poll.previousPage.name"),
    GUI_POLL_TITLE("gui.poll.title"),
    GUI_POLL_TITLE_CLOSED("gui.poll.titleClosed"),
    GUI_POLL_REGION_TYPE_DISPLAY("gui.poll.regionTypeDisplay"),
    GUI_POLL_VOTES_DISPLAY("gui.poll.votesDisplay"),

    PLACEHOLDER_ALLIANCE_DISPLAY("placeholder.allianceDisplay"),
    PLACEHOLDER_FACTION_DISPLAY("placeholder.factionDisplay"),

    PROTECTION_CANNOT_ATTACK_CAPITAL("protection.cannotAttack.capital"),
    PROTECTION_CANNOT_ATTACK_FACTION("protection.cannotAttack.faction"),
    PROTECTION_CANNOT_ATTACK_IN_CURRENT_PHASE("protection.cannotAttack.inCurrentPhase"),
    PROTECTION_CANNOT_ATTACK_PLAYER("protection.cannotAttack.player"),
    PROTECTION_CANNOT_ATTACK_WAR_OBJECTIVE("protection.cannotAttack.warObjective"),
    PROTECTION_CANNOT_BUILD_FACTION("protection.cannotBuildFaction"),
    PROTECTION_CANNOT_DESTROY_FACTION("protection.cannotDestroyFaction"),
    PROTECTION_CANNOT_EQUIP_FACTION("protection.cannotEquipFaction"),
    PROTECTION_CANNOT_LEASH_FACTION("protection.cannotLeashFaction"),
    PROTECTION_CANNOT_SHEAR_FACTION("protection.cannotShearFaction"),
    PROTECTION_CANNOT_SPLASH_POTION_FACTION("protection.cannotSplashPotionFaction"),
    PROTECTION_CANNOT_TAME_FACTION("protection.cannotTameFaction"),
    PROTECTION_CANNOT_UNLEASH_FACTION("protection.cannotUnleashFaction"),
    PROTECTION_WAR_ZONE_IS_CLOSED("protection.warZoneIsClosed"),

    REGION_ALLIANCE_CITY("region.allianceCity"),
    REGION_BARREN("region.barren"),
    REGION_CAPITAL("region.capital"),
    REGION_DESERT("region.desert"),
    REGION_FARMLAND("region.farmland"),
    REGION_FOREST("region.forest"),
    REGION_MAGIC("region.magic"),
    REGION_MOUNTAINOUS("region.mountainous"),
    REGION_PVE("region.pve"),
    REGION_SEA("region.sea"),
    REGION_WAR_ZONE("region.warZone"),

    UI_REGION_DISPLAY_NAME("ui.region.displayName"),
    UI_WAR_OBJECTIVE_OCCUPY_CONTESTED("ui.warObjective.occupyContested"),
    UI_WAR_OBJECTIVE_OCCUPY_CONTESTED_OTHER("ui.warObjective.occupyContestedOther"),
    UI_WAR_OBJECTIVE_OCCUPY_PROGRESS("ui.warObjective.occupyProgress"),
    UI_WAR_OBJECTIVE_OCCUPY_PROGRESS_OTHER("ui.warObjective.occupyProgressOther"),
    UI_WAR_OBJECTIVE_OCCUPY_NEUTRAL("ui.warObjective.occupyNeutral"),
    UI_WAR_OBJECTIVE_OCCUPIED("ui.warObjective.occupied"),
    UI_WAR_OBJECTIVE_OCCUPIED_CONTESTED("ui.warObjective.occupiedContested"),
    UI_WAR_OBJECTIVE_OCCUPIED_CONTESTED_OTHER("ui.warObjective.occupiedContestedOther"),

    WAR_END_RANKING("war.end.ranking"),
    WAR_END_WINNER("war.end.winner"),
    WAR_OBJECTIVE_DESYTROYED("war.objective.destroyed"),
    WAR_OBJECTIVE_DESYTROYED_BY_PLAYER("war.objective.destroyedByPlayer"),
    WAR_REGION_OCCUPIED("war.region.occupied"),
    WAR_PHASE_ACTIVE_ANNOUNCEMENT("war.phase.active.announcement"),
    WAR_PHASE_ACTIVE_DISPLAY_NAME("war.phase.active.displayName"),
    WAR_PHASE_ANNOUNCEMENT_MINUTE("war.phase.announcement.minute"),
    WAR_PHASE_ANNOUNCEMENT_MINUTES("war.phase.announcement.minutes"),
    WAR_PHASE_ANNOUNCEMENT_SECOND("war.phase.announcement.second"),
    WAR_PHASE_ANNOUNCEMENT_SECONDS("war.phase.announcement.seconds"),
    WAR_PHASE_CAPITAL_ANNOUNCEMENT("war.phase.capital.announcement"),
    WAR_PHASE_CAPITAL_DISPLAY_NAME("war.phase.capital.displayName"),
    WAR_PHASE_HOVER("war.phase.hover"),
    WAR_PHASE_REGULAR_ANNOUNCEMENT("war.phase.regular.announcement"),
    WAR_PHASE_REGULAR_DISPLAY_NAME("war.phase.regular.displayName"),
    WAR_PHASE_SCORING_ANNOUNCEMENT("war.phase.scoring.announcement"),
    WAR_PHASE_SCORING_DISPLAY_NAME("war.phase.scoring.displayName"),
    WAR_PHASE_PEACE_ANNOUNCEMENT("war.phase.peace.announcement"),
    WAR_PHASE_PEACE_DISPLAY_NAME("war.phase.peace.displayName"),
    WAR_PHASE_UNDEFINED_DISPLAY_NAME("war.phase.undefined.displayName"),
    WAR_PHASE_UNDEFINED_ANNOUNCEMENT("war.phase.undefined.announcement"),
    WAR_PREFIX("war.prefix"),
    ;

    private final String path;

    FMessage(String path) {
        this.path = path;
    }

    /* Getters */

    public @NotNull Component itemMessage() {
        return Component.text().decoration(TextDecoration.ITALIC, false).append(message()).build();
    }

    public @NotNull Component itemMessage(String... args) {
        return Component.text().decoration(TextDecoration.ITALIC, false).append(message(args)).build();
    }

    public @NotNull Component itemMessage(Component... args) {
        return Component.text().decoration(TextDecoration.ITALIC, false).append(message(args)).build();
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public MessageHandler getMessageHandler() {
        return Factions.get().getMessageHandler();
    }

    @Override
    public Component message() {
        return translatable();
    }

    @Override
    public Component message(String... args) {
        Component[] components = new Component[args.length];
        for (int i = 0; i < args.length; i++) {
            components[i] = Component.text(args[i]);
        }
        return translatable(components);
    }

    @Override
    public Component message(ComponentLike... args) {
        return translatable(args);
    }


}
