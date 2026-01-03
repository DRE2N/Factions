package de.erethon.factions.player;

import de.erethon.aergia.command.logic.ESender;
import de.erethon.aergia.ui.UIActionBar;
import de.erethon.aergia.ui.UIBossBar;
import de.erethon.aergia.ui.UIComponent;
import de.erethon.aergia.ui.UIUpdater;
import de.erethon.aergia.util.AergiaUtil;
import de.erethon.aergia.util.DynamicComponent;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.config.EConfig;
import de.erethon.bedrock.player.PlayerWrapper;
import de.erethon.bedrock.user.LoadableUser;
import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.entity.FEntity;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.poll.Poll;
import de.erethon.factions.region.AutomatedChunkManager;
import de.erethon.factions.region.LazyChunk;
import de.erethon.factions.region.Region;
import de.erethon.factions.integration.UIFactionsListener;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.util.FPermissionUtil;
import de.erethon.factions.war.WarStats;
import de.erethon.factions.war.structure.WarStructure;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author Fyreum
 */
public class FPlayer extends EConfig implements FEntity, LoadableUser, PlayerWrapper, ESender {

    public static final int CONFIG_VERSION = 1;

    final Factions plugin = Factions.get();

    private final UUID uuid;
    private Player player;
    /* Data */
    private Alliance alliance;
    private Faction faction;
    private String lastName = "";
    private String title = "";
    private boolean bypass = false;
    private long lastAllianceJoinDate;
    private long lastFactionJoinDate;
    private WarStats warStats;
    private List<String> seenHints = new ArrayList<>();
    /* Functionality */
    private final Set<WarStructure> activeWarStructures = new HashSet<>();
    private final AutomatedChunkManager automatedChunkManager = new AutomatedChunkManager(this);
    private Region lastRegion;
    private Location pos1, pos2;
    private PermissionAttachment permissionAttachment;

    public FPlayer(@NotNull UUID uuid) {
        super(Factions.getPlayerFile(uuid), CONFIG_VERSION);
        this.uuid = uuid;
        this.player = Bukkit.getPlayer(uuid);
        if (player != null && plugin.getRegionManager().getRegionByPlayer(player) != null) {
            this.lastRegion = plugin.getRegionManager().getRegionByPlayer(player);
        }
        load();
    }

    public FPlayer(@NotNull Player player) {
        super(Factions.getPlayerFile(player.getUniqueId()), CONFIG_VERSION);
        this.player = player;
        this.uuid = player.getUniqueId();
        this.lastName = player.getName();
        if (plugin.getRegionManager().getRegionByPlayer(player) != null) {
            this.lastRegion = plugin.getRegionManager().getRegionByPlayer(player);
        }
        load();
    }

    /* EConfig methods */

    @Override
    public void load() {
        alliance = plugin.getAllianceCache().getById(config.getInt("alliance", -1));
        faction = plugin.getFactionCache().getById(config.getInt("faction", -1));
        lastName = config.getString("lastName", lastName);
        title = config.getString("title", title);
        bypass = config.getBoolean("bypass", bypass);
        lastAllianceJoinDate = config.getLong("lastAllianceJoinDate", lastAllianceJoinDate);
        lastFactionJoinDate = config.getLong("lastFactionJoinDate", lastFactionJoinDate);
        warStats = new WarStats(config.getConfigurationSection("warStats"));
        if (config.contains("seenHints")) {
            seenHints = config.getStringList("seenHints");
        } else {
            seenHints = new ArrayList<>();
        }
    }

    /* LoadableUser methods */

    @Override
    public void onJoin(PlayerJoinEvent event) {
        lastName = event.getPlayer().getName();
        updateDisplayNames();
        if (hasFaction()) {
            PermissionAttachment attachment = player.addAttachment(plugin);
            for (String perm : faction.getAdditionalMemberPermissions()) {
                attachment.setPermission(perm, true);
            }
        }
    }

    @Override
    public void onQuit(PlayerQuitEvent event) {
        bypass = isBypassRaw();
        if (hasFaction()) {
            if (permissionAttachment != null) {
                player.removeAttachment(permissionAttachment);
            }
        }
    }

    @Override
    public void updatePlayer(Player player) {
        this.player = player;
    }

