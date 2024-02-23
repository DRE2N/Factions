package de.erethon.factions.war;

import de.erethon.aergia.util.TickUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.poll.polls.CapturedRegionsPoll;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionCache;
import de.erethon.factions.region.RegionType;
import de.erethon.factions.util.FBroadcastUtil;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.war.objective.WarObjective;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author Fyreum
 */
public enum WarPhase {

    CAPITAL(FMessage.WAR_PHASE_CAPITAL_DISPLAY_NAME, FMessage.WAR_PHASE_CAPITAL_ANNOUNCEMENT),
    REGULAR(FMessage.WAR_PHASE_REGULAR_DISPLAY_NAME, FMessage.WAR_PHASE_REGULAR_ANNOUNCEMENT),
    SCORING(FMessage.WAR_PHASE_SCORING_DISPLAY_NAME, FMessage.WAR_PHASE_SCORING_ANNOUNCEMENT),
    PEACE(FMessage.WAR_PHASE_PEACE_DISPLAY_NAME, FMessage.WAR_PHASE_PEACE_ANNOUNCEMENT);

    private final Factions plugin = Factions.get();
    private final FMessage displayName, announcementMessage;

    WarPhase(@NotNull FMessage displayName, @NotNull FMessage announcementMessage) {
        this.displayName = displayName;
        this.announcementMessage = announcementMessage;
    }

    public void onChangeTo(WarPhase nextPhase) {
        if (isInfluencingScoring() && !nextPhase.isInfluencingScoring()) {
            // scoring: true -> false
            onScoringClose();
        }
        if (isAllowPvP() != nextPhase.isAllowPvP()) {
            if (isAllowPvP()) {
                // PvP: true -> false
                foreachWarObjective(obj -> true, WarObjective::deactivate);
            } else {
                // PvP: false -> true
                foreachWarObjective(obj -> obj.getRegion().getType() != RegionType.CAPITAL, WarObjective::activate);
            }
        }
        if (isOpenCapital() != nextPhase.isOpenCapital()) {
            if (isOpenCapital()) {
                // openCapital: true -> false
                onWarEnd();
            } else {
                // openCapital: false -> true
                foreachWarObjective(obj -> obj.getRegion().getType() != RegionType.CAPITAL, WarObjective::activate);
            }
        }
    }

    public void initialize() {
        if (isAllowPvP()) {
            foreachWarObjective(obj -> obj.getRegion().getType() != RegionType.CAPITAL, WarObjective::activate);
        }
        if (isOpenCapital()) {
            foreachWarObjective(obj -> obj.getRegion().getType() != RegionType.CAPITAL, WarObjective::activate);
        }
    }

    private void foreachWarObjective(Predicate<WarObjective> filter, Consumer<WarObjective> consumer) {
        for (RegionCache cache : plugin.getRegionManager()) {
            for (Region region : cache) {
                Map<String, WarObjective> structures = region.getStructures(WarObjective.class);
                structures.forEach((name, obj) -> {
                    if (filter.test(obj)) {
                        consumer.accept(obj);
                    }
                });
            }
        }
    }

    // Called after the SCORING phase has ended
    private void onScoringClose() {
        FLogger.WAR.log("Awarding alliances relative to their captured regions...");
        for (Alliance alliance : plugin.getAllianceCache()) {
            for (Region region : alliance.getUnconfirmedTemporaryRegions()) {
                alliance.addWarScore(region.getRegionalWarTracker().getRegionValue());
            }
        }
    }

    // Called after the CAPITAL phase has ended
    private void onWarEnd() {
        // Calculate remaining winners for each region.
        for (RegionCache cache : plugin.getRegionManager()) {
            for (Region region : cache) {
                if (region.getType() != RegionType.WAR_ZONE) {
                    continue;
                }
                Alliance winner = region.getRegionalWarTracker().getLeader();
                Alliance rAlliance = region.getAlliance();
                if (winner != null) {
                    winner.temporaryOccupy(region);
                    continue;
                }
                if (rAlliance != null) {
                    FLogger.WAR.log("Region '" + region.getId() + "' is no longer held by alliance '" + rAlliance + "'");
                    region.setAlliance(null);
                }
            }
        }
        // Get the overall war winning alliance.
        List<Alliance> ranked = plugin.getAllianceCache().ranked();
        if (ranked.isEmpty()) {
            return;
        }
        Alliance winner = ranked.get(0);
        // Open alliance polls & store new WarHistory entry
        Map<Integer, Double> scores = new HashMap<>(plugin.getAllianceCache().getSize());
        for (Alliance alliance : plugin.getAllianceCache()) {
            scores.put(alliance.getId(), alliance.getWarScore());
            alliance.setCurrentEmperor(false);
            alliance.setWarScore(0);
            alliance.addPoll(new CapturedRegionsPoll(alliance), TickUtil.DAY);
        }
        plugin.getWarHistory().storeEntry(System.currentTimeMillis(), scores);

        winner.setCurrentEmperor(true);
        for (Alliance current : ranked) {
            FBroadcastUtil.broadcastWar(FMessage.WAR_END_RANKING, current.getColoredLongName(), Component.text(current.getWarScore()));
        }
        FBroadcastUtil.broadcastWar(Component.empty());
        FBroadcastUtil.broadcastWar(FMessage.WAR_END_WINNER, winner.getColoredLongName());
    }

    public void announce() {
        FBroadcastUtil.broadcastWar(announcementMessage);
    }

    /* Getters */

    public boolean isAllowPvP() {
        return this != PEACE;
    }

    public boolean isOpenCapital() {
        return this == CAPITAL;
    }

    public boolean isInfluencingScoring() {
        return this == SCORING || this == CAPITAL;
    }

    public @NotNull FMessage getDisplayName() {
        return displayName;
    }

    public @NotNull FMessage getAnnouncementMessage() {
        return announcementMessage;
    }

}
