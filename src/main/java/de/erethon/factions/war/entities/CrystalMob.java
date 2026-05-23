package de.erethon.factions.war.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.jetbrains.annotations.NotNull;

public class CrystalMob extends EndCrystal {

    public CrystalMob(EntityType<? extends EndCrystal> type, Level world) {
        super(type, world);
        setShowBottom(false);
    }

    public CrystalMob(World world, double x, double y, double z) {
        this(EntityType.END_CRYSTAL, ((CraftWorld) world).getHandle());
        Level level = ((CraftWorld) world).getHandle();
        setPos(x, y, z);
        setNoGravity(true);
        setSilent(true);
        setInvulnerable(false);
        setShowBottom(false);
        level.addFreshEntity(this);
    }

    @Override
    public void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("papyrus-entity-id", "factions_crystal_mob");
    }

    public @NotNull EndCrystal getDataCrystal() {
        return this;
    }
}
