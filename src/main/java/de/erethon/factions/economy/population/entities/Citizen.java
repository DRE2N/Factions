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
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.jetbrains.annotations.NotNull;

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
        setCustomName(Component.literal(randomName));
        VillagerData villagerData = switch (level) {
            case PEASANT -> getVillagerData().withType(level().registryAccess(), VillagerType.PLAINS).withProfession(level().registryAccess(), VillagerProfession.FARMER);
            case PATRICIAN -> getVillagerData().withType(level().registryAccess(), VillagerType.PLAINS).withProfession(level().registryAccess(), VillagerProfession.LIBRARIAN);
            case NOBLEMEN -> getVillagerData().withType(level().registryAccess(), VillagerType.PLAINS).withProfession(level().registryAccess(), VillagerProfession.CLERIC);
            default -> getVillagerData().withType(level().registryAccess(), VillagerType.PLAINS).withProfession(level().registryAccess(), VillagerProfession.NITWIT);
        };
        setVillagerData(villagerData);
        setPos(location.getX(), location.getY(), location.getZ());
        level().addFreshEntity(this);
    }

    @Override
    protected void customServerAiStep(@NotNull ServerLevel level) {
        if (lastGossipTick > 0) {
            lastGossipTick--;
            return;
        }
        if (lastGossipTick == 0) {
            lastGossipTick = 500;
            net.kyori.adventure.text.Component gossip = faction.getEconomy().getFittingCitizenGossip(populationLevel);
            net.kyori.adventure.text.Component name = net.kyori.adventure.text.Component.text(randomName, NamedTextColor.GREEN);
            for (org.bukkit.entity.Player player : getBukkitEntity().getLocation().getNearbyPlayers(4)) {
                player.sendMessage(name.append(net.kyori.adventure.text.Component.text(": ", NamedTextColor.DARK_GRAY)).append(gossip));
            }
        }
    }

    @Override
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        PopulationGUI gui = new PopulationGUI((org.bukkit.entity.Player) player.getBukkitEntity(), faction);
        gui.open();
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean wantsToSpawnGolem(long time) {
        return false;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        try { // Just in case
            nbt.putString("papyrus-entity-id", "factions_citizen");
        } catch (Exception e) {
            FLogger.WAR.log("Failed to save citizen NPC data at " + position().x + ", " + position().y + ", " + position().z);
            e.printStackTrace();
        }
    }

    private static String randomCitizenName() {
        String[] names = {"Bob", "Alice", "Charlie", "Dave", "Eve"};
        return names[(int) (Math.random() * names.length)];
    }
}
