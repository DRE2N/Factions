package de.erethon.factions.war.task;

import de.erethon.aergia.util.TickUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.util.FBroadcastUtil;
import de.erethon.factions.war.WarPhaseManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author Fyreum
 */
public class PhaseSwitchTask extends BukkitRunnable {

    private static final Set<Integer> finalSeconds = Set.of(1, 2, 3, 4, 5);

    final Factions plugin = Factions.get();

    private final WarPhaseManager warPhaseManager;
    private int m;
    private int m2;
    private int s;

    public PhaseSwitchTask(@NotNull WarPhaseManager warPhaseManager, int minutes) {
        this.warPhaseManager = warPhaseManager;
        this.m = minutes;
        this.m2 = minutes;
        this.s = minutes*60;
    }

    @Override
    public void run() {
        if (m == m2 && m != 0) {
            if (m > 1) {
                FBroadcastUtil.broadcastWar(FMessage.WAR_PHASE_ANNOUNCEMENT_MINUTES, warPhaseManager.getNextWarPhase().getDisplayName().getMessage(), String.valueOf(m));
            } else {
                FBroadcastUtil.broadcastWar(FMessage.WAR_PHASE_ANNOUNCEMENT_MINUTE, warPhaseManager.getNextWarPhase().getDisplayName().getMessage());
            }
            --m2;
        } else if (finalSeconds.contains(s)) {
            if (s > 1) {
                FBroadcastUtil.broadcastWar(FMessage.WAR_PHASE_ANNOUNCEMENT_SECONDS, warPhaseManager.getNextWarPhase().getDisplayName().getMessage(), String.valueOf(s));
            } else {
                FBroadcastUtil.broadcastWar(FMessage.WAR_PHASE_ANNOUNCEMENT_SECOND, warPhaseManager.getNextWarPhase().getDisplayName().getMessage());
            }
        }
        if (s <= 0) {
            warPhaseManager.updateCurrentStageTask();
        }
        m = (int) Math.ceil((double) --s/60);
    }

    public @NotNull BukkitTask start() {
        BukkitTask task = runTaskTimerAsynchronously(plugin, 0, TickUtil.SECOND);
        warPhaseManager.getRunningTasks().add(task);
        return task;
    }

    public @NotNull WarPhaseManager getWarPhaseManager() {
        return warPhaseManager;
    }

}
