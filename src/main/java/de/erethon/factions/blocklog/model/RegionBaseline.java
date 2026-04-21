package de.erethon.factions.blocklog.model;

import java.sql.Timestamp;

/**
 * Represents the baseline (natural state) of a region.
 *
 * @author Malfrador
 */
public record RegionBaseline(
        int regionId,
        int currentVersion,
        String currentSchematicPath,
        Timestamp createdAt,
        Timestamp updatedAt
) {
}

