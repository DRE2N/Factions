package de.erethon.factions.war.structure;

import de.erethon.aergia.util.TickUtil;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.WarRegion;
import io.papermc.paper.math.Position;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Fyreum
 */
public class OccupyWarStructure extends TickingWarStructure {

    /* Settings */
    protected long occupyDuration;
    protected long occupiedInterval;
    protected int warProgressDecline;
    protected int warProgressDeclineContested;
    protected int warProgressPerOccupiedInterval;
    protected Set<FlagStructure> flagStructures = new HashSet<>();
    /* Temporary */
    protected final Set<Alliance> activeAlliances = new HashSet<>();
    protected final List<Position> occupationDisplayBlocks = new ArrayList<>();
    protected Alliance leadingAlliance;
    protected int currentProgress = 0;
    protected int currentOccupiedProgress = 0;
    protected BossBar bossBar;
    protected boolean occupationDisplayScanned;

    public OccupyWarStructure(@NotNull WarRegion region, @NotNull ConfigurationSection config) {
        super(region, config);
    }

    public OccupyWarStructure(@NotNull WarRegion region, @NotNull ConfigurationSection config, @NotNull Position a, @NotNull Position b) {
        super(region, config, a, b);
    }

    @Override
    protected void load(@NotNull ConfigurationSection config) {
        super.load(config);
        this.occupyDuration = Math.max(1, config.getLong("occupyDuration", config.getLong("captureTime", TickUtil.SECOND * 30)) / tickInterval);
        this.occupiedInterval = Math.max(1, config.getLong("occupiedInterval", TickUtil.SECOND * 30) / tickInterval);
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
        super.deactivate();
        for (FPlayer fPlayer : Set.copyOf(activePlayers.keySet())) {
            hideProgressBar(fPlayer);
            fPlayer.getActiveWarObjectives().remove(this);
        }
        for (FPlayer fPlayer : Set.copyOf(activeSpectators)) {
            hideProgressBar(fPlayer);
        }
        activePlayers.clear();
        activeSpectators.clear();
        activeAlliances.clear();
    }

