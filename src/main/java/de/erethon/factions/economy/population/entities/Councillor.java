package de.erethon.factions.economy.population.entities;

import com.google.common.base.Predicates;
import de.erethon.factions.Factions;
import de.erethon.factions.economy.gui.EconomyGUI;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.util.FLogger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

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
        setCustomName(Component.translatable("factions.economy.councillor.name"));
        setCustomNameVisible(true);
        setPos(location.getX(), location.getY(), location.getZ());
        level().addFreshEntity(this);
        setPersistenceRequired(false);
        persist = false;
        this.faction = faction;
    }

    @Override
    protected void customServerAiStep(@NotNull ServerLevel level) {
        return; // Do nothing
    }

    @Override
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        EconomyGUI gui = new EconomyGUI((org.bukkit.entity.Player) player.getBukkitEntity(), faction);
        gui.open();
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean wantsToSpawnGolem(long time) {
        return false;
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        Optional<Integer> factionId = input.getInt("factions-faction-id");
        factionId.ifPresent(integer -> faction = plugin.getFactionCache().getById(integer));
    }

    @Override
    public void addAdditionalSaveData(@NotNull ValueOutput output) {
        super.addAdditionalSaveData(output);
        try { // Just in case the Factions side of things is broken.
            output.putString("papyrus-entity-id", "factions_councillor");
            output.putInt("factions-faction-id", faction.getId());
        } catch (Exception e) {
            FLogger.WAR.log("Failed to save councillor NPC data at " + position().x + ", " + position().y + ", " + position().z);
            persist = false;
            remove(RemovalReason.DISCARDED);
            e.printStackTrace();
        }
    }
}
