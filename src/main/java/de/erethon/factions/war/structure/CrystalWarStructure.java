package de.erethon.factions.war.structure;

import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.entity.Relation;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.policy.FPolicy;
import de.erethon.factions.region.LazyChunk;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FBroadcastUtil;
import de.erethon.factions.war.entities.CrystalChargeCarrier;
import de.erethon.factions.war.entities.CrystalMob;
import io.papermc.paper.math.Position;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.minecraft.world.entity.Entity;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author Fyreum, Malfrador
 */
public class CrystalWarStructure extends TickingWarStructure implements Listener {

    /* Settings */
    protected Alliance alliance;
    protected double energyLossOnDamage;
    protected double energyLossPerInterval;
    protected double maxEnergy;
    protected double energyGainPerCarrier;
    protected double energyLossForCarrierSpawn;
    /* Temporary */
    protected CrystalMob crystal;
    protected double energy;
    protected double energyAtLastCarrierSpawn;
    protected TextDisplay energyDisplay;
    protected Location crystalLocation;
    protected Set<CrystalChargeCarrier> carriers = new HashSet<>();
    protected boolean defenderCrystal;

    public CrystalWarStructure(@NotNull Region region, @NotNull ConfigurationSection config) {
        super(region, config);
    }

    public CrystalWarStructure(@NotNull Region region, @NotNull ConfigurationSection config, @NotNull Position a, @NotNull Position b) {
        super(region, config, a, b);
    }

