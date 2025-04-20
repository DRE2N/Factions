package de.erethon.factions.war.entities.caravans;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.entity.Relation;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.util.FLogger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Random;

public class CaravanCarrier extends Ravager {

    private static final int MIN_GUARDIANS = 2;
    private static final int MAX_GUARDIANS = 6;
    private static final double MIN_NODE_DISTANCE = 3;

    private final Factions plugin = Factions.get();
    private Alliance alliance;

    private boolean hasSpawnedReinforcements = false;
    private ActiveCaravanRoute route;
    private CaravanRouting routing;
    private long lastNavigationCheckTick = 0;

    // Required constructor for entity loading
    public CaravanCarrier(EntityType<? extends Ravager> type, Level world) {
        super(type, world);
        drops.clear();
    }

    public CaravanCarrier(World world, double x, double y, double z, Alliance alliance, ActiveCaravanRoute route, CaravanRouting routing) {
        this(EntityType.RAVAGER, ((CraftWorld) world).getHandle());
        //syncAttributes = false;
        setPos(x, y, z);
        this.alliance = alliance;
        this.route = route;
        this.routing = routing;
        readyUp();
    }

    private void readyUp() {
        spawnGuardians();
        Display.ItemDisplay itemDisplay = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level());
        ItemStack bannerStack;
        switch (alliance.getBossBarColor()) {
            case RED -> bannerStack = new ItemStack(Items.RED_BANNER);
            case BLUE -> bannerStack = new ItemStack(Items.BLUE_BANNER);
            case GREEN -> bannerStack = new ItemStack(Items.GREEN_BANNER);
            default -> bannerStack = new ItemStack(Items.WHITE_BANNER);
        }
        itemDisplay.setItemStack(bannerStack);
        addPassenger(itemDisplay);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);
        setHealth(getMaxHealth());
        setPersistenceRequired(false);
    }

    @Override
    protected void registerGoals() {
        // None
    }

    private void hasArrivedAtCaravanNode() {
        CaravanRouteNode[] nodes = route.route().nodes();
        if (nodes.length == 0) {
            routing.onCaravanArrived(route);
            remove(RemovalReason.DISCARDED);
            MessageUtil.log("Caravan for " + alliance.getName() + " from " + route.route().start().getName() + " has arrived at " + route.route().end().getName() + " but had no nodes");
            return;
        }
        if (route.isAtEnd()) {
            routing.onCaravanArrived(route);
            remove(RemovalReason.DISCARDED);
            MessageUtil.log("Caravan for " + alliance.getName() + " from " + route.route().start().getName() + " has arrived at " + route.route().end().getName());
            return;
        }
        route.advance();
        CaravanRouteNode nextNode = route.currentNode();
        getNavigation().moveTo(nextNode.x(), nextNode.y(), nextNode.z(), 0.9);
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount - lastNavigationCheckTick < 20) { // We really don't need to check every tick
            return;
        }
        if (getNavigation().isDone() || getNavigation().getPath() == null) {
            hasArrivedAtCaravanNode();
            lastNavigationCheckTick = tickCount;
            return;
        }
        if (getNavigation().getPath().getDistToTarget() < MIN_NODE_DISTANCE) {
            hasArrivedAtCaravanNode();
        }
        lastNavigationCheckTick = tickCount;
    }

    @Override
    public boolean hurtServer(ServerLevel level, @NotNull DamageSource source, float amount) {
        if (!hasSpawnedReinforcements && source.getEntity() instanceof Player player && isFactionEnemy(player, level)) {
            hasSpawnedReinforcements = true;
            spawnGuardians();
        }
        return super.hurtServer(level, source, amount);
    }

    public void spawnGuardians() {
        Random random = new Random();
        int guardians = random.nextInt(MAX_GUARDIANS - MIN_GUARDIANS) + MIN_GUARDIANS;
        for (int i = 0; i < guardians; i++) {
            CaravanGuard guard = new CaravanGuard(this);
            double x = getX() + random.nextInt(5) - 2;
            double y = getY();
            double z = getZ() + random.nextInt(5) - 2;
            guard.setPos(x, y, z);
            if (getPassengers().isEmpty()) { // Lets put one of the guards on the carrier because it looks cool
                guard.startRiding(this, true);
            }
        }
    }

    public boolean isFactionEnemy(LivingEntity entity, ServerLevel level) {
        if (alliance == null) { // Just so we don't throw an entity ticking exception.
            return false;
        }
        if (entity instanceof Player player) {
            FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer((org.bukkit.entity.Player) player.getBukkitEntity());
            return fPlayer.getRelation(alliance) == Relation.ENEMY;
        }
        return false;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        Optional<Integer> id = nbt.getInt("factions-alliance-id");
        if (id.isPresent()) {
            alliance = plugin.getAllianceCache().getById(id.get());
            readyUp();
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        try { // Just in case the Factions side of things is broken.
            nbt.putString("papyrus-entity-id", "factions_caravan_carrier");
            nbt.putInt("factions-alliance-id", alliance.getId());
        } catch (Exception e) {
            FLogger.WAR.log("Failed to save crystal charge carrier data at " + position().x + ", " + position().y + ", " + position().z);
            e.printStackTrace();
        }
        routing.removeRouteWithRealCaravan(route); // We are being removed, notify the routing that it now has to process us virtually again
    }
}