    @Override
    public void onEnter(@NotNull FPlayer fPlayer) {
        if (fPlayer.getAlliance() == null) {
            return;
        }
        super.onEnter(fPlayer);
        // The first and only alliance to enter, when the score is 0 (again).
        if (leadingAlliance == null && activeAlliances.isEmpty() && fPlayer.getAlliance() != region.getAlliance()) {
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
        hideProgressBar(fPlayer);
    }

    @Override
    public void tick() {
        refreshPlayersInRange();
        if (!plugin.getCurrentWarPhase().isAllowCapture()) {
            updateProgressBars(FMessage.UI_WAR_OBJECTIVE_OCCUPY_CAPTURE_DISABLED.message());
            return;
        }
        if (!isCaptureUnlocked()) {
            updateProgressBars(FMessage.UI_WAR_OBJECTIVE_OCCUPY_CRYSTAL_LOCKED.message());
            return;
        }
        updateScore();
        updateProgressBars();
    }

    private void updateScore() {
        // No alliance is in range.
        if (activeAlliances.isEmpty()) {
            updateScoreEmpty();
            updateOccupationDisplay();
            return;
        }
        // Multiple alliances fight over the objective.
        if (activeAlliances.size() > 1) {
            updateOccupationDisplay();
            return;
        }
        Alliance soleAlliance = activeAlliances.iterator().next();
        if (soleAlliance == region.getAlliance()) {
            updateScoreEmpty();
            return;
        }
        // Only the leading alliance is in range.
        if (soleAlliance == leadingAlliance) {
            if (isOccupied() && region.getAlliance() != leadingAlliance) {
                captureForLeadingAlliance();
                return;
            }
            currentProgress++;
            updateOccupationDisplay();
            if (isOccupied()) {
                captureForLeadingAlliance();
            }
            return;
        }
        // Only a non-leading alliance is in range.
        if (currentProgress > 2) {
            currentProgress -= 2;
            updateOccupationDisplay();
            return;
        }
        // A new alliance is leading.
        currentProgress = 0;
        currentOccupiedProgress = 0;
        leadingAlliance = soleAlliance;
        updateOccupationDisplay();
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
            updateOccupationDisplay();
            return;
        }
        if (isOccupied()) {
            incrementOccupiedScore();
            updateOccupationDisplay();
            return;
        }
        currentProgress -= warProgressDecline;
        if (currentProgress > 0) {
            return;
        }
        // No alliance has progress anymore -> completely empty objective.
        currentProgress = 0;
        updateOccupationDisplay();
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

    private void captureForLeadingAlliance() {
        if (leadingAlliance == null) {
            return;
        }
        for (FPlayer fPlayer : activePlayers.keySet()) {
            if (fPlayer.getAlliance() == leadingAlliance) {
                region.getRegionalWarTracker().addContribution(fPlayer.getFaction(), "captures_assisted", 1);
            }
        }
        updateOccupationDisplay(leadingAlliance, 1.0);
        leadingAlliance.temporaryOccupy(region);
        currentProgress = 0;
        currentOccupiedProgress = 0;
    }

    private boolean isCaptureUnlocked() {
        return region.getRegionalWarTracker().isCaptureUnlocked();
    }

    private void updateOccupationDisplay() {
        if (leadingAlliance == null) {
            updateOccupationDisplay(null, 0.0);
            return;
        }
        updateOccupationDisplay(leadingAlliance, Math.min(1.0, Math.max(0.0, currentProgress / (double) occupyDuration)));
    }

    private void updateOccupationDisplay(@Nullable Alliance alliance, double progress) {
        scanOccupationDisplayBlocks();
        if (occupationDisplayBlocks.isEmpty()) {
            return;
        }
        Material captureMaterial = alliance == null ? Material.WHITE_CONCRETE
                : FlagStructure.concreteFor(NamedTextColor.nearestTo(alliance.getColor()));
        int coloredBlocks = (int) Math.round(occupationDisplayBlocks.size() * progress);
        for (int i = 0; i < occupationDisplayBlocks.size(); i++) {
            Position position = occupationDisplayBlocks.get(i);
            Material material = i < coloredBlocks ? captureMaterial : Material.WHITE_CONCRETE;
            if (region.getWorld().getType(position.blockX(), position.blockY(), position.blockZ()) != material) {
                region.getWorld().setType(position.blockX(), position.blockY(), position.blockZ(), material);
            }
        }
    }

    private void scanOccupationDisplayBlocks() {
        if (occupationDisplayScanned) {
            return;
        }
        occupationDisplayScanned = true;
        Position min = getMinPosition();
        Position max = getMaxPosition();
        for (int y = min.blockY(); y <= max.blockY(); y++) {
            for (int z = min.blockZ(); z <= max.blockZ(); z++) {
                for (int x = min.blockX(); x <= max.blockX(); x++) {
                    Material material = region.getWorld().getType(x, y, z);
                    if (material.name().endsWith("_CONCRETE")) {
                        occupationDisplayBlocks.add(Position.block(x, y, z));
                    }
                }
            }
        }
    }

    public void updateProgressBars() {
        if (activeAlliances.isEmpty()) {
            return;
        }
        if (activeAlliances.size() == 1 && activeAlliances.contains(region.getAlliance())) {
            updateProgressBars(FMessage.UI_WAR_OBJECTIVE_OCCUPY_NEUTRAL.message());
            return;
        }
        if (!isCaptureUnlocked()) {
            updateProgressBars(FMessage.UI_WAR_OBJECTIVE_OCCUPY_CRYSTAL_LOCKED.message());
            return;
        }
        if (leadingAlliance == null) {
            updateProgressBars(FMessage.UI_WAR_OBJECTIVE_OCCUPY_NEUTRAL.message());
            return;
        }
        if (isOccupied()) {
            bossBar.name((activeAlliances.size() > 1 ? FMessage.UI_WAR_OBJECTIVE_OCCUPIED_CONTESTED : FMessage.UI_WAR_OBJECTIVE_OCCUPIED)
                    .message(leadingAlliance.getColoredShortName()));
            return;
        }
        // Update the progress percentage.
        float percent = 1f / occupyDuration * currentProgress;
        double displayPercent = Math.round(percent * 1000.0) / 10.0;

        bossBar.progress(percent);
        bossBar.name((activeAlliances.size() > 1 ? FMessage.UI_WAR_OBJECTIVE_OCCUPY_CONTESTED : FMessage.UI_WAR_OBJECTIVE_OCCUPY_PROGRESS)
                .message(leadingAlliance.getColoredShortName(), Component.text(displayPercent)));
    }

    private void updateProgressBars(@NotNull Component message) {
        if (activeAlliances.isEmpty()) {
            return;
        }
        bossBar.name(message);
        bossBar.progress(0f);
    }

    private void refreshPlayersInRange() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(player);
            boolean inRegion = fPlayer.getCurrentRegion() == region;
            boolean inRange = inRegion && containsPlayerPosition(player.getLocation());
            boolean spectator = isSpectatorCandidate(fPlayer);
            if (isActive(fPlayer)) {
                if (!inRange || spectator) {
                    onExit(fPlayer);
                }
                continue;
            }
            if (isSpectator(fPlayer)) {
                if (!inRange || !spectator) {
                    onSpectatorExit(fPlayer);
                }
                continue;
            }
            if (!inRange) {
                continue;
            }
            if (spectator) {
                onSpectatorEnter(fPlayer);
            } else {
                onEnter(fPlayer);
            }
        }
    }

    private boolean isSpectatorCandidate(@NotNull FPlayer fPlayer) {
        return fPlayer.getAlliance() == null || fPlayer.getPlayer().getGameMode() == GameMode.SPECTATOR;
    }

    public void showProgressBar(@NotNull FPlayer fPlayer) {
        fPlayer.getPlayer().showBossBar(bossBar);
    }

    public void hideProgressBar(@NotNull FPlayer fPlayer) {
        fPlayer.getPlayer().hideBossBar(bossBar);
    }

    @Override
    public void onTemporaryOccupy(@NotNull Alliance alliance) {
        currentProgress = 0;
        currentOccupiedProgress = 0;
        leadingAlliance = alliance;
    }

    /* Serialization */

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> serialized = super.serialize();
        serialized.put("occupyDuration", occupyDuration * tickInterval);
        serialized.put("occupiedInterval", occupiedInterval * tickInterval);
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

}
