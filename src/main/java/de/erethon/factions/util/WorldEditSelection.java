package de.erethon.factions.util;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import io.papermc.paper.math.Position;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record WorldEditSelection(@NotNull World world, @NotNull Position min, @NotNull Position max) {

    public static @Nullable WorldEditSelection get(@NotNull Player player) {
        try {
            com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(player);
            com.sk89q.worldedit.LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);
            com.sk89q.worldedit.regions.Region selection = session.getSelection(wePlayer.getWorld());
            BlockVector3 min = selection.getMinimumPoint();
            BlockVector3 max = selection.getMaximumPoint();
            return new WorldEditSelection(player.getWorld(),
                    Position.block(min.x(), min.y(), min.z()),
                    Position.block(max.x(), max.y(), max.z()));
        } catch (IncompleteRegionException e) {
            return null;
        }
    }

    public @NotNull Location minLocation() {
        return min.toLocation(world);
    }

    public @NotNull Location maxLocation() {
        return max.toLocation(world);
    }
}
