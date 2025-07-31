package de.erethon.factions.war.entities;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.util.FUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FollowMobGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class LoggedOutPlayer extends Vindicator {

    private ServerPlayer dataPlayer;
    private UUID uuid;
    private String name;
    private org.bukkit.inventory.@Nullable ItemStack @NotNull [] items;
    private Player loggedOutPlayer;

    public LoggedOutPlayer(EntityType<? extends Vindicator> type, Level world) {
        super(type, world);
        createPlayerStuff((ServerLevel) world);
    }

    public LoggedOutPlayer(ServerLevel level, Player player) {
        this(EntityType.VINDICATOR, level);
        setPos(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(player.getHealth());
        setHealth(getMaxHealth());
        createPlayerStuff(level);
        loggedOutPlayer = player;
        setSilent(true); // Vindicators are loud and annoying
        if (dataPlayer == null) {
            FLogger.WAR.log("Failed to create a fake player for a logged out player");
            return;
        }
        storeItems(player);
        setPersistenceRequired(false); // If the chunk unloads, it's okay if the player despawns because apparently no other player is around them
    }

    private void createPlayerStuff(ServerLevel level) {
        CraftPlayerProfile craftPlayerProfile = new CraftPlayerProfile(UUID.randomUUID(), name);
        this.dataPlayer = new ServerPlayer(MinecraftServer.getServer(), level, craftPlayerProfile.buildGameProfile(), new ClientInformation("en", 0, ChatVisiblity.SYSTEM, false, 1, HumanoidArm.RIGHT, false, false, ParticleStatus.ALL));
    }

    private void storeItems(Player player) {
        items = player.getInventory().getContents();
    }

    private void restoreItems() {
        if (items == null) {
            return;
        }
        int index = 0;
        for (ItemStack item : items) {
            if (item != null) {
                loggedOutPlayer.getInventory().setItem(index, item);
            }
            items[index] = new ItemStack(Material.AIR);
            index++;
        }
    }

    @Override
    protected void registerGoals() {
        // None
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        int index = 0;
        for (ItemStack item : items) {
            if (item != null) {
                ItemEntity itemEntity = new ItemEntity(level(), getX(), getY(), getZ(), CraftItemStack.unwrap(item));
                level().addFreshEntity(itemEntity);
            }
            items[index] = new ItemStack(Material.AIR);
            index++;
        }
    }

   /* @EventHandler TODO
    private void onCharSelected(//) {
        if (event.getPlayer().getUniqueId().equals(uuid)) {
            remove(RemovalReason.DISCARDED);
        }
    }*/

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity entity) {
        level().players().forEach(player -> {
            if (player instanceof ServerPlayer) {
                ClientboundPlayerInfoUpdatePacket infoUpdatePacket = new ClientboundPlayerInfoUpdatePacket(
                        EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                        new ClientboundPlayerInfoUpdatePacket.Entry(dataPlayer.getUUID(), dataPlayer.getGameProfile(), false, 0, GameType.SURVIVAL, Component.empty(), true, 1, null));
                ((ServerPlayer) player).connection.send(infoUpdatePacket);
            }
        });
        return FUtil.getAddEntityPacketWithType(this, EntityType.PLAYER);
    }

    @Override
    public @NotNull SynchedEntityData getEntityData() {
        if (dataPlayer == null) {
            Factions.log("Tried to get entity data for a logged out player without a fake player");
            return super.getEntityData();
        }
        return dataPlayer.getEntityData();
    }

}
