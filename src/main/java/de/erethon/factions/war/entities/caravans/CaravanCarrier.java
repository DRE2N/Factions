package de.erethon.factions.war.entities.caravans;

import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.entity.Relation;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.util.FLogger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class CaravanCarrier extends Ravager {

    private static final double NODE_REACHED_DISTANCE = 2.25;
    private static final double ROUTE_SPEED = 1.0;
    private static final double STUCK_PROGRESS_EPSILON = 0.5;
    private static final int STUCK_TELEPORT_TICKS = 20 * 12;
    private static final int GUARD_COUNT = 2;
    private static final double GUARD_SLOT_TOLERANCE = 3.0;
    private static final double GUARD_TELEPORT_DISTANCE = 24.0;
    private static final double GUARD_SPEED = 1.15;

    private final Factions plugin = Factions.get();
    private Alliance alliance;

    private boolean hasSpawnedReinforcements = false;
    private ActiveCaravanRoute route;
    private CaravanRouting routing;
    private CaravanRouteNode lastTargetNode;
    private double bestDistanceToTarget = Double.MAX_VALUE;
    private int ticksWithoutRouteProgress = 0;
    private Display.ItemDisplay bannerDisplay;
    private final List<CaravanGuard> guards = new ArrayList<>();

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
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.33);
        setHealth(getMaxHealth());
        setNoAi(false);
        persistenceRequired = false;
    }

    public void startVisuals() {
        spawnBanner();
        spawnGuards();
        updateRouteTarget(true);
    }

    private void spawnBanner() {
        if (bannerDisplay != null || alliance == null) {
            return;
        }
        bannerDisplay = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level());
        ItemStack bannerStack;
        switch (alliance.getBossBarColor()) {
            case RED -> bannerStack = new ItemStack(Items.RED_BANNER);
            case BLUE -> bannerStack = new ItemStack(Items.BLUE_BANNER);
            case GREEN -> bannerStack = new ItemStack(Items.GREEN_BANNER);
            default -> bannerStack = new ItemStack(Items.WHITE_BANNER);
        }
        bannerDisplay.setItemStack(bannerStack);
        bannerDisplay.setPos(getX(), getY() + 1.5, getZ());
        level().addFreshEntity(bannerDisplay);
        bannerDisplay.persist = false;
        bannerDisplay.startRiding(this, true, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new CaravanRouteGoal());
    }

    @Override
    public void tick() {
        super.tick();
        updateGuards();
    }

    private void updateRouteTarget(boolean force) {
        if (route == null || routing == null || route.route().nodes().length == 0 || route.isAtEnd()) {
            return;
        }
        CaravanRouteNode node = route.nextNode();
        if (isAtNode(node)) {
            routing.onVisualReachedNextNode(route, this);
            if (isRemoved() || route == null || route.isAtEnd()) {
                return;
            }
            node = route.nextNode();
            force = true;
        }
        if (!force && node.equals(lastTargetNode) && !needsNavigationRefresh()) {
            checkStuck(node);
            return;
        }
        getNavigation().moveTo(node.x() + 0.5, node.y(), node.z() + 0.5, ROUTE_SPEED);
        if (!node.equals(lastTargetNode)) {
            bestDistanceToTarget = distanceSquaredToNode(node);
            ticksWithoutRouteProgress = 0;
        }
        lastTargetNode = node;
        checkStuck(node);
    }

    private boolean needsNavigationRefresh() {
        return getNavigation().isDone() || getNavigation().getPath() == null;
    }

    private boolean isAtNode(CaravanRouteNode node) {
        return distanceSquaredToNode(node) <= NODE_REACHED_DISTANCE * NODE_REACHED_DISTANCE;
    }

    private void checkStuck(CaravanRouteNode node) {
        double distance = distanceSquaredToNode(node);
        if (distance + STUCK_PROGRESS_EPSILON < bestDistanceToTarget) {
            bestDistanceToTarget = distance;
            ticksWithoutRouteProgress = 0;
            return;
        }
        ticksWithoutRouteProgress++;
        if (ticksWithoutRouteProgress < STUCK_TELEPORT_TICKS) {
            return;
        }
        getNavigation().stop();
        setPos(node.x() + 0.5, node.y(), node.z() + 0.5);
        bestDistanceToTarget = 0;
        ticksWithoutRouteProgress = 0;
        routing.onVisualReachedNextNode(route, this);
    }

    private double distanceSquaredToNode(CaravanRouteNode node) {
        double dx = getX() - (node.x() + 0.5);
        double dz = getZ() - (node.z() + 0.5);
        return dx * dx + dz * dz;
    }

    private void spawnGuards() {
        if (!guards.isEmpty()) {
            return;
        }
        for (int i = 0; i < GUARD_COUNT; i++) {
            CaravanGuard guard = new CaravanGuard(this);
            double angle = (Math.PI * 2 / GUARD_COUNT) * i;
            guard.setPos(getX() + Math.cos(angle) * 2.0, getY(), getZ() + Math.sin(angle) * 2.0);
            guards.add(guard);
        }
    }

    private void updateGuards() {
        guards.removeIf(guard -> guard == null || !guard.isAlive() || guard.isRemoved());
        if (isRemoved()) {
            discardGuards();
            return;
        }
        for (int i = 0; i < guards.size(); i++) {
            CaravanGuard guard = guards.get(i);
            double distance = guard.distanceToSqr(this);
            if (distance > GUARD_TELEPORT_DISTANCE * GUARD_TELEPORT_DISTANCE) {
                guard.getNavigation().stop();
                EscortSlot slot = escortSlot(i);
                guard.setPos(slot.x(), getY(), slot.z());
                continue;
            }
            if (guard.getTarget() == null) {
                EscortSlot slot = escortSlot(i);
                double dx = guard.getX() - slot.x();
                double dz = guard.getZ() - slot.z();
                if (dx * dx + dz * dz > GUARD_SLOT_TOLERANCE * GUARD_SLOT_TOLERANCE || guard.getNavigation().isDone()) {
                    guard.getNavigation().moveTo(slot.x(), getY(), slot.z(), GUARD_SPEED);
                }
            }
        }
    }

    private EscortSlot escortSlot(int index) {
        double targetX = lastTargetNode == null ? getX() : lastTargetNode.x() + 0.5;
        double targetZ = lastTargetNode == null ? getZ() + 1.0 : lastTargetNode.z() + 0.5;
        double forwardX = targetX - getX();
        double forwardZ = targetZ - getZ();
        double length = Math.sqrt(forwardX * forwardX + forwardZ * forwardZ);
        if (length < 0.01) {
            forwardX = 0.0;
            forwardZ = 1.0;
            length = 1.0;
        }
        forwardX /= length;
        forwardZ /= length;
        double sideX = -forwardZ;
        double sideZ = forwardX;
        double side = index % 2 == 0 ? -1.0 : 1.0;
        return new EscortSlot(
                getX() - forwardX * 3.0 + sideX * side * 3.0,
                getZ() - forwardZ * 3.0 + sideZ * side * 3.0
        );
    }

    @Override
    public boolean hurtServer(ServerLevel level, @NotNull DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && !isFactionEnemy(player, level)) {
            return false;
        }
        if (!hasSpawnedReinforcements && source.getEntity() instanceof Player player && isFactionEnemy(player, level)) {
            hasSpawnedReinforcements = true;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    public void discardVisuals() {
        for (Entity passenger : List.copyOf(getPassengers())) {
            passenger.stopRiding();
            passenger.discard();
        }
        if (bannerDisplay != null) {
            bannerDisplay.stopRiding();
            bannerDisplay.discard();
            bannerDisplay = null;
        }
        discardGuards();
    }

    private void discardGuards() {
        for (CaravanGuard guard : List.copyOf(guards)) {
            if (guard != null) {
                guard.discard();
            }
        }
        guards.clear();
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (routing != null && route != null) {
            routing.removeRouteWithRealCaravan(route);
        }
        discardVisuals();
        super.remove(reason);
    }

    private class CaravanRouteGoal extends Goal {

        private CaravanRouteGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return route != null && routing != null && route.route().nodes().length > 0;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            updateRouteTarget(true);
        }

        @Override
        public void tick() {
            updateRouteTarget(false);
        }
    }

    private record EscortSlot(double x, double z) {
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
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        Optional<Integer> id = input.getInt("factions-alliance-id");
        if (id.isPresent()) {
            alliance = plugin.getAllianceCache().getById(id.get());
            readyUp();
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);
        try { // Just in case the Factions side of things is broken.
            output.putString("papyrus-entity-id", "factions_caravan_carrier");
            output.putInt("factions-alliance-id", alliance.getId());
        } catch (Exception e) {
            FLogger.WAR.log("Failed to save crystal charge carrier data at " + position().x + ", " + position().y + ", " + position().z);
            e.printStackTrace();
        }
    }
}
