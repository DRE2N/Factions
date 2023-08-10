package de.erethon.factions.war.objective;

import de.erethon.aergia.util.TickUtil;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.war.structure.FlagStructure;
import io.papermc.paper.math.Position;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
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
    protected Set<FlagStructure> flagStructures = new HashSet<>();
    /* Temporary */
    protected final Set<Alliance> activeAlliances = new HashSet<>();
    protected Alliance leadingAlliance;
    protected int currentProgress = 0;
    protected int currentOccupiedProgress = 0;
    protected BossBar bossBar;

    public OccupyWarObjective(@NotNull Region region, @NotNull ConfigurationSection config) {
        super(region, config);
        load(config);
    }

    public OccupyWarObjective(@NotNull Region region, @NotNull ConfigurationSection config, @NotNull Position a, @NotNull Position b) {
        super(region, config, a, b);
        load(config);
    }

    private void load(@NotNull ConfigurationSection config) {
        this.occupyDuration = config.getLong("occupyDuration", TickUtil.MINUTE * 5) / tickInterval;
        this.occupiedInterval = config.getLong("occupiedInterval", TickUtil.SECOND * 30);
        this.warProgressDecline = config.getInt("warProgressDecline", 1);
        this.warProgressDeclineContested = config.getInt("warProgressDeclineContested", 2);
        this.warProgressPerOccupiedInterval = config.getInt("warProgressPerOccupiedInterval", 1);

        ConfigurationSection flagsSection = config.getConfigurationSection("flagStructures");
        if (flagsSection != null) {
            for (String key : flagsSection.getKeys(false)) {
                ConfigurationSection section = flagsSection.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                this.flagStructures.add(new FlagStructure(region, config));
            }
        }
    }

    @Override
    public void activate() {
        super.activate();
        bossBar = BossBar.bossBar(FMessage.UI_WAR_OBJECTIVE_OCCUPY_NEUTRAL.message(), 0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
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
        showProgressBar(fPlayer);
    }

    @Override
    public void onSpectatorExit(@NotNull FPlayer fPlayer) {
        super.onSpectatorExit(fPlayer);
        showProgressBar(fPlayer);
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
            if (isOccupied()) {
                incrementOccupiedScore();
                return;
            }
            currentProgress++;
            return;
        }
        // Only a non-leading alliance is in range.
        if (currentProgress > 2) {
            currentProgress -= 2;
            return;
        }
        // A new alliance is leading.
        currentProgress = 0;
        currentOccupiedProgress = 0;
        leadingAlliance = soleAlliance;
        bossBar.color(leadingAlliance.getBossBarColor());
        for (FPlayer fPlayer : activePlayers.keySet()) {
            fPlayer.getPlayer().hideBossBar(bossBar);
        }
        for (FlagStructure flag : flagStructures) {
            flag.displayColor(region.getWorld(), NamedTextColor.nearestTo(leadingAlliance.getColor()));
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
        currentProgress -= warProgressDecline;
        if (currentProgress > 0) {
            return;
        }
        // No alliance has progress anymore -> completely empty objective.
        currentProgress = 0;
        for (FlagStructure flag : flagStructures) {
            flag.displayColor(region.getWorld(), NamedTextColor.WHITE);
        }
    }

    private void incrementOccupiedScore() {
        if (++currentOccupiedProgress >= occupiedInterval) {
            currentOccupiedProgress = 0;
            region.getRegionalWarTracker().addScore(leadingAlliance, warProgressPerOccupiedInterval);
        }
    }

    public void updateProgressBars() {
        if (activeAlliances.isEmpty()) {
            return;
        }
        if (isOccupied()) {
            bossBar.name((activeAlliances.size() > 1 ? FMessage.UI_WAR_OBJECTIVE_OCCUPIED_CONTESTED : FMessage.UI_WAR_OBJECTIVE_OCCUPIED)
                    .message(leadingAlliance.getColoredShortName()));
            return;
        }
        // Update the progress percentage.
        float percent = 1f / occupyDuration * currentProgress;
        Component displayPercent = Component.text(percent * 100);

        bossBar.progress(percent);
        bossBar.name((activeAlliances.size() > 1 ? FMessage.UI_WAR_OBJECTIVE_OCCUPY_CONTESTED : FMessage.UI_WAR_OBJECTIVE_OCCUPY_PROGRESS)
                .message(leadingAlliance.getColoredShortName(), displayPercent));
    }

    public void showProgressBar(@NotNull FPlayer fPlayer) {
        fPlayer.getPlayer().showBossBar(bossBar);
    }

    public void hideProgressBar(@NotNull FPlayer fPlayer) {
        fPlayer.getPlayer().hideBossBar(bossBar);
    }

    /* Serialization */

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> serialized = super.serialize();
        serialized.put("captureTime", occupyDuration);
        serialized.put("occupiedInterval", occupiedInterval);
        serialized.put("warProgressDecline", warProgressDecline);
        serialized.put("warProgressDeclineContested", warProgressDeclineContested);
        serialized.put("warPointsPerOccupiedInterval", warProgressPerOccupiedInterval);
        Map<String, Object> serializedFlags = new HashMap<>(flagStructures.size());
        for (FlagStructure flag : flagStructures) {
            serializedFlags.put(String.valueOf(serializedFlags.size()), flag.serialize());
        }
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

    public @NotNull Set<FlagStructure> getFlagStructures() {
        return flagStructures;
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
        public @NotNull OccupyWarObjective build(@NotNull Region region) {
            return new OccupyWarObjective(region, data);
        }
    }
}
