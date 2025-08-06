package de.erethon.factions.economy.population.entities;

import com.google.common.base.Predicates;
import de.erethon.factions.Factions;
import de.erethon.factions.economy.gui.PopulationGUI;
import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.util.FLogger;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class Citizen extends Villager {

    private final Factions plugin = Factions.get();
    private Faction faction;
    private int lastGossipTick = 0;
    private PopulationLevel populationLevel;
    private String randomName;

    public Citizen(EntityType<? extends Villager> entityType, Level world) {
        super(entityType, world);
        targetSelector.removeAllGoals(Predicates.alwaysTrue());
        brain.removeAllBehaviors();
        brain.getActiveActivities().clear();
        goalSelector.removeAllGoals(Predicates.alwaysTrue());
        // Some basic behaviors so they seem alive
        goalSelector.addGoal(0, new RandomStrollGoal(this, 0.5D));
        goalSelector.addGoal(1, new RandomLookAroundGoal(this));
        goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        setPersistenceRequired(false); // We just respawn them
        persist = false;
    }

    public Citizen(Faction faction, Location location, PopulationLevel level) {
        super(EntityType.VILLAGER, ((CraftWorld) location.getWorld()).getHandle());
        randomName = randomCitizenName();
        this.faction = faction;
        this.populationLevel = level;
        setCustomName(Component.literal(randomName));
        VillagerData villagerData = switch (level) {
            case PEASANT -> getVillagerData().withType(level().registryAccess(), VillagerType.PLAINS).withProfession(level().registryAccess(), VillagerProfession.FARMER);
            case PATRICIAN -> getVillagerData().withType(level().registryAccess(), VillagerType.PLAINS).withProfession(level().registryAccess(), VillagerProfession.LIBRARIAN);
            case NOBLEMEN -> getVillagerData().withType(level().registryAccess(), VillagerType.PLAINS).withProfession(level().registryAccess(), VillagerProfession.CLERIC);
            default -> getVillagerData().withType(level().registryAccess(), VillagerType.PLAINS).withProfession(level().registryAccess(), VillagerProfession.NITWIT);
        };
        setVillagerData(villagerData);
        setPos(location.getX(), location.getY(), location.getZ());
        targetSelector.removeAllGoals(Predicates.alwaysTrue());
        brain.removeAllBehaviors();
        brain.getActiveActivities().clear();
        goalSelector.removeAllGoals(Predicates.alwaysTrue());
        // Some basic behaviors so they seem alive
        goalSelector.addGoal(0, new RandomStrollGoal(this, 0.5D));
        goalSelector.addGoal(1, new RandomLookAroundGoal(this));
        goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        setPersistenceRequired(false); // We just respawn them
        persist = false;
        level().addFreshEntity(this);
    }

    @Override
    protected void customServerAiStep(@NotNull ServerLevel level) {
        if (faction == null) {
            remove(RemovalReason.DISCARDED);
            return;
        }
        if (lastGossipTick > 0) {
            lastGossipTick--;
            return;
        }
        if (lastGossipTick == 0) {
            lastGossipTick = 500;
            gossip();
        }
    }

    @Override
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        PopulationGUI gui = new PopulationGUI((org.bukkit.entity.Player) player.getBukkitEntity(), faction);
        gui.open();
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean wantsToSpawnGolem(long time) {
        return false;
    }

    private void gossip() {
        net.kyori.adventure.text.Component gossip = faction.getEconomy().getFittingCitizenGossip(populationLevel);
        if (gossip == null || gossip.equals(Component.empty())) {
            return; // No gossip to send
        }
        net.kyori.adventure.text.Component name = net.kyori.adventure.text.Component.text(randomName, NamedTextColor.GREEN);
        for (org.bukkit.entity.Player player : getBukkitEntity().getLocation().getNearbyPlayers(3)) {
            player.sendMessage(name.append(net.kyori.adventure.text.Component.text(": ", NamedTextColor.DARK_GRAY)).append(gossip));
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);
        try { // Just in case
            output.putString("papyrus-entity-id", "factions_citizen");
            output.putInt("faction-id", faction.getId());
            output.putString("population-level", populationLevel.name());
        } catch (Exception e) {
            FLogger.ECONOMY.log("Failed to save citizen NPC data at " + position().x + ", " + position().y + ", " + position().z);
            e.printStackTrace();
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        Optional<Integer> id = input.getInt("papyrus-entity-id");
        id.ifPresent(integer -> faction = plugin.getFactionCache().getById(integer));
        Optional<String> level = input.getString("population-level");
        level.ifPresent(s -> populationLevel = PopulationLevel.valueOf(s.toUpperCase()));
        if (faction == null) {
            FLogger.ECONOMY.log("Failed to load citizen NPC data at " + position().x + ", " + position().y + ", " + position().z + ". Faction not found for ID: " + input.getInt("faction-id"));
            remove(RemovalReason.DISCARDED);
            return;
        }
    }

    private static String randomCitizenName() {
        String[] names = {"Bob", "Alice", "Charlie", "Dave", "Eve"};
        return names[(int) (Math.random() * names.length)];
    }
}
