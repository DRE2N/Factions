package de.erethon.factions.war.entities;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.entity.Relation;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.policy.FPolicy;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionStructure;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.util.FUtil;
import io.papermc.paper.math.Position;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Optional;

public class ObjectiveGuard extends Vindicator {

    private Factions plugin = Factions.get();

    private ServerPlayer dataPlayer;
    private Alliance alliance;
    private Region region;
    private RegionStructure structure;

    public ObjectiveGuard(EntityType<? extends Vindicator> type, Level world) {
        super(type, world);
        createPlayerStuff((ServerLevel) world);
    }

    public ObjectiveGuard(World world, double x, double y, double z, Region region, Alliance alliance, RegionStructure structure) {
        this(EntityType.VINDICATOR, ((CraftWorld) world).getHandle());
        if (structure == null) {
            FLogger.WAR.log("Tried to spawn an objective guard without a structure at " + x + ", " + y + ", " + z);
            return;
        }
        this.region = region;
        this.alliance = alliance;
        this.structure = structure;
        //syncAttributes = false;
        ServerLevel level = ((CraftWorld) world).getHandle();
        setPos(x, y, z);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(plugin.getFConfig().getDefaultObjectiveGuardHealth());
        if (alliance.hasPolicy(FPolicy.STRONGER_OBJECTIVE_GUARDS)) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(getMaxHealth() * 1.5);
        }
        if (alliance.hasPolicy(FPolicy.OBJECTIVE_GUARDS_REGEN)) {
            getAttribute(Attributes.STAT_HEALTH_REGEN).setBaseValue(0.1);
        }
        setHealth(getMaxHealth());
        createPlayerStuff(level);
        setSilent(true); // Vindicators are loud and annoying
        if (dataPlayer == null) {
            FLogger.WAR.log("Failed to create a fake player for an objective guard");
            return;
        }
        level.addFreshEntity(this);
        Position pos = structure.getCenterPosition();
        setHomeTo(new BlockPos((int) pos.x(), (int) pos.y(), (int) pos.z()), 32);
    }

    private void createPlayerStuff(ServerLevel level) {
        CraftPlayerProfile craftPlayerProfile = new CraftPlayerProfile(uuid, "Objective Guard");
        this.dataPlayer = new ServerPlayer(MinecraftServer.getServer(), level, craftPlayerProfile.buildGameProfile(), new ClientInformation("en", 0, ChatVisiblity.SYSTEM, false, 1, HumanoidArm.RIGHT, false, false, ParticleStatus.ALL));
        dataPlayer.getInventory().add(100, new ItemStack(Items.CHAINMAIL_BOOTS));
        dataPlayer.getInventory().add(101, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        dataPlayer.getInventory().add(102, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        dataPlayer.getInventory().add(98, new ItemStack(Items.IRON_SWORD));
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 1.0, 3)); // Do we need this and the melee attack goal?
        goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 1.3));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.9));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isFactionEnemy));
    }

    private boolean isFactionEnemy(LivingEntity entity, ServerLevel level) {
        if (region == null || alliance == null) { // Just so we don't throw an entity ticking exception.
            return false;
        }
        if (entity instanceof Player player) {
            FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer((org.bukkit.entity.Player) player.getBukkitEntity());
            return region.getFaction() != null && fPlayer.getRelation(alliance) == Relation.ENEMY;
        }
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
            Factions.log("Tried to get entity data for an objective guard without a fake player");
            return super.getEntityData();
        }
        return dataPlayer.getEntityData();
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        Optional<Integer> regionId = input.getInt("factions-region-id");
        regionId.ifPresent(id -> {
            region = plugin.getRegionManager().getRegionById(id);
            if (region == null) {
                FLogger.WAR.log("Failed to load crystal charge carrier data at " + position().x + ", " + position().y + ", " + position().z);
            }
        });
        Optional<Integer> allianceId = input.getInt("factions-alliance-id");
        allianceId.ifPresent(id -> {
            alliance = plugin.getAllianceCache().getById(id);
            if (alliance == null) {
                FLogger.WAR.log("Failed to load crystal charge carrier data at " + position().x + ", " + position().y + ", " + position().z);
            }
        });
    }


    @Override
    public void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("papyrus-entity-id", "factions_objective_guard");
        try { // Just in case the Factions side of things is broken.
            output.putInt("factions-region-id", region.getId());
            output.putInt("factions-alliance-id", alliance.getId());
        } catch (Exception e) {
            FLogger.WAR.log("Failed to save objective guard at " + position().x + ", " + position().y + ", " + position().z);
            e.printStackTrace();
        }
    }
}
