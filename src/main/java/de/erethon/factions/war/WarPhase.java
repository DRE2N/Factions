package de.erethon.factions.war;

import de.erethon.aergia.util.TickUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.poll.polls.CapturedRegionsPoll;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionCache;
import de.erethon.factions.region.RegionType;
import de.erethon.factions.region.WarRegion;
import de.erethon.factions.util.FBroadcastUtil;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.war.structure.WarFortressStructure;
import de.erethon.factions.war.structure.WarStructure;
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

    CAPITAL(FMessage.WAR_PHASE_CAPITAL_DISPLAY_NAME, FMessage.WAR_PHASE_CAPITAL_ANNOUNCEMENT, true, true, false, true, true),
    TRUCE(FMessage.WAR_PHASE_TRUCE_DISPLAY_NAME, FMessage.WAR_PHASE_TRUCE_ANNOUNCEMENT, true, false, true, false, false),
    SCORING(FMessage.WAR_PHASE_SCORING_DISPLAY_NAME, FMessage.WAR_PHASE_SCORING_ANNOUNCEMENT, true, true, false, true, false),
    PEACE(FMessage.WAR_PHASE_PEACE_DISPLAY_NAME, FMessage.WAR_PHASE_PEACE_ANNOUNCEMENT, false, false, false, false, false),
    UNDEFINED(FMessage.WAR_PHASE_UNDEFINED_DISPLAY_NAME, FMessage.WAR_PHASE_UNDEFINED_ANNOUNCEMENT, false, false, false, false, false); // do not use as an actual phase

    private final Factions plugin = Factions.get();
    private final FMessage displayName, announcementMessage;
    private final boolean allowPvP;
    private final boolean allowCapture;
    private final boolean allowRuinBuilding;
    private final boolean influencingScoring;
    private final boolean openCapital;

    WarPhase(@NotNull FMessage displayName, @NotNull FMessage announcementMessage, boolean allowPvP, boolean allowCapture,
             boolean allowRuinBuilding, boolean influencingScoring, boolean openCapital) {
        this.displayName = displayName;
        this.announcementMessage = announcementMessage;
        this.allowPvP = allowPvP;
        this.allowCapture = allowCapture;
        this.allowRuinBuilding = allowRuinBuilding;
        this.influencingScoring = influencingScoring;
        this.openCapital = openCapital;
    }

    public void onChangeTo(WarPhase nextPhase) {
        if (isInfluencingScoring() && !nextPhase.isInfluencingScoring()) {
            // scoring: true -> false
            onScoringClose();
        }
        if (isAllowPvP() != nextPhase.isAllowPvP()) {
            if (isAllowPvP()) {
                // PvP: true -> false
                foreachWarObjective(obj -> true, WarStructure::deactivate);
            } else {
                // PvP: false -> true
                foreachWarObjective(obj -> obj.getRegion().getType() != RegionType.CAPITAL, WarStructure::activate);
            }
        }
        if (isOpenCapital() != nextPhase.isOpenCapital()) {
            if (isOpenCapital()) {
                // openCapital: true -> false
                onWarEnd();
            } else {
                // openCapital: false -> true
                foreachWarObjective(obj -> obj.getRegion().getType() == RegionType.CAPITAL, WarStructure::activate);
            }
        }
    }

    private void foreachWarObjective(Predicate<WarStructure> filter, Consumer<WarStructure> consumer) {
        for (RegionCache cache : plugin.getRegionManager()) {
            for (Region region : cache) {
                if (!(region instanceof WarRegion warRegion)) {
                    continue;
                }
                Map<String, WarStructure> structures = warRegion.getStructures(WarStructure.class);
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
        FLogger.WAR.log("Awarding daily victory points from current WvW score...");
        if (plugin.getWar() != null && plugin.getWar().getScore() != null) {
            plugin.getWar().getScore().awardDailyVictoryPoints();
        }
    }

    // Called after the CAPITAL phase has ended
    private void onWarEnd() {
        // Remove every occupied region that is not a fortress
        for (RegionCache cache : plugin.getRegionManager()) {
            for (Region region : cache) {
                if (region.getType() != RegionType.WAR_ZONE) {
                    continue;
                }
                if (!(region instanceof WarRegion warRegion)) {
                    continue;
                }
                warRegion.getRegionalWarTracker().clearCycleState();

                if (!region.hasAlliance() || !warRegion.getStructures(WarFortressStructure.class).isEmpty()) {
                    continue;
                }
                if (region.hasAlliance()) {
                    region.getAlliance().removeTemporaryRegion(region);
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
            if (alliance.getTemporaryRegions().size() > plugin.getFConfig().getWarMaximumOccupiedRegions()) {
                alliance.addPoll(new CapturedRegionsPoll(alliance), TickUtil.DAY);
            }
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
        FBroadcastUtil.broadcastWar(announcementMessage.message()
                .hoverEvent(FMessage.WAR_PHASE_HOVER.message(
                        boolToComponent(isAllowPvP()),
                        boolToComponent(isInfluencingScoring()),
                        boolToComponent(isOpenCapital()))
                )
        );
    }

    private Component boolToComponent(boolean bool) {
        return (bool ? FMessage.GENERAL_YES : FMessage.GENERAL_NO).message();
    }

    /* Getters */

    public boolean isAllowPvP() {
        return allowPvP;
    }

    public boolean isAllowCapture() {
        return allowCapture;
    }

    public boolean isAllowRuinBuilding() {
        return allowRuinBuilding;
    }

    public boolean isOpenCapital() {
        return openCapital;
    }

    public boolean isInfluencingScoring() {
        return influencingScoring;
    }

    public @NotNull FMessage getDisplayName() {
        return displayName;
    }

    public @NotNull FMessage getAnnouncementMessage() {
        return announcementMessage;
    }

}
