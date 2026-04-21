package de.erethon.factions.blocklog.model;

import java.sql.Timestamp;

/**
 * Tracks the progress of renaturation for an abandoned region.
 *
 * @author Malfrador
 */
public class RenaturationProgress {
    private int regionId;
    private Timestamp startedAt;
    private short currentYLevel;
    private short maxYProcessed;
    private short minYTarget;
    private RenaturationPhase phase;
    private String decayRulesJson;
    private Timestamp lastProcessed;
    private Timestamp estimatedCompletion;

    public RenaturationProgress() {
    }

    public RenaturationProgress(int regionId, short currentYLevel, short maxYProcessed, short minYTarget,
                                RenaturationPhase phase, String decayRulesJson) {
        this.regionId = regionId;
        this.startedAt = new Timestamp(System.currentTimeMillis());
        this.currentYLevel = currentYLevel;
        this.maxYProcessed = maxYProcessed;
        this.minYTarget = minYTarget;
        this.phase = phase;
        this.decayRulesJson = decayRulesJson;
        this.lastProcessed = new Timestamp(System.currentTimeMillis());
    }

    public int getRegionId() {
        return regionId;
    }

    public void setRegionId(int regionId) {
        this.regionId = regionId;
    }

    public Timestamp getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Timestamp startedAt) {
        this.startedAt = startedAt;
    }

    public short getCurrentYLevel() {
        return currentYLevel;
    }

    public void setCurrentYLevel(short currentYLevel) {
        this.currentYLevel = currentYLevel;
    }

    public short getMaxYProcessed() {
        return maxYProcessed;
    }

    public void setMaxYProcessed(short maxYProcessed) {
        this.maxYProcessed = maxYProcessed;
    }

    public short getMinYTarget() {
        return minYTarget;
    }

    public void setMinYTarget(short minYTarget) {
        this.minYTarget = minYTarget;
    }

    public RenaturationPhase getPhase() {
        return phase;
    }

    public void setPhase(RenaturationPhase phase) {
        this.phase = phase;
    }

    public String getDecayRulesJson() {
        return decayRulesJson;
    }

    public void setDecayRulesJson(String decayRulesJson) {
        this.decayRulesJson = decayRulesJson;
    }

    public Timestamp getLastProcessed() {
        return lastProcessed;
    }

    public void setLastProcessed(Timestamp lastProcessed) {
        this.lastProcessed = lastProcessed;
    }

    public Timestamp getEstimatedCompletion() {
        return estimatedCompletion;
    }

    public void setEstimatedCompletion(Timestamp estimatedCompletion) {
        this.estimatedCompletion = estimatedCompletion;
    }
}

