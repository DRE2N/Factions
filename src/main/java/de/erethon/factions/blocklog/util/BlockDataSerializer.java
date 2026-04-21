package de.erethon.factions.blocklog.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.CraftWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility for serializing and deserializing block data using NMS and NBT.
 * Data is compressed using GZIP for efficient storage.
 * Uses Codec-based serialization for proper block state handling.
 *
 * @author Malfrador
 */
public class BlockDataSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockDataSerializer.class);

    /**
     * Serializes a block to compressed NBT data.
     *
     * @param block The block to serialize
     * @return Gzipped NBT data as byte array
     */
    public static byte[] serializeBlock(Block block) {
        return serializeBlockState(block.getState());
    }

    /**
     * Serializes a Bukkit BlockState snapshot to compressed NBT data.
     * This is safe to use with event snapshots like {@code BlockPlaceEvent.getBlockReplacedState()}.
     *
     * @param blockState The Bukkit BlockState snapshot to serialize
     * @return Gzipped NBT data as byte array
     */
    public static byte[] serializeBlockState(org.bukkit.block.BlockState blockState) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzos = new GZIPOutputStream(baos);
             DataOutputStream dos = new DataOutputStream(gzos)) {

            CraftWorld craftWorld = (CraftWorld) blockState.getWorld();
            ServerLevel level = craftWorld.getHandle();
            BlockPos pos = new BlockPos(blockState.getX(), blockState.getY(), blockState.getZ());

            // Get the NMS block state from the Bukkit BlockState snapshot
            BlockState nmsState = ((org.bukkit.craftbukkit.block.CraftBlockState) blockState).getHandle();

            RegistryOps<Tag> registryOps = MinecraftServer.getServer().registryAccess()
                    .createSerializationContext(NbtOps.INSTANCE);

            // Serialize block state using Codec
            CompoundTag stateTag = (CompoundTag) BlockState.CODEC
                    .encodeStart(registryOps, nmsState)
                    .resultOrPartial(error -> {})
                    .orElseGet(CompoundTag::new);

            NbtIo.write(stateTag, dos);

            // Check for block entity from the snapshot
            // CraftBlockEntityState holds a snapshot of the tile entity
            if (blockState instanceof org.bukkit.craftbukkit.block.CraftBlockEntityState<?> craftBEState) {
                dos.writeBoolean(true);
                CompoundTag blockEntityTag = craftBEState.getSnapshotNBT();
                NbtIo.write(blockEntityTag, dos);
            } else {
                dos.writeBoolean(false);
            }

            gzos.finish();
            return baos.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    /**
     * Serializes a block state string (e.g. "minecraft:oak_stairs[facing=east]")
     * into the same compressed format used by the palette.
     */
    public static byte[] serializeBlockDataString(String blockDataString) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzos = new GZIPOutputStream(baos);
             DataOutputStream dos = new DataOutputStream(gzos)) {

            if (blockDataString == null || blockDataString.isBlank() || "minecraft:air".equals(blockDataString)) {
                return new byte[0];
            }

            BlockData bukkitData = Bukkit.createBlockData(blockDataString);
            BlockState nmsState = ((CraftBlockData) bukkitData).getState();

            RegistryOps<Tag> registryOps = MinecraftServer.getServer().registryAccess()
                    .createSerializationContext(NbtOps.INSTANCE);

            CompoundTag stateTag = (CompoundTag) BlockState.CODEC
                    .encodeStart(registryOps, nmsState)
                    .resultOrPartial(error -> {})
                    .orElseGet(CompoundTag::new);

            NbtIo.write(stateTag, dos);
            dos.writeBoolean(false); // WorldEdit hook does not track block-entity NBT in this path.

            gzos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    /**
     * Deserializes compressed NBT data to a CompoundTag.
     *
     * @param data Gzipped NBT data
     * @return The decompressed CompoundTag
     */
    public static CompoundTag deserializeToNBT(byte[] data) {
        try {
            if (data == null || data.length == 0) {
                return new CompoundTag();
            }

            try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                 GZIPInputStream gzis = new GZIPInputStream(bais);
                 DataInputStream dis = new DataInputStream(gzis)) {

                return NbtIo.read(dis);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new CompoundTag();
        }
    }

    /**
     * Applies serialized block data to a location.
     *
     * @param location The location to apply the block to
     * @param data The compressed NBT data
     */
    public static void applyBlockData(Location location, byte[] data) {
        try {
            if (data == null || data.length == 0) {
                return;
            }

            CraftWorld craftWorld = (CraftWorld) location.getWorld();
            ServerLevel level = craftWorld.getHandle();
            BlockPos pos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());

            try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                 GZIPInputStream gzis = new GZIPInputStream(bais);
                 DataInputStream dis = new DataInputStream(gzis)) {

                RegistryOps<Tag> registryOps = MinecraftServer.getServer().registryAccess()
                        .createSerializationContext(NbtOps.INSTANCE);

                // Read block state
                CompoundTag stateTag = NbtIo.read(dis);

                BlockState state = BlockState.CODEC
                        .parse(registryOps, stateTag)
                        .resultOrPartial(error -> {})
                        .orElse(Blocks.AIR.defaultBlockState());

                // Set block state
                level.setBlock(pos, state, 3);

                // Read block entity if present
                boolean hasBlockEntity = dis.readBoolean();
                if (hasBlockEntity) {
                    CompoundTag blockEntityTag = NbtIo.read(dis);
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity != null) {
                        try (ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(LOGGER)) {
                            HolderLookup.Provider provider = level.registryAccess();
                            TagValueInput tagValueInput = (TagValueInput) TagValueInput.create(scopedCollector, provider, blockEntityTag);
                            blockEntity.loadWithComponents(tagValueInput);
                            blockEntity.setChanged();
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets a human-readable string representation of block data.
     */
    public static String getBlockDescription(byte[] data) {
        try {
            if (data == null || data.length == 0) {
                return "AIR";
            }

            try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                 GZIPInputStream gzis = new GZIPInputStream(bais);
                 DataInputStream dis = new DataInputStream(gzis)) {

                RegistryOps<Tag> registryOps = MinecraftServer.getServer().registryAccess()
                        .createSerializationContext(NbtOps.INSTANCE);

                CompoundTag stateTag = NbtIo.read(dis);

                BlockState state = BlockState.CODEC
                        .parse(registryOps, stateTag)
                        .resultOrPartial(error -> {})
                        .orElse(Blocks.AIR.defaultBlockState());

                return state.toString();
            }
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    /**
     * Extracts the material name (e.g. "minecraft:stone") from compressed block data.
     * This reads only the Name field from the NBT without full deserialization.
     */
    public static String getMaterialName(byte[] data) {
        try {
            if (data == null || data.length == 0) {
                return "minecraft:air";
            }

            try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                 GZIPInputStream gzis = new GZIPInputStream(bais);
                 DataInputStream dis = new DataInputStream(gzis)) {

                CompoundTag stateTag = NbtIo.read(dis);

                // BlockState codec stores as {"Name": "minecraft:stone", "Properties": {...}}
                return stateTag.getString("Name").orElse("minecraft:air");
            }
        } catch (Exception e) {
            return "minecraft:air";
        }
    }
}
