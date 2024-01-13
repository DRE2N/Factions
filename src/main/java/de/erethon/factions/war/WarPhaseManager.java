package de.erethon.factions.war;

import de.erethon.aergia.util.DateUtil;
import de.erethon.aergia.util.TickUtil;
import de.erethon.bedrock.config.EConfig;
import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.poll.polls.CapturedRegionsPoll;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionCache;
import de.erethon.factions.region.RegionType;
import de.erethon.factions.util.FBroadcastUtil;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.util.FUtil;
import de.erethon.factions.war.objective.WarObjective;
import de.erethon.factions.war.task.PhaseSwitchTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * @author Fyreum
 */
public class WarPhaseManager extends EConfig {

    public static final int CONFIG_VERSION = 1;

    private static final long DAY_DURATION = TimeUnit.DAYS.toMillis(1);

    final Factions plugin = Factions.get();

    private int currentWeek = 1;
    private WarPhaseStage currentStage;
    private ZonedDateTime midnight;
    private final Map<Integer, Map<Integer, WarPhaseStage>> schedule = new HashMap<>();
    private final LinkedList<BukkitTask> runningTasks = new LinkedList<>();

    public WarPhaseManager(@NotNull File file) {
        super(file, CONFIG_VERSION);
        midnight = FUtil.getMidnightDateTime();
        initialize();
    }

