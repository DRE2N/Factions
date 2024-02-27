package de.erethon.factions.war.entities;

import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.entity.Relation;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.policy.FPolicy;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FLogger;
import net.kyori.adventure.text.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class CrystalChargeCarrier extends IronGolem {

    public static NamespacedKey CARRIER_KEY = new NamespacedKey(Factions.get(), "crystal-charge-carrier");
    public static NamespacedKey CARRIER_PLAYER_KEY = new NamespacedKey(Factions.get(), "crystal-charge-carrier");
    public static final AttributeModifier CARRIER_DEBUFF = new AttributeModifier("factions-carrier-damage-debuff", -1000, AttributeModifier.Operation.ADD_NUMBER);
    public static final AttributeModifier CARRIER_BUFF = new AttributeModifier("factions-carrier-speed-buff", Factions.get().getFConfig().getCrystalCarrierSpeedBuff(), AttributeModifier.Operation.ADD_NUMBER);

    private final Factions plugin = Factions.get();
    private Region region;
    private Alliance alliance;

    // Required constructor for entity loading
    public CrystalChargeCarrier(EntityType<? extends IronGolem> type, Level world) {
        super(type, world);
    }

    public CrystalChargeCarrier(World world, Location location, Region region, Alliance alliance) {
        this(EntityType.IRON_GOLEM, ((CraftWorld) world).getHandle(), region, alliance, location.getX(), location.getY(), location.getZ());
    }

    public CrystalChargeCarrier(EntityType<? extends IronGolem> type, Level world, Region region, Alliance alliance, double x, double y, double z) {
        super(type, world);
        setPos(x, y, z);
        this.region = region;
        this.alliance = alliance;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(plugin.getFConfig().getDefaultCrystalCarrierHealth());
        if (alliance.hasPolicy(FPolicy.CRYSTAL_CARRIER_HEALTH_BUFF)) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(getAttribute(Attributes.MAX_HEALTH).getBaseValue() * 1.2);
        }
        world.addFreshEntity(this);
        setPersistenceRequired(true);
        getBukkitEntity().getPersistentDataContainer().set(CARRIER_KEY, PersistentDataType.BYTE, (byte) 1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 0.9D, 32.0F));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.6D));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isFactionEnemy));
    }

    private boolean isFactionEnemy(LivingEntity entity) {
        if (region == null || alliance == null) { // Just in so we don't throw an entity ticking exception.
            return false;
        }
        if (entity instanceof Player player) {
            FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer((org.bukkit.entity.Player) player.getBukkitEntity());
            return region.getFaction() != null && fPlayer.getRelation(alliance) == Relation.ENEMY;
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player) {
            org.bukkit.entity.Player bukkitPlayer = (org.bukkit.entity.Player) player.getBukkitEntity();
            FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(bukkitPlayer);
            if (fPlayer.getFaction() == null) {
                bukkitPlayer.sendMessage(Component.translatable("factions.war.carrier.noFaction"));
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if (damageSource.getEntity() instanceof Player player) {
            org.bukkit.entity.Player bukkitPlayer = (org.bukkit.entity.Player) player.getBukkitEntity();
            bukkitPlayer.getPersistentDataContainer().set(CARRIER_PLAYER_KEY, PersistentDataType.BYTE, (byte) 1);
            bukkitPlayer.sendMessage(Component.translatable("factions.war.carrier.killed"));
            bukkitPlayer.sendMessage(Component.translatable("factions.war.carrier.killedHint"));
            bukkitPlayer.setGlowing(true);
            bukkitPlayer.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addTransientModifier(CARRIER_BUFF);
            bukkitPlayer.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).addTransientModifier(CARRIER_DEBUFF);
            bukkitPlayer.getAttribute(Attribute.ADV_PHYSICAL).addTransientModifier(CARRIER_DEBUFF);
            bukkitPlayer.getAttribute(Attribute.ADV_MAGIC).addTransientModifier(CARRIER_DEBUFF);
            region.getRegionalWarTracker().addCrystalCarrier(bukkitPlayer);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        region = plugin.getRegionManager().getRegionById(nbt.getInt("factions-region-id"));
        alliance = plugin.getAllianceCache().getById(nbt.getInt("factions-alliance-id"));
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        try { // Just in case the Factions side of things is broken.
            nbt.putString("papyrus-entity-id", "crystal_charge_carrier");
            nbt.putInt("factions-region-id", region.getId());
            nbt.putInt("factions-alliance-id", alliance.getId());
        } catch (Exception e) {
            FLogger.WAR.log("Failed to save crystal charge carrier data at " + position().x + ", " + position().y + ", " + position().z);
            e.printStackTrace();
        }
    }
}