    @Override
    public void saveUser() {
        FLogger.PLAYER.log("Saving player...");
        config.set("alliance", alliance == null ? null : alliance.getId());
        config.set("faction", faction == null ? null : faction.getId());
        config.set("lastName", lastName);
        config.set("title", title);
        config.set("bypass", bypass);
        config.set("lastAllianceJoinDate", lastAllianceJoinDate);
        config.set("lastFactionJoinDate", lastFactionJoinDate);
        config.set("warStats", warStats.serialize());
        config.set("seenHints", seenHints);
        save();
    }

    /* Message */

    public void sendMessage(@NotNull String msg) {
        if (isOnline()) {
            player.sendMessage(MessageUtil.parse(msg));
        }
    }

    public void sendMessage(@NotNull Component msg) {
        if (isOnline()) {
            player.sendMessage(msg);
        }
    }

    @Override
    public @NotNull Component asComponent(@NotNull FEntity viewer) {
        Component component = Component.text(getDisplayName());
        Component hoverMessage = getAllianceTag();
        hoverMessage = hoverMessage.appendNewline().append(Component.text(getDisplayMembership(), getRelation(viewer).getColor()));
        // TODO: Aergia RP char name
        hoverMessage = hoverMessage.appendNewline().append(Component.translatable("factions.general.clickHints.player"));
        component = component.hoverEvent(HoverEvent.showText(hoverMessage));
        return component;
    }

    @Override
    public @NotNull Iterable<? extends Audience> audiences() {
        return Collections.singleton(player);
    }

    /* Aergia */

    public void sendActionBarMessage(@NotNull Component msg) {
        if (isOnline()) {
            UIComponent infoComponent = getUIActionBar().getCenter().getById(UIFactionsListener.FACTIONS_INFO_ID);
            if (infoComponent == null) {
                return;
            }
            infoComponent.setComponent(p -> msg);
            infoComponent.resetDuration();
        }
    }

    public void updateOrSendActionBarCenter(@NotNull String id, @NotNull DynamicComponent component, long ticks) {
        UIComponent sent = getUIActionBar().getCenter().getById(id);
        if (sent != null) {
            sent.setComponent(component);
        } else {
            sent = UIComponent.reactivatable(component, ticks, id);
            getUIActionBar().getCenter().add(sent);
        }
        sent.setRemainingDuration(ticks);
    }

    public UIUpdater getUIUpdater() {
        return AergiaUtil.getUIUpdater(player);
    }

    public UIActionBar getUIActionBar() {
        return getUIUpdater().getActionBar();
    }

    public UIBossBar getUIBossBar() {
        return getUIUpdater().getBossBar();
    }

    @Override
    public CommandSender getCommandSender() {
        return player;
    }

    @Override
    public boolean isPlayer() {
        return true;
    }

    /* Permission stuff */

    public boolean isAdmin() {
        return hasFaction() && isAdminRaw();
    }

    public boolean isAdminRaw() {
        return faction.isAdmin(this);
    }

    public boolean isMod() {
        return hasFaction() && isModRaw();
    }

    public boolean isModRaw() {
        return faction.isMod(this);
    }

    public boolean isBypass() {
        return isOnline() ? isBypassRaw() : bypass;
    }

    public boolean isBypassRaw() {
        return FPermissionUtil.isBypass(player);
    }

    /* Getters */

    public @NotNull UUID getUniqueId() {
        return uuid;
    }

    public @Nullable Player getPlayer() {
        return player;
    }

    public @NotNull OfflinePlayer getOfflinePlayer() {
        return Bukkit.getOfflinePlayer(uuid);
    }

    public boolean isOnline() {
        return player != null;
    }

    @Override
    public @Nullable Alliance getAlliance() {
        return alliance;
    }

    public boolean setAlliance(@Nullable Alliance alliance) {
        if (alliance != this.alliance && faction != null) {
            return false;
        }
        this.alliance = alliance;
        updateDisplayNames();
        return true;
    }

    public @NotNull Component getAllianceTag() {
        return alliance == null ?
                Component.text().color(NamedTextColor.GRAY).content(FMessage.GENERAL_NONE.getMessage()).build() :
                alliance.getColoredName();
    }

    public @NotNull TextColor getAllianceColor() {
        return alliance == null ? NamedTextColor.GRAY : alliance.getColor();
    }

    @Override
    public @Nullable Faction getFaction() {
        return faction;
    }

    public void setFaction(@Nullable Faction faction) {
        this.faction = faction;
        updateDisplayNames();
    }

    public @NotNull Component getFactionTag() {
        return Component.text()
                .color(alliance == null ? NamedTextColor.GRAY : alliance.getColor())
                .content(faction == null ? FMessage.GENERAL_LONER.getMessage() : faction.getName())
                .build();
    }

