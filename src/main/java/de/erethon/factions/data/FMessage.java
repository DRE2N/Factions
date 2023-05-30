package de.erethon.factions.data;

import de.erethon.bedrock.config.Message;
import de.erethon.bedrock.config.MessageHandler;
import de.erethon.factions.Factions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public enum FMessage implements Message {

    ALLIANCE_INFO_NEW_POLL("alliance.info.newPoll"),
    ALLIANCE_INFO_PREFIX("alliance.info.prefix"),

    ACM_ADDED_CHUNK("acm.addedChunk"),
    ACM_ADDED_CHUNKS("acm.addedChunks"),
    ACM_ADD_INSIDE_REGION("acm.addInsideRegion"),
    ACM_ILLEGAL_RADIUS("acm.illegalRadius"),
    ACM_PREFIX("acm.prefix"),
    ACM_NOT_SELECTED("acm.notSelected"),
    ACM_RADIUS_SELECTED("acm.radiusSelected"),
    ACM_REMOVED_CHUNK("acm.removedChunk"),
    ACM_REMOVED_CHUNKS("acm.removedChunks"),
    ACM_SELECTED("acm.selected"),
    ACM_UNSELECTED("acm.unselected"),

    BUILDING_SITE_CREATED("buildingSite.created"),

    CMD_ALLIANCE_CHOOSE_ON_COOLDOWN("cmd.alliance.choose.onCooldown"),
    CMD_ALLIANCE_CHOOSE_SUCCESS("cmd.alliance.choose.success"),
    CMD_DISBAND_CONFIRMATION_REQUIRED("cmd.disband.confirmationRequired"),
    CMD_JOIN_NOT_INVITED("cmd.join.notInvited"),
    CMD_JOIN_ON_COOLDOWN("cmd.join.onCooldown"),
    CMD_INVITE_SUCCESS("cmd.invite.success"),
    CMD_JOIN_SUCCESS("cmd.join.success"),
    CMD_KICK_SUCCESS("cmd.kick.success"),
    CMD_LEAVE_CONFIRMATION_REQUIRED("cmd.leave.confirmationRequired"),
    CMD_LEAVE_SUCCESS("cmd.leave.success"),
    CMD_OBJECTIVE_CREATE_SUCCESS("cmd.objective.create.success"),
    CMD_REGION_ADD_CHUNK_SUCCESS("cmd.region.addChunk.success"),
    CMD_REGION_ADD_NEIGHBOUR_SUCCESS("cmd.region.addNeighbour.success"),
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
    CMD_REGION_REMOVE_CHUNK_SUCCESS("cmd.region.removeChunk.success"),
    CMD_REGION_REMOVE_NEIGHBOUR_SUCCESS("cmd.region.removeNeighbour.success"),
    CMD_REGION_TYPE_SUCCESS("cmd.region.type.success"),
    CMD_RELOAD_ALL_END("cmd.reload.allEnd"),
    CMD_RELOAD_ALL_START("cmd.reload.allStart"),
    CMD_RELOAD_CACHES("cmd.reload.caches"),
    CMD_RELOAD_CONFIGS("cmd.reload.configs"),
    CMD_RELOAD_MESSAGES("cmd.reload.messages"),
    CMD_SHOW_ADMIN("cmd.show.admin"),
    CMD_SHOW_CORE_REGION("cmd.show.coreRegion"),
    CMD_SHOW_DESCRIPTION("cmd.show.description"),
    CMD_SHOW_HEADER("cmd.show.header"),
    CMD_SHOW_NAME("cmd.show.name"),
    CMD_SHOW_MEMBERS("cmd.show.members"),
    CMD_SHOW_MONEY("cmd.show.money"),
    CMD_SHOW_SEPARATOR("cmd.show.separator"),
    CMD_VERSION_AUTHORS("cmd.version.authors"),
    CMD_VERSION_HEADER("cmd.version.header"),
    CMD_VERSION_STATUS("cmd.version.status"),

    ERROR_BUILDING_BLOCKED("error.buildingBlocked"),
    ERROR_BUILDING_REQUIRED_TYPE("error.buildingRequiredType"),
    ERROR_BUILDING_REQUIRED_FACTION("error.buildingRequiredFaction"),
    ERROR_BUILDING_POPULATION("error.buildingPopulation"),
    ERROR_BUILDING_NOT_ENOUGH_RESOURCES("error.buildingNotEnoughResources"),
    ERROR_CANNOT_KICK_YOURSELF("error.cannotKickYourself"),
    ERROR_CANNOT_UNCLAIM_CORE_REGION("error.cannotUnclaimCoreRegion"),
    ERROR_CHUNK_ALREADY_A_REGION("error.chunkAlreadyARegion"),
    ERROR_FACTION_HAS_NOT_ENOUGH_MONEY("error.factionHasNotEnoughMoney"),
    ERROR_FACTION_NOT_FOUND("error.factionNotFound"),
    ERROR_NAME_IN_USE("error.nameInUse"),
    ERROR_NAME_IS_FORBIDDEN("error.nameIsForbidden"),
    ERROR_NO_PERMISSION("error.noPermission"),
    ERROR_NOT_ENOUGH_MONEY("error.notEnoughMoney"),
    ERROR_NOT_PARSABLE_DOUBLE("error.notParsableDouble"),
    ERROR_NOT_PARSABLE_INTEGER("error.notParsableInteger"),
    ERROR_PLAYER_ALREADY_IN_A_FACTION("error.playerAlreadyInAFaction"),
    ERROR_PLAYER_IS_NOT_IN_A_FACTION("error.playerIsNotInAFaction"),
    ERROR_PLAYER_NOT_FOUND("error.playerNotFound"),
    ERROR_PLAYER_REGION_NULL("error.playerRegionNull"),
    ERROR_POLL_NOT_FOUND("error.pollNotFound"),
    ERROR_REGION_ALREADY_CLAIMED("error.regionAlreadyClaimed"),
    ERROR_REGION_IN_ANOTHER_WORLD("error.regionInAnotherWorld"),
    ERROR_REGION_IS_NOT_A_WARZONE("error.regionIsNotAWarzone"),
    ERROR_REGION_IS_NOT_CLAIMABLE("error.regionIsNotClaimable"),
    ERROR_REGION_NOT_FOUND("error.regionNotFound"),
    ERROR_REGION_TYPE_NOT_FOUND("error.regionTypeNotFound"),
    ERROR_REGIONS_ALREADY_NEIGHBOURS("error.regionsAlreadyNeighbours"),
    ERROR_REGIONS_ARE_NOT_NEIGHBOURS("error.regionsAreNotNeighbours"),
    ERROR_SENDER_IS_NO_PLAYER("error.senderIsNoPlayer"),
    ERROR_TARGET_IS_ALREADY_ADMIN("error.targetIsAlreadyAdmin"),
    ERROR_TARGET_IS_ALREADY_IN_THIS_FACTION("error.targetIsAlreadyInThisFaction"),
    ERROR_TARGET_IS_NOT_IN_A_FACTION("error.targetIsNotInAFaction"),
    ERROR_TARGET_IS_NOT_IN_THIS_FACTION("error.targetIsNotInThisFaction"),
    ERROR_WAR_OBJECTIVE_TYPE_NOT_FOUND("error.warObjectiveNotFound"),
    ERROR_WAR_OBJECTIVE_WRONG_ARG_FORMAT("error.warObjectiveWrongArgFormat"),
    ERROR_WRONG_DOUBLE_VALUE("error.wrongDoubleValue"),

    FACTION_INFO_ADMIN_CHANGED("faction.info.adminChanged"),
    FACTION_INFO_CREATED("faction.info.created"),
    FACTION_INFO_DESCRIPTION_CHANGED("faction.info.descriptionChanged"),
    FACTION_INFO_DISBANDED("faction.info.disbanded"),
    FACTION_INFO_INACTIVE_PLAYER_KICKED("faction.info.inactivePlayerKicked"),
    FACTION_INFO_NAME_CHANGED("faction.info.nameChanged"),
    FACTION_INFO_NEW_POLL("faction.info.newPoll"),
    FACTION_INFO_PLAYER_GOT_KICKED("faction.info.playerGotKicked"),
    FACTION_INFO_PLAYER_JOINED("faction.info.playerJoined"),
    FACTION_INFO_PLAYER_LEFT("faction.info.playerLeft"),
    FACTION_INFO_PREFIX("faction.info.prefix"),
    FACTION_INFO_REGION_CLAIMED("faction.info.regionClaimed"),
    FACTION_INFO_SHORT_NAME_CHANGED("faction.info.shortNameChanged"),
    FACTION_INVITE_ACCEPT("faction.invite.accept"),
    FACTION_INVITE_ACCEPT_HOVER("faction.invite.acceptHover"),
    FACTION_INVITE_DECLINE("faction.invite.decline"),
    FACTION_INVITE_DECLINE_HOVER("faction.invite.declineHover"),
    FACTION_INVITE_MESSAGE("faction.invite.message"),

    GENERAL_DEFAULT_DESCRIPTION("general.defaultDescription"),
    GENERAL_FACTION("general.faction"),
    GENERAL_LONER("general.loner"),
    GENERAL_NONE("general.none"),
    GENERAL_REGION_DEFAULT_NAME_PREFIX("general.regionDefaultNamePrefix"),
    GENERAL_SPECTATOR("general.spectator"),
    GENERAL_WAR_ZONES("general.warZones"),
    GENERAL_WILDERNESS("general.wilderness"),

    GUI_POLL_NEXT_PAGE_INFO("gui.poll.nextPage.info"),
    GUI_POLL_NEXT_PAGE_NAME("gui.poll.nextPage.name"),
    GUI_POLL_PREVIOUS_PAGE_INFO("gui.poll.previousPage.info"),
    GUI_POLL_PREVIOUS_PAGE_NAME("gui.poll.previousPage.name"),
    GUI_POLL_TITLE("gui.poll.title"),
    GUI_POLL_TITLE_CLOSED("gui.poll.titleClosed"),
    GUI_POLL_REGION_TYPE_DISPLAY("gui.poll.regionTypeDisplay"),
    GUI_POLL_VOTES_DISPLAY("gui.poll.votesDisplay"),

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

    REGION_BARREN("region.barren"),
    REGION_CITY("region.city"),
    REGION_DESERT("region.desert"),
    REGION_FARMLAND("region.farmland"),
    REGION_FOREST("region.forest"),
    REGION_MAGIC("region.magic"),
    REGION_MOUNTAINOUS("region.mountainous"),
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

    WAR_OBJECTIVE_DESYTROYED("war.objective.destroyed"),
    WAR_OBJECTIVE_DESYTROYED_BY_PLAYER("war.objective.destroyedByPlayer"),
    WAR_PHASE_ACTIVE_ANNOUNCEMENT("war.phase.active.announcement"),
    WAR_PHASE_ACTIVE_DISPLAY_NAME("war.phase.active.displayName"),
    WAR_PHASE_ANNOUNCEMENT_MINUTE("war.phase.announcement.minute"),
    WAR_PHASE_ANNOUNCEMENT_MINUTES("war.phase.announcement.minutes"),
    WAR_PHASE_ANNOUNCEMENT_SECOND("war.phase.announcement.second"),
    WAR_PHASE_ANNOUNCEMENT_SECONDS("war.phase.announcement.seconds"),
    WAR_PHASE_INACTIVE_ANNOUNCEMENT("war.phase.inactive.announcement"),
    WAR_PHASE_INACTIVE_DISPLAY_NAME("war.phase.inactive.displayName"),
    WAR_PHASE_PASSIVE_ANNOUNCEMENT("war.phase.passive.announcement"),
    WAR_PHASE_PASSIVE_DISPLAY_NAME("war.phase.passive.displayName"),
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
}
