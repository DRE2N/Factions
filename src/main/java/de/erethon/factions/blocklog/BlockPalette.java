package de.erethon.factions.blocklog;

import de.erethon.factions.blocklog.util.BlockDataSerializer;
import de.erethon.factions.util.FLogger;
import org.jetbrains.annotations.NotNull;
import org.jdbi.v3.core.Jdbi;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory palette that maps compressed block data to integer IDs.
 **/
public class BlockPalette {

    public static final int AIR_ID = 0;

    private final Jdbi jdbi;

    /** hash → palette id */
    private final Map<String, Integer> hashToId = new ConcurrentHashMap<>();
    /** palette id → compressed block data */
    private final Map<Integer, byte[]> idToData = new ConcurrentHashMap<>();
    /** palette id → material name (e.g. "minecraft:stone") */
    private final Map<Integer, String> idToMaterial = new ConcurrentHashMap<>();

    public BlockPalette(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    /**
     * Creates the palette table if it doesn't exist and loads all existing entries into memory.
     */
    public void initialize() {
        jdbi.useHandle(handle -> {
            // Create table (without material column for initial creation compatibility)
            handle.execute("""
                CREATE TABLE IF NOT EXISTS block_palette (
                    id SERIAL PRIMARY KEY,
                    hash VARCHAR(64) NOT NULL UNIQUE,
                    block_data BYTEA NOT NULL
                )
                """);

            // Migration: add material column if it doesn't exist
            handle.execute("""
                ALTER TABLE block_palette ADD COLUMN IF NOT EXISTS material VARCHAR(128) NOT NULL DEFAULT 'minecraft:air'
                """);

            // Ensure AIR entry exists at id 0
            handle.execute("""
                INSERT INTO block_palette (id, hash, block_data, material)
                VALUES (0, 'AIR', '\\x', 'minecraft:air')
                ON CONFLICT (id) DO NOTHING
                """);

            // Index for material lookups
            handle.execute("""
                CREATE INDEX IF NOT EXISTS idx_palette_material ON block_palette (material)
                """);
        });

        loadAll();
    }

    /**
     * Loads all palette entries from the database into the in-memory maps.
     */
    private void loadAll() {
        jdbi.useHandle(handle ->
            handle.createQuery("SELECT id, hash, block_data, material FROM block_palette")
                    .map((rs, ctx) -> {
                        int id = rs.getInt("id");
                        String hash = rs.getString("hash");
                        byte[] data = rs.getBytes("block_data");
                        String material = rs.getString("material");
                        hashToId.put(hash, id);
                        idToData.put(id, data);
                        idToMaterial.put(id, material);
                        return id;
                    })
                    .list()
        );
        FLogger.REGION.log("Block palette loaded: " + hashToId.size() + " entries");
    }

    /**
     * Gets or creates a palette ID for the given block data.
     *
     * @param blockData Compressed (gzipped) block data, or null/empty for air
     * @return The palette ID
     */
    public int getOrCreate(byte[] blockData) {
        if (blockData == null || blockData.length == 0) {
            return AIR_ID;
        }

        String hash = computeHash(blockData);

        // Fast path: already in memory
        Integer existing = hashToId.get(hash);
        if (existing != null) {
            return existing;
        }

        // Extract material name from the block data
        String material = BlockDataSerializer.getMaterialName(blockData);

        // Slow path: insert into DB and update cache
        int id = jdbi.withHandle(handle ->
            handle.createQuery("""
                INSERT INTO block_palette (hash, block_data, material)
                VALUES (:hash, :data, :material)
                ON CONFLICT (hash) DO UPDATE SET hash = EXCLUDED.hash
                RETURNING id
                """)
                    .bind("hash", hash)
                    .bind("data", blockData)
                    .bind("material", material)
                    .mapTo(Integer.class)
                    .one()
        );

        // Update in-memory cache
        hashToId.put(hash, id);
        idToData.put(id, blockData);
        idToMaterial.put(id, material);
        return id;
    }

    /**
     * Gets the compressed block data for a palette ID.
     */
    public byte[] getData(int id) {
        if (id == AIR_ID) {
            return new byte[0];
        }

        byte[] data = idToData.get(id);
        if (data != null) {
            return data;
        }

        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT block_data FROM block_palette WHERE id = :id")
                        .bind("id", id)
                        .mapTo(byte[].class)
                        .findOne()
                        .orElse(new byte[0])
        );
    }

    /**
     * Gets the material name for a palette ID (e.g. "minecraft:stone").
     */
    public @NotNull String getMaterial(int id) {
        if (id == AIR_ID) {
            return "minecraft:air";
        }
        String mat = idToMaterial.get(id);
        return mat != null ? mat : "minecraft:air";
    }

    /**
     * Returns the number of unique block states in the palette.
     */
    public int size() {
        return hashToId.size();
    }

    /**
     * Gets the JDBI instance for dynamic query building.
     */
    public Jdbi getJdbi() {
        return jdbi;
    }

    private static String computeHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}

