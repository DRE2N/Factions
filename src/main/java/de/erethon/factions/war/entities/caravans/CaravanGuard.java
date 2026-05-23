package de.erethon.factions.war.entities.caravans;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.util.FUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class CaravanGuard extends Vindicator {

    private Factions plugin = Factions.get();

    private ServerPlayer dataPlayer;
    private ServerLevel serverLevel;
    private CaravanCarrier carrier;
    private boolean caravanGoalsRegistered;

    public CaravanGuard(EntityType<? extends Vindicator> type, Level world) {
        super(type, world);
        createPlayerStuff((ServerLevel) world);
        serverLevel = (ServerLevel) world;
    }

    public CaravanGuard(CaravanCarrier carrier) {
        this(EntityType.VINDICATOR, carrier.level());
        this.carrier = carrier;
        //syncAttributes = false; Don't think we still need this
        setPos(carrier.getX(), carrier.getY(), carrier.getZ());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(plugin.getFConfig().getDefaultObjectiveGuardHealth());
        setHealth(getMaxHealth());
        createPlayerStuff((ServerLevel) carrier.level());
        setSilent(true); // Vindicators are loud and annoying
        if (dataPlayer == null) {
            FLogger.WAR.log("Failed to create a fake player for a caravan guard");
            return;
        }
        registerCaravanGoals();
        carrier.level().addFreshEntity(this);
        persistenceRequired = false;
    }

    private void createPlayerStuff(ServerLevel level) {
        CraftPlayerProfile craftPlayerProfile = new CraftPlayerProfile(uuid, "Guard");
        this.dataPlayer = new ServerPlayer(MinecraftServer.getServer(), level, craftPlayerProfile.buildGameProfile(), new ClientInformation("en", 0, ChatVisiblity.SYSTEM, false, 1, HumanoidArm.RIGHT, false, false, ParticleStatus.ALL));
        dataPlayer.getInventory().add(100, new ItemStack(Items.CHAINMAIL_BOOTS));
        dataPlayer.getInventory().add(101, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        dataPlayer.getInventory().add(102, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        dataPlayer.getInventory().add(98, new ItemStack(Items.IRON_SWORD));
    }

    @Override
    protected void registerGoals() {
        // Called from the Mob superclass constructor before our carrier field can be assigned.
    }

    private void registerCaravanGoals() {
        if (caravanGoalsRegistered || carrier == null) {
            return;
        }
        caravanGoalsRegistered = true;
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, carrier::isFactionEnemy));
    }

    public CaravanCarrier getCarrier() {
        return carrier;
    }

    @Override
    public void tick() {
        super.tick();
        if (carrier == null || carrier.isRemoved() || !carrier.isAlive()) {
            discard();
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(@NotNull Entity entity) {
        if (entity instanceof CaravanCarrier || entity instanceof CaravanGuard) {
            return;
        }
        super.doPush(entity);
    }

    @Override
    public boolean hurtServer(ServerLevel level, @NotNull DamageSource source, float amount) {
        if (carrier != null && source.getEntity() instanceof Player player && !carrier.isFactionEnemy(player, level)) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity entity) {
        level().players().forEach(player -> {
            if (player instanceof ServerPlayer) {
                ClientboundPlayerInfoUpdatePacket infoUpdatePacket = new ClientboundPlayerInfoUpdatePacket(
                        EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                        new ClientboundPlayerInfoUpdatePacket.Entry(dataPlayer.getUUID(), dataPlayer.getGameProfile(), false, 0, GameType.SURVIVAL, Component.empty(), true, 1, null));
                ((ServerPlayer) player).connection.send(infoUpdatePacket);
            }
        });
        return FUtil.getAddEntityPacketWithType(this, EntityType.PLAYER);
    }

    @Override
    public @NotNull SynchedEntityData getEntityData() {
        if (dataPlayer == null) {
            Factions.log("Tried to get entity data for a caravan guard without a fake player");
            return super.getEntityData();
        }
        return dataPlayer.getEntityData();
    }


    @Override
    public void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (carrier != null) {
            output.putString("factions-caravan-uuid", carrier.getUUID().toString());
        }
    }
}