    public void updateCurrentStageTask() {
        updateCurrentStage();
        long delay = currentStage.getFullDuration() - (System.currentTimeMillis() - midnight.toInstant().toEpochMilli());
        int minutes = 5;
        // Schedule next update task.
        runningTasks.add(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin,
                () -> new PhaseSwitchTask(this, minutes).start(),
                (delay - TimeUnit.MINUTES.toMillis(minutes)) / 50 + TickUtil.SECOND) // 1 second puffer
        );
        FLogger.WAR.log("Current war phase stage: " + currentStage.getWarPhase() + ", remaining duration: " + DateUtil.formatDateDiff(System.currentTimeMillis() + delay));
    }

    void updateCurrentStage() {
        // Cancel previous tasks.
        if (!runningTasks.isEmpty()) {
            Iterator<BukkitTask> iterator = runningTasks.iterator();
            while (iterator.hasNext()) {
                iterator.next().cancel();
                iterator.remove();
            }
        }
        // Initialize or progress the current stage.
        if (currentStage == null) {
            initializeStage();
            return;
        }
        WarPhaseStage nextStage = currentStage.getNextStage();
        if (nextStage == null) { // the current day schedule appears to have ended.
            incrementCurrentDay();
        }
        if (currentStage.getWarPhase() != nextStage.getWarPhase()) {
            updateWarState(nextStage);
            FBroadcastUtil.broadcastWar(currentStage.getWarPhase().getAnnouncementMessage());
        }
        currentStage = nextStage;
    }

    private void initializeStage() {
        currentStage = schedule.get(currentWeek).get(midnight.getDayOfWeek().getValue());
        long currentProgress = System.currentTimeMillis() - midnight.toInstant().toEpochMilli();

        while (currentStage != null && currentStage.getFullDuration() < currentProgress) {
            currentStage = currentStage.getNextStage();
        }
    }

    void incrementCurrentDay() {
        midnight = midnight.plusDays(1);
        // Increase week count on each monday & reset week count if no scheduled weeks left.
        if (midnight.getDayOfWeek() == DayOfWeek.MONDAY && ++currentWeek > schedule.size()) {
            currentWeek = 1;
        }
        currentStage = schedule.get(currentWeek).get(midnight.getDayOfWeek().getValue());
    }

    private void updateWarState(WarPhaseStage nextStage) {
        if (currentStage.getWarPhase().isInfluencingScoring() && !nextStage.getWarPhase().isInfluencingScoring()) {
            onScoringClose();
        }
        if (currentStage.getWarPhase().isAllowPvP()) {
            if (!nextStage.getWarPhase().isAllowPvP()) {
                deactivateObjectives(obj -> true);
            }
        } else if (nextStage.getWarPhase().isAllowPvP()) {
            activateObjectives(obj -> !obj.isCapitalObjective());
        }
        if (currentStage.getWarPhase().isOpenCapital()) {
            if (!nextStage.getWarPhase().isOpenCapital()) {
                onWarEnd();
            }
        } else if (nextStage.getWarPhase().isOpenCapital()) {
            activateObjectives(WarObjective::isCapitalObjective);
        }
    }

    private void activateObjectives(Predicate<WarObjective> filter) {
        for (RegionCache cache : plugin.getRegionManager().getCaches().values()) {
            for (Region region : cache) {
                Map<String, WarObjective> structures = region.getStructures(WarObjective.class);
                structures.forEach((name, obj) -> {
                    if (filter.test(obj)) {
                        obj.activate();
                    }
                });
            }
        }
    }

    private void deactivateObjectives(Predicate<WarObjective> filter) {
        for (RegionCache cache : plugin.getRegionManager().getCaches().values()) {
            for (Region region : cache) {
                Map<String, WarObjective> structures = region.getStructures(WarObjective.class);
                structures.forEach((name, obj) -> {
                    if (filter.test(obj)) {
                        obj.deactivate();
                    }
                });
            }
        }
    }

    private void onScoringClose() {
        FLogger.WAR.log("Awarding alliances relative to their captured regions...");
        for (Alliance alliance : plugin.getAllianceCache()) {
            for (Region region : alliance.getUnconfirmedTemporaryRegions()) {
                alliance.addWarScore(region.getRegionalWarTracker().getRegionValue());
            }
        }
    }

    private void onWarEnd() {
        // Calculate remaining winners for each region.
        for (RegionCache cache : plugin.getRegionManager().getCaches().values()) {
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

    /* Serialization */

    @Override
    public void initialize() {
        initValue("currentWeek", currentWeek);
        save();
    }

    @Override
    public void load() {
        currentWeek = config.getInt("currentWeek");
        try {
            ConfigurationSection scheduleSection = config.getConfigurationSection("schedule");
            if (scheduleSection == null) {
                FLogger.WAR.log("No war schedule found. Disabling war cycle...");
                setupInactiveSchedule();
                return;
            }
            for (String week : scheduleSection.getKeys(false)) {
                List<Integer> weeks = getSeparatedValues(week);
                ConfigurationSection weekSection = scheduleSection.getConfigurationSection(week);
                assert weekSection != null : "Section for week '" + week + "' not found.";
                for (int w : weeks) {
                    loadWeek(w, weekSection);
                }
            }
        } catch (Exception e) {
            FLogger.ERROR.log("Couldn't load WarPhaseManager: ");
            e.printStackTrace();
            FLogger.WAR.log("Disabling war cycle...");
            setupInactiveSchedule();
        }
    }

    private void setupInactiveSchedule() {
        for (int week = 1; week < 8; week++) {
            Map<Integer, WarPhaseStage> staleStages = new HashMap<>(7);
            for (int day = 1; day < 8; day++) {
                staleStages.put(day, new WarPhaseStage(DAY_DURATION, 0, WarPhase.PEACE));
            }
            schedule.put(week, staleStages);
        }
    }

    private void loadWeek(int week, ConfigurationSection section) {
        schedule.putIfAbsent(week, new HashMap<>());
        for (String key : section.getKeys(false)) {
            List<Integer> days = getSeparatedValues(key);
            ConfigurationSection daySection = section.getConfigurationSection(key);
            assert daySection != null : "Section for week '" + week + "' day '" + key + "' not found.";

            for (int day : days) {
                WarPhaseStage warPhaseStage = new WarPhaseStage(daySection, 0);
                WarPhaseStage lastStage = warPhaseStage.getLastWarPhaseStage();
                long fullDuration = lastStage.getScheduleDuration();

                if (fullDuration > DAY_DURATION) {
                    FLogger.ERROR.log("Duration of week " + week + " day " + day + " is too long. Inactive stage will be used instead...");
                    warPhaseStage = new WarPhaseStage(DAY_DURATION, 0, WarPhase.PEACE);
                } else if (fullDuration < DAY_DURATION) {
                    FLogger.WARN.log("Duration of week " + week + " day " + day + " is too short. Inactive stage will be used additionally...");
                    lastStage.setNextStage(new WarPhaseStage(DAY_DURATION - fullDuration, fullDuration, WarPhase.PEACE));
                }
                schedule.get(week).put(day, warPhaseStage);
            }
        }
    }

    private List<Integer> getSeparatedValues(String key) {
        List<Integer> result = new ArrayList<>();
        if (key.contains("-")) {
            String[] split = key.split("-");
            int start = Integer.parseInt(split[0]);
            int end = Integer.parseInt(split[1]);
            int max = Math.max(start, Math.max(end, 7));
            result.add(start); // Add the first value

            while (start != end) {
                if (++start > max) {
                    start -= max;
                }
                result.add(start);
            }
        } else {
            result.add(Integer.parseInt(key));
        }
        return result;
    }

    public void saveData() {
        config.set("currentWeek", currentWeek);
        save();
    }

    /* Getters and setters */

    public @NotNull WarPhaseStage getCurrentWarPhaseStage() {
        return currentStage;
    }

    public @NotNull WarPhase getCurrentWarPhase() {
        return currentStage.getWarPhase();
    }

    public @NotNull WarPhaseStage getNextWarPhaseStage() {
        return currentStage.getNextStage() == null ? getNextDayWarPhaseStage() : currentStage.getNextStage();
    }

    public @NotNull WarPhase getNextWarPhase() {
        return getNextWarPhaseStage().getWarPhase();
    }

    public @NotNull WarPhaseStage getNextDayWarPhaseStage() {
        int week = currentWeek;
        ZonedDateTime nextDay = midnight.plusDays(1);
        if (nextDay.getDayOfWeek() == DayOfWeek.MONDAY && ++week > schedule.size()) {
            week = 1;
        }
        return schedule.get(week).get(nextDay.getDayOfWeek().getValue());
    }

    public @NotNull WarPhase getNextDayWarPhase() {
        return getNextDayWarPhaseStage().getWarPhase();
    }

    public @NotNull LinkedList<BukkitTask> getRunningTasks() {
        return runningTasks;
    }
}
