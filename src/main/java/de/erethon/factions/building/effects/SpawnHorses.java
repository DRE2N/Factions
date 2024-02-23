package de.erethon.factions.building.effects;

import de.erethon.factions.Factions;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.util.FLogger;
import io.papermc.paper.math.Position;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class SpawnHorses extends BuildingEffect {

    private final int interval;
    private final double jumpStrengthBoost;
    private final double speedBoost;
    private final int maxHorses;
    private final int checkRadius;

    private final NamespacedKey key = new NamespacedKey(Factions.get(), "horseSpawn");
    private BukkitRunnable task;
    private final Random random = new Random();

    public SpawnHorses(BuildingEffectData data, BuildSite site) {
        super(data, site);
        interval = data.getInt("interval", 60);
        maxHorses = data.getInt("maxHorses", 3);
        checkRadius = data.getInt("checkRadius", 16);
        jumpStrengthBoost = data.getDouble("jumpStrengthBoost", 0.5);
        speedBoost = data.getDouble("speedBoost", 0.2);
    }

    @Override
    public void apply() {
        if (task != null) {
            task.cancel();
        }
        task = new BukkitRunnable() {
            @Override
            public void run() {
                spawnHorse();
            }
        };
        task.runTaskTimer(Factions.get(), 0, (long) interval * 20 * 60);
    }

    private void spawnHorse() {
        Position spawner = site.getNamedPositions().get("horseSpawn");
        if (spawner == null) {
            FLogger.BUILDING.log("No horse spawn position found for building " + site.getBuilding().getName());
            return;
        }
        Location loc = spawner.toLocation(site.getInteractive().getWorld());
        int horses = 0;
        for (LivingEntity livingEntity : loc.getNearbyEntitiesByType(Horse.class, checkRadius)) {
            if (livingEntity instanceof Horse horse && horse.getPersistentDataContainer().has(key)) {
                horses++;
            }
        }
        if (horses >= maxHorses) {
            return;
        }
        loc = loc.add(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
        loc.getWorld().spawn(loc, Horse.class, horse -> {
            horse.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1); // Add a marker, so we can identify horses spawned by this effect
            horse.setAdult();
            horse.setAgeLock(true);
            horse.setBreed(false); // Prevent the horse from breeding
            horse.setJumpStrength(horse.getJumpStrength() + jumpStrengthBoost);
            horse.setHealth(20);
            horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue() + speedBoost);
        });
    }

    @Override
    public void remove() {
        task.cancel();
    }
}
