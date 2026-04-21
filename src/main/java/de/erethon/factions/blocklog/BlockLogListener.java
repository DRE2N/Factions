package de.erethon.factions.blocklog;

import de.erethon.factions.Factions;
import de.erethon.factions.blocklog.model.ChangeType;
import de.erethon.factions.blocklog.util.BlockDataSerializer;
import de.erethon.factions.region.Region;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockLogListener implements Listener {

    private final Factions plugin;
    private final BlockLogManager blockLogManager;

    private final Map<Location, byte[]> pendingOldStates = new ConcurrentHashMap<>();

    public BlockLogListener(Factions plugin, BlockLogManager blockLogManager) {
        this.plugin = plugin;
        this.blockLogManager = blockLogManager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlaceCapture(BlockPlaceEvent event) {
        if (!blockLogManager.isReady()) return;
        Block block = event.getBlock();
        Region region = plugin.getRegionManager().getRegionByLocation(block.getLocation());
        if (region == null) {
            return;
        }
        pendingOldStates.put(block.getLocation(),
                BlockDataSerializer.serializeBlockState(event.getBlockReplacedState()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!blockLogManager.isReady()) return;
        Block block = event.getBlock();
        Location loc = block.getLocation();
        byte[] oldBlockData = pendingOldStates.remove(loc);
        if (oldBlockData == null) {
            return; // Not in a region, or capture didn't run
        }

        Region region = plugin.getRegionManager().getRegionByLocation(loc);
        if (region == null) {
            return;
        }

        Player player = event.getPlayer();
        // At MONITOR, the block is now the placed block — serialize the new state
        byte[] newBlockData = BlockDataSerializer.serializeBlock(block);

        int baselineVersion = blockLogManager.getRegionBaselineDAO()
                .getCurrentVersion(region.getId())
                .orElse(0);

        blockLogManager.logPlace(
                region.getId(),
                block.getX(), block.getY(), block.getZ(),
                block.getWorld().getName(),
                oldBlockData, newBlockData,
                player.getUniqueId(), player.getName(),
                baselineVersion
        );
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreakCapture(BlockBreakEvent event) {
        if (!blockLogManager.isReady()) return;
        Block block = event.getBlock();
        Region region = plugin.getRegionManager().getRegionByLocation(block.getLocation());
        if (region == null) {
            return;
        }
        // Serialize the block as it currently is (before it's broken)
        pendingOldStates.put(block.getLocation(), BlockDataSerializer.serializeBlock(block));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!blockLogManager.isReady()) return;
        Block block = event.getBlock();
        Location loc = block.getLocation();
        byte[] oldBlockData = pendingOldStates.remove(loc);
        if (oldBlockData == null) {
            return; // Not in a region, or capture didn't run
        }

        Region region = plugin.getRegionManager().getRegionByLocation(loc);
        if (region == null) {
            return;
        }

        Player player = event.getPlayer();
        // New state is air (empty) — palette ID 0
        byte[] newBlockData = new byte[0];

        int baselineVersion = blockLogManager.getRegionBaselineDAO()
                .getCurrentVersion(region.getId())
                .orElse(0);

        blockLogManager.logBreak(
                region.getId(),
                block.getX(), block.getY(), block.getZ(),
                block.getWorld().getName(),
                oldBlockData, newBlockData,
                player.getUniqueId(), player.getName(),
                baselineVersion
        );
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockExplodeCapture(BlockExplodeEvent event) {
        if (!blockLogManager.isReady()) return;
        for (Block block : event.blockList()) {
            Region region = plugin.getRegionManager().getRegionByLocation(block.getLocation());
            if (region == null) {
                continue;
            }
            pendingOldStates.put(block.getLocation(), BlockDataSerializer.serializeBlock(block));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!blockLogManager.isReady()) return;
        for (Block block : event.blockList()) {
            Location loc = block.getLocation();
            byte[] oldBlockData = pendingOldStates.remove(loc);
            if (oldBlockData == null) {
                continue;
            }

            Region region = plugin.getRegionManager().getRegionByLocation(loc);
            if (region == null) {
                continue;
            }

            int oldId = blockLogManager.getPalette().getOrCreate(oldBlockData);
            int newId = 0; // AIR

            int baselineVersion = blockLogManager.getRegionBaselineDAO()
                    .getCurrentVersion(region.getId())
                    .orElse(0);

            blockLogManager.logBlockChange(
                    new de.erethon.factions.blocklog.model.BlockChange(
                            region.getId(),
                            block.getX(), block.getY(), block.getZ(),
                            block.getWorld().getName(),
                            oldId, newId,
                            ChangeType.EXPLOSION,
                            null,
                            "Explosion",
                            new java.sql.Timestamp(System.currentTimeMillis()),
                            baselineVersion
                    )
            );
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityExplodeCapture(EntityExplodeEvent event) {
        if (!blockLogManager.isReady()) return;
        for (Block block : event.blockList()) {
            Region region = plugin.getRegionManager().getRegionByLocation(block.getLocation());
            if (region == null) {
                continue;
            }
            pendingOldStates.put(block.getLocation(), BlockDataSerializer.serializeBlock(block));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!blockLogManager.isReady()) return;
        String causeName = event.getEntity().getType().name();

        for (Block block : event.blockList()) {
            Location loc = block.getLocation();
            byte[] oldBlockData = pendingOldStates.remove(loc);
            if (oldBlockData == null) {
                continue;
            }

            Region region = plugin.getRegionManager().getRegionByLocation(loc);
            if (region == null) {
                continue;
            }

            int oldId = blockLogManager.getPalette().getOrCreate(oldBlockData);
            int newId = 0; // AIR

            int baselineVersion = blockLogManager.getRegionBaselineDAO()
                    .getCurrentVersion(region.getId())
                    .orElse(0);

            blockLogManager.logBlockChange(
                    new de.erethon.factions.blocklog.model.BlockChange(
                            region.getId(),
                            block.getX(), block.getY(), block.getZ(),
                            block.getWorld().getName(),
                            oldId, newId,
                            ChangeType.EXPLOSION,
                            null,
                            causeName,
                            new java.sql.Timestamp(System.currentTimeMillis()),
                            baselineVersion
                    )
            );
        }
    }

    // ========================
    // Cancellation cleanup
    // ========================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockPlaceCancelled(BlockPlaceEvent event) {
        if (event.isCancelled()) {
            pendingOldStates.remove(event.getBlock().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockBreakCancelled(BlockBreakEvent event) {
        if (event.isCancelled()) {
            pendingOldStates.remove(event.getBlock().getLocation());
        }
    }
}