    @Override
    public String getName() {
        return lastName;
    }

    public @NotNull String getLastName() {
        return lastName;
    }

    public @NotNull String getDisplayName() {
        return hasFaction() ? (isAdminRaw() ? "**" : isModRaw() ? "*" : "") + lastName : lastName;
    }

    public void updateDisplayNames() {
        if (!isOnline()) {
            return;
        }
        Component displayName = Component.text()
                .content("[" + getDisplayMembership() + "] ")
                .color(hasAlliance() ? alliance.getColor() : NamedTextColor.GRAY)
                .append(Component.text(lastName).color(NamedTextColor.GRAY))
                .build();
        player.displayName(displayName);
        player.playerListName(displayName);
    }

    public void setLastName(@NotNull String lastName) {
        this.lastName = lastName;
    }

    public @NotNull String getTitle() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title == null ? "" : title;
    }

    public long getLastAllianceJoinDate() {
        return lastAllianceJoinDate;
    }

    public void setLastAllianceJoinDate(long lastAllianceJoinDate) {
        this.lastAllianceJoinDate = lastAllianceJoinDate;
    }

    public long getLastFactionJoinDate() {
        return lastFactionJoinDate;
    }

    public void setLastFactionJoinDate(long lastFactionJoinDate) {
        this.lastFactionJoinDate = lastFactionJoinDate;
    }

    public @NotNull WarStats getWarStats() {
        return warStats;
    }

    public @NotNull Set<WarStructure> getActiveWarObjectives() {
        return activeWarStructures;
    }

    public boolean hasActiveWarObjectives() {
        return !activeWarStructures.isEmpty();
    }

    public @NotNull AutomatedChunkManager getAutomatedChunkManager() {
        return automatedChunkManager;
    }

    public @Nullable Region getLastRegion() {
        return lastRegion;
    }

    public void setLastRegion(@Nullable Region lastRegion) {
        this.lastRegion = lastRegion;
    }

    /**
     * Returns the region the player is currently located in, or null.
     *
     * @return the region the player is currently located in, or null
     * @throws NullPointerException if the player is not online
     */
    public @Nullable Region getCurrentRegion() {
        return plugin.getRegionManager().getRegionByPlayer(player);
    }

    public @NotNull String getDisplayRegion() {
        return lastRegion == null ? FMessage.GENERAL_WILDERNESS.getMessage() : lastRegion.getName(true);
    }

    public @NotNull String getDisplayRegionWithOwner() {
        return lastRegion == null ? FMessage.GENERAL_WILDERNESS.getMessage() :
                lastRegion.hasFaction() ? lastRegion.getName(true) + " (" + lastRegion.getFaction().getName(true) + ")" : lastRegion.getName(true);
    }

    public @NotNull LazyChunk getCurrentChunk() {
        return new LazyChunk(player.getLocation().getChunk());
    }

    public @NotNull Map<String, Poll<?>> getParticipativePolls() {
        Map<String, Poll<?>> polls = new HashMap<>();
        if (alliance != null) {
            polls.putAll(alliance.getPollsFor(this));
        }
        if (faction != null) {
            polls.putAll(faction.getPollsFor(this));
        }
        return polls;
    }

    public @Nullable Location getPos1() {
        return pos1;
    }

    public void setPos1(@Nullable Location pos1) {
        this.pos1 = pos1;
    }

    public @Nullable Location getPos2() {
        return pos2;
    }

    public void setPos2(@Nullable Location pos2) {
        this.pos2 = pos2;
    }

    public boolean hasSelection() {
        return pos1 != null && pos2 != null;
    }

    public @NotNull PermissionAttachment getPermissionAttachment() {
        if (permissionAttachment == null) {
            permissionAttachment = player.addAttachment(plugin);
        }
        return permissionAttachment;
    }

    public void removePermissionAttachment() {
        if (permissionAttachment != null) {
            player.removeAttachment(permissionAttachment);
            permissionAttachment = null;
        }
    }

    public boolean hasSeenHint(@NotNull String hint) {
        return seenHints.contains(hint);
    }

    public void addSeenHint(@NotNull String hint) {
        if (!seenHints.contains(hint)) {
            seenHints.add(hint);
        }
    }

    /* Object methods */

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FPlayer other)) return false;
        return uuid.equals(other.getUniqueId());
    }
}
