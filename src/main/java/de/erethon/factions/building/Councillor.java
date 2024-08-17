package de.erethon.factions.building;

import com.google.common.base.Predicates;
import de.erethon.factions.Factions;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.util.FLogger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.jetbrains.annotations.NotNull;

public class Councillor extends Villager {

    private final Factions plugin = Factions.get();
    private Faction faction;

    public Councillor(EntityType<? extends Villager> entityType, Level world) {
        super(entityType, world);
        targetSelector.removeAllGoals(Predicates.alwaysTrue());
        goalSelector.removeAllGoals(Predicates.alwaysTrue());
        brain.removeAllBehaviors();
        brain.getActiveActivities().clear();
    }

    public Councillor(Faction faction, Location location) {
        super(EntityType.VILLAGER, ((CraftWorld) location.getWorld()).getHandle());
        setCustomName(Component.translatable("factions.building.councillor.name"));
        setCustomNameVisible(true);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        // Open GUI here
        return InteractionResult.CONSUME;
    }

    @Override
    protected void customServerAiStep(boolean inactive) {
        // Do nothing
    }

    @Override
    public boolean wantsToSpawnGolem(long time) {
        return false;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        faction = plugin.getFactionCache().getById(nbt.getInt("factions-region-id"));
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        try { // Just in case the Factions side of things is broken.
            nbt.putString("papyrus-entity-id", "factions_councillor");
            nbt.putInt("factions-region-id", faction.getId());
        } catch (Exception e) {
            FLogger.WAR.log("Failed to save councillor NPC data at " + position().x + ", " + position().y + ", " + position().z);
            e.printStackTrace();
        }
    }
}
