package de.erethon.factions.building.effects;

import de.erethon.factions.Factions;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.entity.Relation;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.player.FPlayerCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class EnemyRadar extends BuildingEffect {

    private final FPlayerCache cache = Factions.get().getFPlayerCache();

    private final double range;
    private final int tickRate;
    private final int announcementDelay;
    private final int locationFuzz;

    private int ticks = 0;
    private final Set<FPlayer> detectedEnemies = new HashSet<>();
    private boolean isWaitingForAnnouncement = false;

    public EnemyRadar(@NotNull BuildingEffectData data, BuildSite site) {
        super(data, site);
        range = data.getDouble("range", 128);
        tickRate = data.getInt("tickRate", 60);
        announcementDelay = data.getInt("announcementDelay", 300);
        locationFuzz = data.getInt("locationFuzz", 50);
    }

    @Override
    public void tick() {
        ticks++;
        if (ticks < tickRate) {
            return;
        }
        ticks = 0;
        for (Player p : site.getInteractive().getNearbyPlayers(range)) {
            FPlayer fPlayer = cache.getByPlayer(p);
            if (fPlayer.getFaction() != null && fPlayer.getFaction().getRelation(faction) == Relation.ENEMY) {
                detectedEnemies.add(fPlayer);
            }
        }
        if (detectedEnemies.isEmpty()) {
            return;
        }
        if (isWaitingForAnnouncement) {
            return;
        }
        isWaitingForAnnouncement = true;
        BukkitRunnable announcement = getAnnouncement();
        // We don't want to instantly announce the enemies, give them a chance to leave the area
        announcement.runTaskLater(Factions.get(), announcementDelay);
    }

    @NotNull
    private BukkitRunnable getAnnouncement() {
        Random random = new Random();
        // Don't want to announce the exact location of the structure, let the players search the enemies
        int x = random.nextInt(locationFuzz) - locationFuzz / 2;
        int z = random.nextInt(locationFuzz) - locationFuzz / 2;
        x += site.getInteractive().getBlockX();
        z += site.getInteractive().getBlockZ();
        String fuzzedLocation = x + ", " + z;
        return new BukkitRunnable() {
            @Override
            public void run() {
                Title title = Title.title(Component.translatable("factions.building.effects.radar.enemiesDetected"),
                        Component.translatable("factions.building.effects.radar.enemiesInfo", Component.text(detectedEnemies.size()), Component.text(fuzzedLocation)));
                site.getRegion().friendlyAudiences().forEach(audience -> audience.showTitle(title));
                detectedEnemies.clear();
                isWaitingForAnnouncement = false;
            }
        };
    }

}
