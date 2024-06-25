package de.erethon.factions.war;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Fyreum
 */
public class WarPhaseStage {

    private final long duration;
    private final long previousDurations;
    private final WarPhase warPhase;
    private WarPhaseStage nextStage;

    public WarPhaseStage(long duration, long previousDurations, @NotNull WarPhase warPhase) {
        this.duration = duration;
        this.warPhase = warPhase;
        this.previousDurations = previousDurations;
    }

    public WarPhaseStage(@NotNull ConfigurationSection section, long previousDurations) {
        this.duration = section.getLong("duration", -1);
        assert duration >= 0 : "Illegal duration found: " + duration;
        this.previousDurations = previousDurations;
        this.warPhase = WarPhase.valueOf(section.getString("phase").toUpperCase());
        ConfigurationSection nextSection = section.getConfigurationSection("next");
        if (nextSection != null) {
            nextStage = new WarPhaseStage(nextSection, previousDurations + duration);
        }
    }

    public long getDuration() {
        return duration;
    }

    public long getPreviousDurations() {
        return previousDurations;
    }

    public long getFullDuration() {
        return previousDurations + duration;
    }

    public @NotNull WarPhase getWarPhase() {
        return warPhase;
    }

    public @Nullable WarPhaseStage getNextStage() {
        return nextStage;
    }

    public void setNextStage(@Nullable WarPhaseStage nextStage) {
        this.nextStage = nextStage;
    }

    public @NotNull WarPhaseStage getLastWarPhaseStage() {
        return nextStage == null ? this : nextStage.getLastWarPhaseStage();
    }
}
