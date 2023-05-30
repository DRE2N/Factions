package de.erethon.factions.war.objective;

import de.erethon.aergia.util.TickUtil;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Fyreum
 */
public class OccupyWarObjective extends TickingWarObjective {

    /* Settings */
    protected long occupyDuration;
    protected long occupiedInterval;
    protected int warProgressDecline;
    protected int warProgressDeclineContested;
    protected int warProgressPerOccupiedInterval;
    /* Temporary */
    protected final Set<Alliance> activeAlliances = new HashSet<>();
    protected Alliance leadingAlliance;
    protected int currentProgress = 0;
    protected int currentOccupiedProgress = 0;
    protected BossBar friendlyBossBar;
    protected BossBar hostileBossBar;

    public OccupyWarObjective(@NotNull ConfigurationSection config) {
        super(config);
    }

    @Override
    public void activate() {
        super.activate();
        friendlyBossBar = BossBar.bossBar(FMessage.UI_WAR_OBJECTIVE_OCCUPY_NEUTRAL.message(), 0f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
        hostileBossBar = BossBar.bossBar(FMessage.UI_WAR_OBJECTIVE_OCCUPY_NEUTRAL.message(), 0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
    }

    @Override
    public void deactivate() {
        super.activate();
        for (FPlayer fPlayer : activePlayers.keySet()) {
            hideProgressBar(fPlayer);
        }
        for (FPlayer fPlayer : activeSpectators) {
            hideProgressBar(fPlayer);
        }
    }

    @Override
    public void onEnter(@NotNull FPlayer fPlayer) {
        super.onEnter(fPlayer);
        // The first and only alliance to enter, when the score is 0 (again).
        if (leadingAlliance == null && activeAlliances.isEmpty()) {
            leadingAlliance = fPlayer.getAlliance();
        }
        activeAlliances.add(fPlayer.getAlliance());
        showProgressBar(fPlayer);
    }

    @Override
    public void onExit(@NotNull FPlayer fPlayer) {
        super.onExit(fPlayer);
        hideProgressBar(fPlayer);
        // Check for other players of the same alliance.
        for (FPlayer active : activePlayers.keySet()) {
            if (active.getAlliance() == fPlayer.getAlliance()) {
                return;
            }
        }
        activeAlliances.remove(fPlayer.getAlliance());
    }

    @Override
    public void onSpectatorEnter(@NotNull FPlayer fPlayer) {
        super.onSpectatorEnter(fPlayer);
        fPlayer.getPlayer().showBossBar(hostileBossBar);
    }

    @Override
    public void onSpectatorExit(@NotNull FPlayer fPlayer) {
        super.onSpectatorExit(fPlayer);
        fPlayer.getPlayer().showBossBar(hostileBossBar);
    }

    @Override
    public void tick() {
        updateScore();
        updateProgressBars();
    }

    private void updateScore() {
        // No alliance is in range.
        if (activeAlliances.isEmpty()) {
            updateScoreEmpty();
            return;
        }
        // Multiple alliances fight over the objective.
        if (activeAlliances.size() > 1) {
            return;
        }
        Alliance soleAlliance = activeAlliances.iterator().next();
        // Only the leading alliance is in range.
        if (soleAlliance == leadingAlliance) {
            updateScoreSole();
            return;
        }
        // Only a non-leading alliance is in range.
        if (currentProgress > 2) {
            currentProgress -= 2;
            return;
        }
        currentProgress = 0;
        leadingAlliance = soleAlliance;
        for (FPlayer fPlayer : activePlayers.keySet()) {
            fPlayer.getPlayer().hideBossBar(hostileBossBar);
            fPlayer.getPlayer().showBossBar(friendlyBossBar);
        }
    }

    private void updateScoreEmpty() {
        if (currentProgress <= 0) {
            return;
        }
        if (isOccupied()) {
            incrementOccupiedScore();
            return;
        }
        currentProgress = Math.max(currentProgress - warProgressDecline, 0);
    }

    private void updateScoreSole() {
        if (!isOccupied()) {
            incrementOccupiedScore();
            return;
        }
        currentProgress++;
    }

    private void incrementOccupiedScore() {
        if (++currentOccupiedProgress >= occupiedInterval) {
            currentOccupiedProgress = 0;
            Region region = plugin.getRegionManager().getRegionByLocation(location);
            if (region == null) {
                return;
            }
            leadingAlliance.getWarScores().getOrCreate(region).addScore(warProgressPerOccupiedInterval);
        }
    }

    public void updateProgressBars() {
        if (activeAlliances.isEmpty()) {
            return;
        }
        if (isOccupied()) {
            updateOccupiedProgressBars();
            return;
        }
        // Update the progress percentage.
        float percent = 1f / occupyDuration * currentProgress;
        String displayPercent = String.valueOf(percent * 100);

        friendlyBossBar.progress(percent);
        hostileBossBar.progress(percent);

        // Multiple alliances contest the objective.
        if (activeAlliances.size() > 1) {
            friendlyBossBar.name(FMessage.UI_WAR_OBJECTIVE_OCCUPY_CONTESTED.message(leadingAlliance.getDisplayShortName(), displayPercent));
            hostileBossBar.name(FMessage.UI_WAR_OBJECTIVE_OCCUPY_CONTESTED_OTHER.message(leadingAlliance.getDisplayShortName(), displayPercent));
            return;
        }
        // Only update the display message of the active alliance.
        if (getSoleAllianceRaw() == leadingAlliance) {
            friendlyBossBar.name(FMessage.UI_WAR_OBJECTIVE_OCCUPY_PROGRESS.message(leadingAlliance.getDisplayShortName(), displayPercent));
        } else {
            hostileBossBar.name(FMessage.UI_WAR_OBJECTIVE_OCCUPY_PROGRESS_OTHER.message(leadingAlliance.getDisplayShortName(), displayPercent));
        }
    }

    private void updateOccupiedProgressBars() {
        // Multiple alliances contest the objective.
        if (activeAlliances.size() > 1) {
            friendlyBossBar.name(FMessage.UI_WAR_OBJECTIVE_OCCUPIED_CONTESTED.message(leadingAlliance.getDisplayShortName()));
            hostileBossBar.name(FMessage.UI_WAR_OBJECTIVE_OCCUPIED_CONTESTED_OTHER.message(leadingAlliance.getDisplayShortName()));
            return;
        }
        // Only update the display message of the active alliance.
        if (getSoleAllianceRaw() == leadingAlliance) {
            friendlyBossBar.name(FMessage.UI_WAR_OBJECTIVE_OCCUPIED.message(leadingAlliance.getDisplayShortName()));
        } else {
            hostileBossBar.name(FMessage.UI_WAR_OBJECTIVE_OCCUPIED_CONTESTED_OTHER.message(leadingAlliance.getDisplayShortName()));
        }
    }

    public void showProgressBar(@NotNull FPlayer fPlayer) {
        fPlayer.getPlayer().showBossBar(fPlayer.getAlliance() == leadingAlliance ? friendlyBossBar : hostileBossBar);
    }

    public void hideProgressBar(@NotNull FPlayer fPlayer) {
        fPlayer.getPlayer().hideBossBar(fPlayer.getAlliance() == leadingAlliance ? friendlyBossBar : hostileBossBar);
    }

    /* Serialization */

    @Override
    public void load() {
        super.load();
        occupyDuration = config.getLong("occupyDuration", TickUtil.MINUTE * 5) / tickInterval;
        occupiedInterval = config.getLong("occupiedInterval", TickUtil.SECOND * 30);
        warProgressDecline = config.getInt("warProgressDecline", 1);
        warProgressDeclineContested = config.getInt("warProgressDeclineContested", 2);
        warProgressPerOccupiedInterval = config.getInt("warProgressPerOccupiedInterval", 1);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> serialized = super.serialize();
        serialized.put("captureTime", occupyDuration);
        serialized.put("occupiedInterval", occupiedInterval);
        serialized.put("warProgressDecline", warProgressDecline);
        serialized.put("warProgressDeclineContested", warProgressDeclineContested);
        serialized.put("warPointsPerOccupiedInterval", warProgressPerOccupiedInterval);
        return serialized;
    }

    /* Getters and setters */

    public long getOccupyDuration() {
        return occupyDuration;
    }

    public void setOccupyDuration(long occupyDuration) {
        this.occupyDuration = occupyDuration;
    }

    public long getOccupiedInterval() {
        return occupiedInterval;
    }

    public void setOccupiedInterval(long occupiedInterval) {
        this.occupiedInterval = occupiedInterval;
    }

    public int getWarProgressDecline() {
        return warProgressDecline;
    }

    public void setWarProgressDecline(int decline) {
        this.warProgressDecline = decline;
    }

    public int getWarProgressDeclineContested() {
        return warProgressDeclineContested;
    }

    public void setWarProgressDeclineContested(int progress) {
        this.warProgressDeclineContested = progress;
    }

    public int getWarProgressPerOccupiedInterval() {
        return warProgressPerOccupiedInterval;
    }

    public void setWarProgressPerOccupiedInterval(int progress) {
        this.warProgressPerOccupiedInterval = progress;
    }

    public @NotNull Set<Alliance> getActiveAlliances() {
        return activeAlliances;
    }

    /**
     * Returns the currently leading alliance, or null, if the score is 0.
     * This might differ from {@link #getSoleAlliance()}, as an alliance
     * can lead the score while not contesting the objective anymore.
     *
     * @return the leading alliance, or null
     */
    public @Nullable Alliance getLeadingAlliance() {
        return leadingAlliance;
    }

    /**
     * Returns the only alliance actively capturing the objective,
     * or null, if multiple alliance are currently in range.
     *
     * @return the sole capturing alliance, or null
     */
    public @Nullable Alliance getSoleAlliance() {
        return activeAlliances.size() == 1 ? getSoleAllianceRaw() : null;
    }

    private Alliance getSoleAllianceRaw() {
        return activeAlliances.iterator().next();
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(int progress) {
        this.currentProgress = progress;
    }

    public boolean isOccupied() {
        return currentProgress >= occupyDuration;
    }

    public int getCurrentOccupiedProgress() {
        return currentOccupiedProgress;
    }

    public void setCurrentOccupiedProgress(int score) {
        this.currentOccupiedProgress = score;
    }

    /* Sub classes */

    public static class Builder extends TickingWarObjective.Builder<Builder, OccupyWarObjective> {

        public @NotNull Builder occupyDuration(long occupyDuration) {
            data.set("occupyDuration", occupyDuration);
            return this;
        }

        public @NotNull Builder occupiedInterval(long occupiedInterval) {
            data.set("occupiedInterval", occupiedInterval);
            return this;
        }

        public @NotNull Builder warProgressDecline(long warProgressDecline) {
            data.set("warProgressDecline", warProgressDecline);
            return this;
        }

        public @NotNull Builder warProgressDeclineContested(long warProgressDeclineContested) {
            data.set("warProgressDeclineContested", warProgressDeclineContested);
            return this;
        }

        public @NotNull Builder warProgressPerOccupiedInterval(long warProgressPerOccupiedInterval) {
            data.set("warProgressPerOccupiedInterval", warProgressPerOccupiedInterval);
            return this;
        }

        @Override
        public @NotNull OccupyWarObjective build() {
            return new OccupyWarObjective(data);
        }
    }
}
