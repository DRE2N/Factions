package de.erethon.factions.blocklog.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * Represents a single block change event.
 * Block states are stored as palette IDs referencing the block_palette table,
 * which deduplicates the actual compressed NBT data.
 *
 * @author Malfrador
 */
public record BlockChange(
        long id,
        int regionId,
        int x,
        int y,
        int z,
        String worldName,
        int oldBlockId,   // Palette ID (block_palette.id)
        int newBlockId,   // Palette ID (block_palette.id)
        ChangeType changeType,
        UUID causeUuid,
        String causeName,
        Timestamp timestamp,
        int baselineVersion
) implements Serializable {

    /**
     * Creates a new block change without an ID (for insertion).
     */
    public BlockChange(int regionId, int x, int y, int z, String worldName,
                      int oldBlockId, int newBlockId,
                      ChangeType changeType, UUID causeUuid, String causeName,
                      Timestamp timestamp, int baselineVersion) {
        this(0, regionId, x, y, z, worldName, oldBlockId, newBlockId,
                changeType, causeUuid, causeName, timestamp, baselineVersion);
    }
}

