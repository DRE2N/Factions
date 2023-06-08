package de.erethon.factions.player;

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
import de.erethon.factions.region.Region;
import de.erethon.factions.ui.UIFactionsListener;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.util.FPermissionUtil;
import de.erethon.factions.war.WarStats;
import de.erethon.factions.war.objective.WarObjective;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author Fyreum
 */
public class FPlayer extends EConfig implements FEntity, LoadableUser, PlayerWrapper {

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
    /* Functionality */
    private Region lastRegion;
    private final AutomatedChunkManager automatedChunkManager = new AutomatedChunkManager(this);
    private final Set<WarObjective> activeWarObjectives = new HashSet<>();

    public FPlayer(@NotNull UUID uuid) {
        super(Factions.getPlayerFile(uuid), CONFIG_VERSION);
        this.uuid = uuid;
        this.player = Bukkit.getPlayer(uuid);
        load();
    }

    public FPlayer(@NotNull Player player) {
        super(Factions.getPlayerFile(player.getUniqueId()), CONFIG_VERSION);
        this.player = player;
        this.uuid = player.getUniqueId();
        this.lastName = player.getName();
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
    }

    /* LoadableUser methods */

    @Override
    public void onJoin(PlayerJoinEvent event) {
        lastName = event.getPlayer().getName();
    }

    @Override
    public void onQuit(PlayerQuitEvent event) {
        bypass = isBypassRaw();
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
        return true;
    }

    public @NotNull Component getAllianceTag() {
        return alliance == null ? FMessage.GENERAL_NONE.message() : Component.text(alliance.getDisplayShortName()).color(alliance.getColor());
    }

    @Override
    public @Nullable Faction getFaction() {
        return faction;
    }

    public void setFaction(@Nullable Faction faction) {
        this.faction = faction;
    }

    public @NotNull Component getFactionTag() {
        return (faction == null ? FMessage.GENERAL_LONER.message() : Component.text(faction.getDisplayShortName()))
                .color(alliance == null ? null : alliance.getColor());
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
        return lastRegion == null ? FMessage.GENERAL_WILDERNESS.getMessage() : lastRegion.getName();
    }

    public @NotNull AutomatedChunkManager getAutomatedChunkManager() {
        return automatedChunkManager;
    }

    public @NotNull Set<WarObjective> getActiveWarObjectives() {
        return activeWarObjectives;
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

    /* Object methods */

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FPlayer fPlayer)) {
            return false;
        }
        return uuid.equals(fPlayer.getUniqueId());
    }
}
