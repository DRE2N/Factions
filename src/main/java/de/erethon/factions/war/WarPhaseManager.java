package de.erethon.factions.war;

import de.erethon.aergia.util.TickUtil;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.config.EConfig;
import de.erethon.factions.Factions;
import de.erethon.factions.event.WarPhaseChangeEvent;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.util.FUtil;
import de.erethon.factions.war.task.PhaseSwitchTask;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    private BukkitTask runningTask;
    private boolean debugMode = false;

    public WarPhaseManager(@NotNull File file) {
        super(file, CONFIG_VERSION);
        initializeMidnight();
        initialize();
    }

    private void initializeMidnight() {
        ZonedDateTime now = FUtil.getMidnightDateTime();
        // If we're within the first hour, treat it as part of the previous day.
        midnight = (now.getHour() < 1) ? now.minusDays(1) : now;
    }

    private long getCurrentProgress() {
        return System.currentTimeMillis() - midnight.toInstant().toEpochMilli();
    }

    private void rollOverDay() {
        midnight = midnight.plusDays(1);
        if (midnight.getDayOfWeek() == DayOfWeek.MONDAY) {
            currentWeek++;
            if (currentWeek > schedule.size()) {
                currentWeek = 1;
            }
        }
    }

    public void updateCurrentStage() {
        MessageUtil.log("Updating current war phase stage...");
        cancelRunningTask();

        ZonedDateTime currentTime = ZonedDateTime.now(midnight.getZone());
        if (currentTime.isAfter(midnight.plusDays(1))) {
            // Day has passed; roll over to the next day.
            rollOverDay();
            currentStage = getFirstStageOfTheDay();
        } else {
            WarPhaseStage nextStage = (currentStage != null) ? currentStage.getNextStage() : null;
            if (nextStage == null) {
                // End of the day reached.
                rollOverDay();
                nextStage = getFirstStageOfTheDay();
            }
            transitionToNewPhase(nextStage);
        }

        scheduleNextPhaseSwitch();
        saveData(); // Persist state changes
    }


    private void scheduleNextPhaseSwitch() {
        if (debugMode) {
            MessageUtil.log("Debug mode active. Skipping phase switch.");
            return;
        }

        WarPhaseStage nextStage = getNextWarPhaseStage();
        long delay = nextStage.getDelay();

        if (delay <= 0) {
            FLogger.WARN.log("Invalid phase switch delay: " + delay + ". Using 1 minute fallback.");
            delay = TickUtil.MINUTE;
        }

        try {
            MessageUtil.log("Scheduling phase switch to " + nextStage.getWarPhase().name() + " in " + delay + " ticks.");
            runningTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this::updateCurrentStage, delay);
        } catch (Exception e) {
            FLogger.ERROR.log("Failed to schedule phase switch task: " + e.getMessage());
            Bukkit.getScheduler().runTaskLater(plugin, this::updateCurrentStage, TickUtil.MINUTE);
        }
    }

    private void transitionToNewPhase(WarPhaseStage nextStage) {
        final WarPhase currentPhase = currentStage.getWarPhase();
        final WarPhase nextPhase = nextStage.getWarPhase();

        if (currentPhase != nextPhase) {
            currentPhase.onChangeTo(nextPhase);
            new WarPhaseChangeEvent(currentPhase, nextPhase).callEvent();
            nextPhase.announce();
        }
        currentStage = nextStage;
    }

    private void initializeStage() {
        long currentProgress = getCurrentProgress();
        currentStage = getFirstStageOfTheDay();

        // Validate and find the correct stage based on the current progress.
        while (currentStage != null && currentStage.getFullDuration() < currentProgress) {
            WarPhaseStage nextStage = currentStage.getNextStage();
            MessageUtil.log("Skipping stage " + currentStage.getWarPhase().name() + " with duration " + currentStage.getFullDuration());
            if (nextStage == null) {
                MessageUtil.log("End of day reached. Moving to next day...");
                rollOverDay();
                currentStage = getFirstStageOfTheDay();
                // Recalculate progress for the new day.
                currentProgress = getCurrentProgress();
            } else {
                currentStage = nextStage;
                MessageUtil.log("Moving to next stage " + currentStage.getWarPhase().name() + " with duration " + currentStage.getFullDuration());
            }
        }

        if (currentStage == null) {
            FLogger.ERROR.log("Failed to initialize war phase stage. Using PEACE phase.");
            currentStage = new WarPhaseStage(DAY_DURATION, 0, WarPhase.PEACE);
        }

        WarPhase.UNDEFINED.onChangeTo(currentStage.getWarPhase());
        new WarPhaseChangeEvent(WarPhase.UNDEFINED, currentStage.getWarPhase()).callEvent();
    }

    private WarPhaseStage getFirstStageOfTheDay() {
        if (schedule.get(currentWeek) == null) {
            currentWeek = 1;
        }
        return schedule.get(currentWeek).get(midnight.getDayOfWeek().getValue());
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
        validateSchedule();
    }

    private void validateSchedule() {
        if (schedule.isEmpty()) {
            FLogger.ERROR.log("War schedule is empty. Setting up inactive schedule...");
            setupInactiveSchedule();
            return;
        }

        int maxWeek = schedule.keySet().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(7);

        for (int week = 1; week <= maxWeek; week++) {
            Map<Integer, WarPhaseStage> days = schedule.computeIfAbsent(week, k -> new HashMap<>());

            for (int day = 1; day <= 7; day++) {
                if (!days.containsKey(day)) {
                    days.put(day, new WarPhaseStage(DAY_DURATION, 0, WarPhase.PEACE));
                }
            }
        }

        for (Map<Integer, WarPhaseStage> days : schedule.values()) {
            for (Map.Entry<Integer, WarPhaseStage> entry : days.entrySet()) {
                WarPhaseStage stage = entry.getValue();
                long fullDuration = stage.getLastWarPhaseStage().getFullDuration();

                if (fullDuration > DAY_DURATION) {
                    FLogger.ERROR.log("Duration exceeds day length in week/day " + entry.getKey() + ". Using PEACE phase.");
                    days.put(entry.getKey(), new WarPhaseStage(DAY_DURATION, 0, WarPhase.PEACE));
                } else if (fullDuration < DAY_DURATION) {
                    FLogger.WARN.log("Duration is shorter than day length in week/day " + entry.getKey() + ". Adding PEACE phase.");
                    stage.getLastWarPhaseStage().setNextStage(
                            new WarPhaseStage(DAY_DURATION - fullDuration, fullDuration, WarPhase.PEACE)
                    );
                }
            }
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
                long fullDuration = lastStage.getLastWarPhaseStage().getFullDuration();

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
        if (currentStage == null) { // TODO: Quick fix for NPE
            return WarPhase.PEACE;
        }
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

    public @Nullable BukkitTask getRunningTask() {
        return runningTask;
    }

    public void setRunningTask(@Nullable BukkitTask runningTask) {
        this.runningTask = runningTask;
    }

    public void cancelRunningTask()  {
        if (runningTask != null) {
            runningTask.cancel();
            MessageUtil.log("Cancelled running warphase task.");
            runningTask = null;
        }
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void debugWarPhase(@NotNull WarPhase warPhase) {
        this.debugMode = true;
        cancelRunningTask();
        WarPhase oldPhase = currentStage.getWarPhase();
        oldPhase.onChangeTo(warPhase);
        this.currentStage = new WarPhaseStage(DAY_DURATION, 0, warPhase);
        this.currentStage.setNextStage(currentStage);
        warPhase.announce();
    }

    public void disableDebugMode() {
        this.debugMode = false;
        this.currentStage = null;
        updateCurrentStage();
    }
}
