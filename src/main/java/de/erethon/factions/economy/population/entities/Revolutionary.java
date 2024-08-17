package de.erethon.factions.economy.population.entities;

import ca.spottedleaf.moonrise.common.misc.NearbyPlayers;
import de.erethon.factions.faction.Faction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class Revolutionary extends Vindicator {

    private final static double UNREST_REDUCTION_FOR_DEATH = 1.0;

    private final Level level;
    private NearbyPlayers nearby;
    private Random random = new Random();

    private Faction faction;
    private long lastTimeISaidSomething = 0;

    // Required constructor for entity loading
    public Revolutionary(EntityType<? extends Vindicator> type, Level world) {
        super(type, world);
        level = world;
        ServerLevel serverLevel = (ServerLevel) world;
        nearby = serverLevel.getChunkSource().chunkMap.level.moonrise$getNearbyPlayers();
    }

    public Revolutionary(Faction faction, Location location) {
        this(EntityType.VINDICATOR, ((org.bukkit.craftbukkit.CraftWorld) location.getWorld()).getHandle());
        this.faction = faction;
        setPos(location.getX(), location.getY(), location.getZ());
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TORCH));
        getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(48);
        setPersistenceRequired(false);
        drops.clear();
        expToDrop = 0;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, false));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, Raider.class).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.player.Player.class, true, e -> {
            Player player = (Player) e.getBukkitEntity();
            return faction.getMembers().contains(player);
        }));
        // Have them kill farm animals because they are revolutionary
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Cow.class, true));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Pig.class, true));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Sheep.class, true));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Chicken.class, true));
        // Let them "Steal" some blocks. May need some work, they are really aggressive lol
        goalSelector.addGoal(5, new RemoveBlockGoal(Blocks.GOLD_BLOCK, this, 1.0, 6));
        goalSelector.addGoal(5, new RemoveBlockGoal(Blocks.DIAMOND_BLOCK, this, 1.0, 6));
        goalSelector.addGoal(6, new RandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, net.minecraft.world.entity.player.Player.class, 3.0F, 1.0F));
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        faction.setUnrestLevel(Math.max(0, faction.getUnrestLevel() - UNREST_REDUCTION_FOR_DEATH));
    }



    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (lastTimeISaidSomething + 500 < getServer().getTickCount()) {
            lastTimeISaidSomething = getServer().getTickCount();
            nearby.getPlayersByChunk(chunkPosition().x, chunkPosition().z, NearbyPlayers.NearbyMapType.GENERAL_SMALL).forEach(player -> {
                Player nearbyPlayer = player.getBukkitEntity();
                if (faction.getMembers().contains(nearbyPlayer)) {
                    Component message = randomRevolutionaryMessage().decoration(TextDecoration.ITALIC, true).color(NamedTextColor.RED);
                    nearbyPlayer.sendMessage(message);
                }
            });
        }
    }

    private Component randomRevolutionaryMessage() {
        int randomInt = random.nextInt(5);
        return Component.translatable("factions.economy.revolutionary.message." + randomInt);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putString("papyrus-entity-id", "factions_revolutionary");
    }
}
