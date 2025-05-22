package de.erethon.factions.building.effects;

import com.fastasyncworldedit.core.Fawe;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;
import de.erethon.factions.Factions;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildSiteSection;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.util.FLogger;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class RegeneratingMine extends BuildingEffect {

    private BuildSiteSection section;
    private final Set<RegenEntry> regenEntries = new HashSet<>();
    private long lastRegen = 0;
    private final long regenInterval;

    public RegeneratingMine(@NotNull BuildingEffectData effect, BuildSite site) {
        super(effect, site);
        for (BuildSiteSection siteSection : site.getSections()) {
            if (siteSection.name().equalsIgnoreCase("mine")) {
                section = siteSection;
                break;
            }
        }
        loadEntriesFromConfig();
        regenInterval = effect.getConfig().getLong("regenInterval", 60) * 1000 * 60;
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                checkForRegen();
            }
        };
        task.runTaskTimer(Factions.get(), 0, 60 * 20);
    }

    private void checkForRegen() {
        if (System.currentTimeMillis() - lastRegen > regenInterval) {
            regen();
            lastRegen = System.currentTimeMillis();
        }
    }

    private void regen() {
        World world = BukkitAdapter.adapt(site.getCorner().getWorld());
        try (EditSession session = Fawe.instance().getWorldEdit().newEditSession(world)) {
            CuboidRegion region = new CuboidRegion(BlockVector3.at(section.corner1().x(), section.corner1().y(), section.corner1().z()), BlockVector3.at(section.corner2().x(), section.corner2().y(), section.corner2().z()));
            for (RegenEntry entry : regenEntries) {
                session.addOre(region, Masks.alwaysTrue(), entry.pattern, entry.size, entry.frequency, entry.rarity, -512, 1024);
            }
            session.close();
            int changed = session.getBlockChangeCount();
            FLogger.BUILDING.log("Regenerated " + changed + " blocks in " + site.getBuilding().getId() + "'s mine at " + site.getRegion().getName());
        }

    }

    private void loadEntriesFromConfig() {
        if (!data.contains("ores")) {
            FLogger.ERROR.log("No ores defined for regenerating mine effect on " + site.getBuilding().getId() + " at " + site.getRegion().getName());
            return;
        }
        for (String id : data.getConfigurationSection("ores").getKeys(false)) {
            String matID = data.getString("ores." + id + ".material");
            int size = data.getInt("ores." + id + ".size");
            int frequency = data.getInt("ores." + id + ".frequency");
            int rarity = data.getInt("ores." + id + ".rarity");
            Material material = Material.getMaterial(matID);
            regenEntries.add(new RegenEntry(BukkitAdapter.adapt(material.createBlockData()), size, frequency, rarity));
        }
    }

    private record RegenEntry(Pattern pattern, int size, int frequency, int rarity) {}
}

