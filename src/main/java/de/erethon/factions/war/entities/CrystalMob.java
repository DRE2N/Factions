package de.erethon.factions.war.entities;

import de.erethon.factions.util.FUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.jetbrains.annotations.NotNull;

public class CrystalMob extends Slime {

    private EndCrystal dataCrystal;

    // Required constructor for entity loading
    public CrystalMob(EntityType<? extends Slime> type, Level world) {
        super(type, world);
        //syncAttributes = false;
        dataCrystal = EntityType.END_CRYSTAL.create(world, EntitySpawnReason.COMMAND);
        dataCrystal.setShowBottom(false);
        drops.clear();
    }

    public CrystalMob(World world, double x, double y, double z) {
        this(EntityType.SLIME, ((CraftWorld) world).getHandle());
        //syncAttributes = false;
        Level level = ((CraftWorld) world).getHandle();
        setPos(x, y, z);
        setNoGravity(true);
        setSilent(true);
        setNoAi(true);
        setHealth(getMaxHealth());
        level.addFreshEntity(this);
        dataCrystal = EntityType.END_CRYSTAL.create(level, EntitySpawnReason.COMMAND);
        dataCrystal.setShowBottom(false);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity entity) {
        return FUtil.getAddEntityPacketWithType(this, EntityType.END_CRYSTAL);
    }

    @Override
    public @NotNull SynchedEntityData getEntityData() { // Return the correct entity data so the client isn't confused
        if (dataCrystal == null) {
            return super.getEntityData();
        }
        return dataCrystal.getEntityData();
    }

    @Override
    public void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("papyrus-entity-id", "factions_crystal_mob");
    }

    public @NotNull EndCrystal getDataCrystal() {
        return dataCrystal;
    }
}