    @Override
    protected void load(@NotNull ConfigurationSection config) {
        this.alliance = plugin.getAllianceCache().getById(config.getInt("alliance", -1));
        this.energyLossOnDamage = config.getDouble("energyLossOnDamage", 10.0);
        this.energyLossPerInterval = config.getDouble("energyLossPerInterval", 1.0);
        this.maxEnergy = config.getDouble("maxEnergy", 600.0);
        this.energyGainPerCarrier = config.getDouble("energyGainPerCarrier", 200.0);
        this.energyLossForCarrierSpawn = config.getDouble("energyLossForCarrierSpawn", 180.0);
        this.energy = maxEnergy;

        World world = region.getWorld();
        int x = (getXRange().getMaximumInteger() + getXRange().getMinimumInteger()) / 2,
                y = getYRange().getMaximumInteger(),
                z = (getZRange().getMaximumInteger() + getZRange().getMinimumInteger()) / 2;

        while (world.getType(x, y, z).isEmpty()) {
            if (--y < yRange.getMinimumInteger()) {
                break;
            }
        }
        crystalLocation = new Location(world, x, y + 1, z);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void tick() {
        removeEnergy(defenderCrystal ? energyLossPerInterval : energyLossPerInterval * 1.25, null);
        if (energy - energyAtLastCarrierSpawn >= energyLossForCarrierSpawn) {
            spawnCrystalCarrier();
            energyAtLastCarrierSpawn = energy;
        }
    }

    public void damage(double damage, @Nullable FPlayer damager) {
        double energyLoss = energyLossOnDamage;
        if (alliance != null && alliance.hasPolicy(FPolicy.CRYSTAL_DAMAGE_REDUCTION)) {
            energyLoss *= 0.5;
        }
        removeEnergy(energyLoss, damager);
    }

    public void destroy(@Nullable FPlayer damager) {
        if (damager != null) {
            FBroadcastUtil.broadcastWar(FMessage.WAR_OBJECTIVE_DESYTROYED_BY_PLAYER, alliance.getDisplayShortName(), region.getName(true), damager.getLastName());
        } else {
            FBroadcastUtil.broadcastWar(FMessage.WAR_OBJECTIVE_DESYTROYED, alliance.getDisplayShortName(), region.getName(true));
        }
        deactivate();
        deleteStructure();
        crystalLocation.createExplosion(4f, false, false);
    }

    @EventHandler
    private void onInteract(PlayerInteractEntityEvent event) {
        if (((CraftEntity) event.getRightClicked()).getHandle() != crystal) {
            return;
        }
        Player player = event.getPlayer();
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(player);
        if (!player.getPersistentDataContainer().has(CrystalChargeCarrier.CARRIER_PLAYER_KEY) || fPlayer.getFaction().getRelation(alliance) == Relation.ENEMY) {
            return;
        }
        handleCarrierDeposit(player);
    }

    @EventHandler
    private void onDeath(PlayerDeathEvent event) {
        if (region.getRegionalWarTracker().isCrystalCarrier(event.getPlayer())) {
            region.getRegionalWarTracker().removeCrystalCarrier(event.getPlayer());
        }
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        region.getRegionalWarTracker().removeCrystalCarrier(player);
        player.getPersistentDataContainer().remove(CrystalChargeCarrier.CARRIER_PLAYER_KEY);
        player.setGlowing(false);
        // The rest of the buffs/debuffs doesn't get saved anyway.
    }

    public void spawnCrystalCarrier() {
        double locationX = 0, locationZ = 0;
        Random random = new Random();
        Chunk chunk = null;
        int chunkIndex = random.nextInt(region.getChunks().size());
        int i = 0;
        for (LazyChunk lc : region.getChunks()) {
            if (i++ == chunkIndex) {
                locationX = lc.getX() * 16 + random.nextDouble(16);
                locationZ = lc.getZ() * 16 + random.nextDouble(16);
                chunk = lc.asBukkitChunk(region.getWorld());
                break;
            }
        }
        if (chunk == null) {
            return;
        }
        World world = region.getWorld();
        double finalLocationX = locationX;
        double finalLocationZ = locationZ;
        CompletableFuture<Chunk> chunkLoad = world.getChunkAtAsync(chunk.getX(), chunk.getZ());
        chunkLoad.thenAccept(c -> {
            CrystalChargeCarrier carrier = new CrystalChargeCarrier(world, new Location(world, finalLocationX, world.getHighestBlockYAt((int) finalLocationX, (int) finalLocationZ), finalLocationZ), region, alliance);
            carriers.add(carrier);
            Title title = Title.title(Component.empty(), Component.translatable("factions.war.carrier.spawn"));
            region.showTitle(title);
        });

    }

    private void handleCarrierDeposit(Player player) {
        region.getRegionalWarTracker().removeCrystalCarrier(player);
        removeCarryingPlayerBuffs(player);
        player.playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, Sound.Source.RECORD, 0.8f, 0.6f));
        player.playSound(Sound.sound(org.bukkit.Sound.ENTITY_PHANTOM_SWOOP, Sound.Source.RECORD, 0.8f, 1.0f));
        Title title = Title.title(Component.empty(), Component.translatable("factions.war.carrier.deposit"));
        player.showTitle(title);
        BukkitRunnable animation = new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                if (i++ > 40) {
                    addEnergy(energyGainPerCarrier);
                    cancel();
                    return;
                }
                crystal.getDataCrystal().setBeamTarget(CraftLocation.toBlockPosition(player.getLocation().add(0, 1, 0)));
            }
        };
        animation.runTaskTimer(plugin, 0, 1);
    }

    public static void removeCarryingPlayerBuffs(Player player) {
        player.setGlowing(false);
        player.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(CrystalChargeCarrier.CARRIER_BUFF);
        player.getAttribute(Attribute.ATTACK_DAMAGE).removeModifier(CrystalChargeCarrier.CARRIER_DEBUFF);
        player.getAttribute(Attribute.ADVANTAGE_PHYSICAL).removeModifier(CrystalChargeCarrier.CARRIER_DEBUFF);
        player.getAttribute(Attribute.ADVANTAGE_MAGICAL).removeModifier(CrystalChargeCarrier.CARRIER_DEBUFF);
        player.getPersistentDataContainer().remove(CrystalChargeCarrier.CARRIER_PLAYER_KEY);
        for (org.bukkit.entity.Entity entity : player.getPassengers()) {
            if (entity instanceof ItemDisplay display && display.getItemStack().getType() == Material.END_CRYSTAL) {
                entity.remove();
            }
        }
    }

    public static void addCarryingPlayerBuffs(Player player) {
        player.getPersistentDataContainer().set(CrystalChargeCarrier.CARRIER_PLAYER_KEY, PersistentDataType.BYTE, (byte) 1);
        player.setGlowing(true);
        player.getAttribute(Attribute.MOVEMENT_SPEED).addTransientModifier(CrystalChargeCarrier.CARRIER_BUFF);
        player.getAttribute(Attribute.ATTACK_DAMAGE).addTransientModifier(CrystalChargeCarrier.CARRIER_DEBUFF);
        player.getAttribute(Attribute.ADVANTAGE_PHYSICAL).addTransientModifier(CrystalChargeCarrier.CARRIER_DEBUFF);
        player.getAttribute(Attribute.ADVANTAGE_MAGICAL).addTransientModifier(CrystalChargeCarrier.CARRIER_DEBUFF);
        ItemDisplay itemDisplay = player.getWorld().spawn(player.getLocation().add(0, 1, 0), ItemDisplay.class, display -> {
            display.setPersistent(false);
            ItemStack itemStack = new ItemStack(Material.END_CRYSTAL);
            itemStack.editMeta(
                    meta -> meta.setEnchantmentGlintOverride(true)
            );
            display.setItemStack(itemStack);
            display.setInvulnerable(true);
        });
        player.addPassenger(itemDisplay);
    }

    /* Setup */

    @Override
    public void activate() {
        super.activate();
        World world = crystalLocation.getWorld();
        crystal = new CrystalMob(world, crystalLocation.getX(), crystalLocation.getY(), crystalLocation.getZ());
        crystal.getBukkitEntity().getPersistentDataContainer().set(NAME_KEY, PersistentDataType.STRING, name);
        crystal.getBukkitEntity().addPassenger(energyDisplay = world.spawn(crystalLocation, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.VERTICAL);
            display.setPersistent(false);
            displayEnergy(display);
        }));
    }

    @Override
    public void deactivate() {
        super.deactivate();
        energyDisplay.remove();
        crystal.remove(Entity.RemovalReason.DISCARDED);
        for (CrystalChargeCarrier carrier : carriers) {
            carrier.remove(Entity.RemovalReason.DISCARDED);
        }
    }

    @Override
    public void deleteStructure() {
        if (defenderCrystal) {
            return;
        }
        super.deleteStructure();
    }

    @Override
    public void onTemporaryOccupy(@NotNull Alliance alliance) {
        deactivate();
    }

    private void displayEnergy() {
        displayEnergy(energyDisplay);
    }

    private void displayEnergy(TextDisplay display) {
        displayPercentage(display, maxEnergy, energy);
    }

    private void displayPercentage(TextDisplay display, double max, double current) {
        if (display == null) {
            return;
        }
        int colored = (int) (20 * (1 / max * current));
        display.text(Component.text().color(NamedTextColor.BLUE).content("█".repeat(colored))
                .append(Component.text().color(NamedTextColor.GRAY).content("█".repeat(20 - colored))).build());
    }

    /* Serialization */

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> serialized = super.serialize();
        serialized.put("alliance", alliance);
        serialized.put("energyLossOnDamage", energyLossOnDamage);
        serialized.put("energyLossPerInterval", energyLossPerInterval);
        serialized.put("maxEnergy", maxEnergy);
        serialized.put("energyGainPerCarrier", energyGainPerCarrier);
        serialized.put("energyLossForCarrierSpawn", energyLossForCarrierSpawn);
        return serialized;
    }

    /* Getters and setters */

    public double getEnergyLossOnDamage() {
        return energyLossOnDamage;
    }

    public void setEnergyLossOnDamage(double energyLossOnDamage) {
        this.energyLossOnDamage = energyLossOnDamage;
    }

    public double getEnergyLossPerInterval() {
        return energyLossPerInterval;
    }

    public void setEnergyLossPerInterval(double energyLossPerInterval) {
        this.energyLossPerInterval = energyLossPerInterval;
    }

    public double getMaxEnergy() {
        return maxEnergy;
    }

    public void setMaxEnergy(double maxEnergy) {
        this.maxEnergy = maxEnergy;
        if (maxEnergy < energy) {
            setEnergy(maxEnergy);
        }
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        assert energy > 0 && energy <= maxEnergy : "The energy number must be greater than 0 and less or equal to " + maxEnergy;
        this.energy = energy;
        displayEnergy();
    }

    public void addEnergy(double amount) {
        this.energy = Math.min(energy + amount, maxEnergy);
        displayEnergy();
    }

    public void removeEnergy(double amount, @Nullable FPlayer causingPlayer) {
        energy -= amount;
        displayEnergy();
        if (energy <= 0) {
            destroy(causingPlayer);
        }
    }

    public @NotNull Alliance getAlliance() {
        return alliance;
    }

    public @NotNull CrystalWarStructure setAlliance(@NotNull Alliance alliance) {
        this.alliance = alliance;
        return this;
    }

    public @Nullable CrystalMob getCrystal() {
        return crystal;
    }

    public boolean isDefenderCrystal() {
        return defenderCrystal;
    }

    public void setDefenderCrystal(boolean defenderCrystal) {
        this.defenderCrystal = defenderCrystal;
    }
}
