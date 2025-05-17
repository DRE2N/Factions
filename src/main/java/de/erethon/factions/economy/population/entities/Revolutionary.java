package de.erethon.factions.economy.population.entities;

import ca.spottedleaf.moonrise.common.misc.NearbyPlayers;
import com.google.common.base.Predicates;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.economy.FEconomy;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.util.FLogger;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RemoveBlockGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Random;

public class Revolutionary extends Vindicator {

    private final Factions plugin = Factions.get();
    private final Level level;
    private final NearbyPlayers nearby;
    private final Random random = new Random();

    private Faction faction;
    private long lastTimeISaidSomething = 0;

    // Required constructor for entity loading
    public Revolutionary(EntityType<? extends Vindicator> type, Level world) {
        super(type, world);
        level = world;
        ServerLevel serverLevel = (ServerLevel) world;
        nearby = serverLevel.getChunkSource().chunkMap.level.moonrise$getNearbyPlayers();
        drops.clear();
        expToDrop = 0;
        targetSelector.removeAllGoals(Predicates.alwaysTrue());
        goalSelector.removeAllGoals(Predicates.alwaysTrue());
        registerGoals();
    }

    public Revolutionary(Faction faction, Location location) {
        this(EntityType.VINDICATOR, ((org.bukkit.craftbukkit.CraftWorld) location.getWorld()).getHandle());
        this.faction = faction;
        setPos(location.getX(), location.getY(), location.getZ());
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
        setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TORCH));
        getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(48);
        getAttribute(Attributes.ADVANTAGE_PHYSICAL).setBaseValue(10);
        setSilent(true);
        setCustomNameVisible(true);
        setCustomName(Component.literal("Revolutionary"));
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.player.Player.class, false, (LivingEntity e, ServerLevel level) -> {
            Player player = (Player) e.getBukkitEntity();
            FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(player);
            return fPlayer.getFaction() == faction;
        }));
        // Have them kill farm animals because they are revolutionary
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Cow.class, false));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Pig.class, false));
        targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Sheep.class, false));
        targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, Chicken.class, false));
        // Let them "Steal" some blocks. May need some work, they are really aggressive lol
        goalSelector.addGoal(2, new RemoveBlockGoal(Blocks.GOLD_BLOCK, this, 1.0, 6));
        goalSelector.addGoal(3, new RemoveBlockGoal(Blocks.DIAMOND_BLOCK, this, 1.0, 6));
        goalSelector.addGoal(4, new RandomStrollGoal(this, 0.6));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, net.minecraft.world.entity.player.Player.class, 3.0F, 1.0F));
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        faction.setUnrestLevel(Math.max(0, faction.getUnrestLevel() - FEconomy.UNREST_REDUCTION_FOR_REVOLUTIONARY_DEATH));
    }

    @Override
    public void dropPreservedEquipment(ServerLevel level) {
        // Not dropping anything
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (lastTimeISaidSomething + 500 < getServer().getTickCount()) {
            lastTimeISaidSomething = getServer().getTickCount();
            if (random.nextDouble() > 0.01) {
                return;
            }
            nearby.getPlayersByChunk(chunkPosition().x, chunkPosition().z, NearbyPlayers.NearbyMapType.GENERAL_SMALL).forEach(player -> {
                Player nearbyPlayer = player.getBukkitEntity();
                if (faction.getMembers().contains(nearbyPlayer)) {
                    net.kyori.adventure.text.Component message = randomRevolutionaryMessage().decoration(TextDecoration.ITALIC, true).color(NamedTextColor.RED);
                    nearbyPlayer.sendMessage(message);
                }
            });
        }
    }

    private  net.kyori.adventure.text.Component randomRevolutionaryMessage() {
        int randomInt = random.nextInt(5);
        return  net.kyori.adventure.text.Component.translatable("factions.economy.revolutionary.message." + randomInt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        Optional<Integer> factionId = nbt.getInt("factions-faction-id");
        factionId.ifPresent(integer -> faction = plugin.getFactionCache().getById(integer));
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        try { // Just in case the Factions side of things is broken.
            nbt.putString("papyrus-entity-id", "factions_revolutionary");
            nbt.putInt("factions-faction-id", faction.getId());
        } catch (Exception e) {
            FLogger.WAR.log("Failed to save revolt NPC data at " + position().x + ", " + position().y + ", " + position().z);
            remove(RemovalReason.DISCARDED);
            e.printStackTrace();
        }
    }
}
