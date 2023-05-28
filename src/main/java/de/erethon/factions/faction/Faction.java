package de.erethon.factions.faction;

import de.erethon.aergia.util.BroadcastUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.Building;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.FSetTag;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.economy.FAccount;
import de.erethon.factions.economy.FAccountDummy;
import de.erethon.factions.economy.FAccountImpl;
import de.erethon.factions.economy.FStorage;
import de.erethon.factions.economy.FactionLevel;
import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.entity.FLegalEntity;
import de.erethon.factions.event.FPlayerFactionLeaveEvent;
import de.erethon.factions.event.FactionDisbandEvent;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FException;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.util.FPermissionUtil;
import de.erethon.factions.util.FactionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author Fyreum
 */
public class Faction extends FLegalEntity {

    /* Persistent */
    private Alliance alliance;
    private FPlayer admin;
    private Set<FPlayer> mods;
    private Set<FPlayer> members;
    private final Set<Region> regions = new HashSet<>();
    private Region coreRegion;
    private String shortName;
    private boolean open = false;
    private final Set<Faction> authorisedBuilders = new HashSet<>();
    private final Set<Faction> adjacentFactions = new HashSet<>();
    /* Temporary */
    private final Set<FPlayer> invitedPlayers = new HashSet<>();
    private FAccount fAccount;
    private FStorage fStorage;
    private FactionLevel level = FactionLevel.HAMLET;

    private Set<BuildSite> buildSites = new HashSet<>();
    private Set<BuildingEffect> buildingEffects = new HashSet<>();
    private HashMap<PopulationLevel, Integer> population = new HashMap<>();

    protected Faction(@NotNull FPlayer admin, @NotNull Region coreRegion, int id, String name, String description) {
        super(new File(Factions.FACTIONS, id + ".yml"), id, name, description);
        this.admin = admin;
        this.mods = new HashSet<>();
        this.members = new HashSet<>();
        this.members.add(admin);
        this.regions.add(coreRegion);
        this.coreRegion = coreRegion;
        this.coreRegion.setOwner(this);
        this.fAccount = plugin.hasEconomyProvider() ? new FAccountImpl(this) : FAccountDummy.INSTANCE;
        saveData();
    }

    protected Faction(@NotNull File file) throws NumberFormatException {
        super(file);
    }

    public void invitePlayer(@NotNull FPlayer fPlayer) {
        invitedPlayers.add(fPlayer);
        Component message = FMessage.FACTION_INVITE_MESSAGE.message(name);
        Component accept = FMessage.FACTION_INVITE_ACCEPT.message()
                .clickEvent(ClickEvent.runCommand("/f join " + id))
                .hoverEvent(HoverEvent.showText(FMessage.FACTION_INVITE_ACCEPT_HOVER.message()));
        Component decline = FMessage.FACTION_INVITE_DECLINE.message()
                .clickEvent(ClickEvent.runCommand("/f decline " + id))
                .hoverEvent(HoverEvent.showText(FMessage.FACTION_INVITE_DECLINE_HOVER.message()));
        fPlayer.sendMessage(message);
        fPlayer.sendMessage(accept.append(Component.space()).append(decline));
    }

    public void playerJoin(@NotNull FPlayer fPlayer) {
        FException.throwIf(isMember(fPlayer), "The player '" + fPlayer.getLastName() + "' is already a member of the faction '" + name + "'",
                FMessage.ERROR_TARGET_IS_ALREADY_IN_THIS_FACTION, fPlayer.getLastName(), name);
        FLogger.FACTION.log("Processing '" + fPlayer.getUniqueId() + "' joining faction '" + name + "'...");
        invitedPlayers.remove(fPlayer);
        members.add(fPlayer);
        fPlayer.setLastFactionJoinDate(System.currentTimeMillis());
        sendMessage(FMessage.FACTION_INFO_PLAYER_JOINED.message(fPlayer.getLastName()));
    }

    public void playerLeave(@NotNull FPlayer fPlayer, @NotNull FPlayerFactionLeaveEvent.Reason reason) {
        FException.throwIf(!isMember(fPlayer), "The player '" + fPlayer.getLastName() + "' is no member of the faction '" + name + "'",
                FMessage.ERROR_TARGET_IS_NOT_IN_THIS_FACTION, fPlayer.getLastName(), name);
        FLogger.FACTION.log("Processing '" + fPlayer.getUniqueId() + "' leaving faction '" + name + "' (reason: " + reason + ")...");
        members.remove(fPlayer);
        mods.remove(fPlayer);
        fPlayer.setFaction(null);
        FPlayerFactionLeaveEvent event = new FPlayerFactionLeaveEvent(this, fPlayer, reason);
        event.callEvent();

        if (isAdmin(fPlayer)) {
            admin = null;
            if (members.isEmpty()) {
                disband(FactionDisbandEvent.Reason.NO_MEMBERS_LEFT);
                return;
            }
            Set<FPlayer> possibleSuccessors = mods.isEmpty() ? members : mods;
            long selected = Long.MAX_VALUE;

            for (FPlayer member : possibleSuccessors) {
                if (member.getLastFactionJoinDate() < selected) {
                    admin = member;
                    selected = member.getLastFactionJoinDate();
                }
            }
        }
        sendMessage(event.getMessage());
    }

    public void disband(@NotNull FactionDisbandEvent.Reason reason) {
        FLogger.FACTION.log("Disbanding faction '" + name + "'...");
        new FactionDisbandEvent(this, reason).callEvent();
        plugin.getFactionCache().getCache().remove(id);
        if (alliance != null) {
            alliance.removeFaction(this);
        }
        for (FPlayer member : members) {
            member.setFaction(null);
        }
        mods.clear();
        for (Region region : regions) {
            region.setOwner(null);
        }
        BroadcastUtil.broadcast(FMessage.FACTION_INFO_DISBANDED.message());
    }

    /* Messages */

    public void sendMessage(@NotNull Component msg) {
        sendMessage(msg, true);
    }

    public void sendMessage(@NotNull Component msg, boolean prefix) {
        Component message = prefix ? FMessage.FACTION_INFO_PREFIX.message().append(msg) : msg;
        for (FPlayer member : members) {
            member.sendMessage(message);
        }
    }

    /* Serialization */

    @Override
    public void load() {
        FLogger.FACTION.log("Loading faction '" + id + "'...");
        this.alliance = plugin.getAllianceCache().getById(config.getInt("alliance", -1));
        String adminId = config.getString("admin", "null");
        try {
            this.admin = plugin.getFPlayerCache().getByUniqueId(UUID.fromString(adminId));
        } catch (IllegalArgumentException e) {
            FLogger.ERROR.log("Illegal UUID in faction '" + file.getName() + "' found: '" + adminId + "'");
        }
        this.mods = getFPlayers("mods");
        this.members = getFPlayers("members");
        for (int regionId : config.getIntegerList("regions")) {
            Region region = plugin.getRegionManager().getRegionById(regionId);
            if (region == null) {
                FLogger.ERROR.log("Unknown region ID in faction '" + id + "' found: " + regionId);
                continue;
            }
            this.regions.add(region);
        }
        int coreRegionId = config.getInt("coreRegion");
        this.coreRegion = plugin.getRegionManager().getRegionById(coreRegionId);
        if (coreRegion == null) {
            FLogger.ERROR.log("Unknown core region ID in faction '" + id + "' found: " + coreRegionId);
        }
        this.shortName = config.getString("shortName");
        this.open = config.getBoolean("open", open);
        for (int factionId : config.getIntegerList("authorisedBuilders")) {
            Faction faction = plugin.getFactionCache().getById(factionId);
            if (faction == null) {
                FLogger.ERROR.log("Unknown faction ID in faction '" + id + "' found: " + factionId);
                continue;
            }
            this.authorisedBuilders.add(faction);
        }
        for (int factionId : config.getIntegerList("adjacentFactions")) {
            Faction faction = plugin.getFactionCache().getById(factionId);
            if (faction == null) {
                FLogger.ERROR.log("Unknown faction ID in faction '" + id + "' found: " + factionId);
                continue;
            }
            this.adjacentFactions.add(faction);
        }
        this.fAccount = plugin.hasEconomyProvider() ? new FAccountImpl(this) : FAccountDummy.INSTANCE;
        for (PopulationLevel level : PopulationLevel.values()) {
            this.population.put(level, config.getInt("population." + level.name(), 0));
        }
        for (String key : config.getConfigurationSection("buildSites").getKeys(false)) {
            buildSites.add(new BuildSite(config.getConfigurationSection("buildSites." + key)));
        }
        fStorage = new FStorage(this, config.getConfigurationSection("storage"));
    }

    private Set<FPlayer> getFPlayers(String path) {
        Set<FPlayer> players = new HashSet<>();
        for (String string : config.getStringList(path)) {
            try {
                UUID uuid = UUID.fromString(string);
                FPlayer fPlayer = plugin.getFPlayerCache().getByUniqueId(uuid);
                if (fPlayer == null) {
                    continue;
                }
                players.add(fPlayer);
            } catch (IllegalArgumentException e) {
                FLogger.ERROR.log("Illegal UUID in faction '" + file.getName() + "' found: '" + string + "'");
            }
        }
        return players;
    }

    @Override
    protected void serializeData() {
        config.set("alliance", alliance == null ? null : alliance.getId());
        config.set("admin", admin.getUniqueId().toString());
        config.set("mods", mods.stream().map(p -> p.getUniqueId().toString()).toList());
        config.set("members", members.stream().map(p -> p.getUniqueId().toString()).toList());
        saveEntities("regions", regions);
        config.set("coreRegion", coreRegion == null ? null : coreRegion.getId());
        config.set("shortName", shortName);
        config.set("open", open);
        config.set("level", level);
        for (PopulationLevel level : PopulationLevel.values()) {
            config.set("population." + level.name(), population.get(level));
        }
        config.set("buildSites", buildSites);
        config.set("storage", fStorage.save());
        saveEntities("authorisedBuilders", authorisedBuilders);
        saveEntities("adjacentFactions", adjacentFactions);
    }

    public boolean isPrivileged(@NotNull FPlayer fPlayer) {
        return isAdmin(fPlayer) || isMod(fPlayer) || fPlayer.isBypass();
    }

    public boolean isPrivileged(@NotNull CommandSender sender) {
        return sender instanceof Player player ? isPrivileged(plugin.getFPlayerCache().getByPlayer(player)) : FPermissionUtil.isBypass(sender);
    }

    public boolean hasPrivilegeOver(@NotNull FPlayer fPlayer, @NotNull FPlayer target) {
        if (fPlayer.isBypass()) {
            return true;
        }
        return !isAdmin(target) && (isAdmin(fPlayer) || (isMod(fPlayer) && !isMod(target)));
    }

    public boolean hasPrivilegeOver(@NotNull CommandSender sender, @NotNull FPlayer target) {
        return sender instanceof Player player ? hasPrivilegeOver(plugin.getFPlayerCache().getByPlayer(player), target) : FPermissionUtil.isBypass(sender);
    }

    /* Getters and setters */

    @Override
    public @Nullable Alliance getAlliance() {
        return alliance;
    }

    public void setAlliance(@Nullable Alliance alliance) {
        this.alliance = alliance;
    }

    @Override
    public @Nullable Faction getFaction() {
        return this;
    }

    public @NotNull FPlayer getAdmin() {
        return admin;
    }

    public boolean isAdmin(@NotNull FPlayer fPlayer) {
        return admin == fPlayer;
    }

    public void setAdmin(@NotNull FPlayer fPlayer) {
        this.admin = fPlayer;
    }

    public @NotNull Set<FPlayer> getMods() {
        return mods;
    }

    public boolean isMod(FPlayer fPlayer) {
        return mods.contains(fPlayer);
    }

    public void addMod(@NotNull FPlayer fPlayer) {
        mods.add(fPlayer);
    }

    public @NotNull Set<FPlayer> getMembers() {
        return members;
    }

    public boolean isMember(@NotNull FPlayer fPlayer) {
        return members.contains(fPlayer);
    }

    public Region getCoreRegion() {
        return coreRegion;
    }

    public boolean isCoreRegion(@NotNull Region region) {
        return coreRegion == region;
    }

    public void setCoreRegion(Region coreRegion) {
        this.coreRegion = coreRegion;
    }

    public @NotNull Set<Region> getRegions() {
        return regions;
    }

    /**
     * Adds the region and updates resulting adjacencies.
     */
    public void addRegion(@NotNull Region region) {
        regions.add(region);
        // Add adjacent factions, if not already adjacent.
        for (Region adjacent : region.getAdjacentRegions()) {
            if (adjacent.getOwner() == null) {
                continue;
            }
            addAdjacentFaction(adjacent.getOwner());
        }
    }

    /**
     * Removes the region and updates resulting adjacencies.
     */
    public void removeRegion(@NotNull Region region) {
        regions.remove(region);
        // Check whether another region ensures the adjacency and remove adjacent faction if not.
        for (Region adjacent : region.getAdjacentRegions()) {
            Faction other = adjacent.getOwner();
            if (other == null) {
                continue;
            }
            if (!FactionUtil.isAdjacent(this, other)) {
                this.removeAdjacentFaction(other);
            }
        }
    }

    public @Nullable String getShortName() {
        return shortName;
    }

    public @NotNull String getDisplayShortName() {
        return shortName == null || shortName.isEmpty() ? name : shortName;
    }

    public void setShortName(@Nullable String shortName) {
        this.shortName = shortName;
    }

    @Override
    public boolean matchingName(@NotNull String name) {
        return super.matchingName(name) || this.shortName.equalsIgnoreCase(name);
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public @NotNull Set<Faction> getAuthorisedBuilders() {
        return authorisedBuilders;
    }

    public boolean isAuthorisedBuilder(@NotNull Faction faction) {
        return authorisedBuilders.contains(faction);
    }

    public void addAuthorisedBuilder(@NotNull Faction faction) {
        if (faction == this) {
            return;
        }
        authorisedBuilders.add(faction);
    }

    public void removeAuthorisedBuilder(@NotNull Faction faction) {
        authorisedBuilders.remove(faction);
    }

    public @NotNull Set<Faction> getAdjacentFactions() {
        return adjacentFactions;
    }

    public boolean addAdjacentFaction(@NotNull Faction other) {
        assert other != this : "Faction cannot be adjacent to itself";
        return adjacentFactions.add(other) | other.adjacentFactions.add(this);
    }

    public boolean removeAdjacentFaction(@NotNull Faction other) {
        return adjacentFactions.remove(other) | other.adjacentFactions.remove(this);
    }

    public @NotNull Set<FPlayer> getInvitedPlayers() {
        return invitedPlayers;
    }

    public boolean isInvitedPlayer(@NotNull FPlayer fPlayer) {
        return invitedPlayers.contains(fPlayer);
    }

    public void addInvitedPlayer(@NotNull FPlayer fPlayer) {
        invitedPlayers.add(fPlayer);
    }

    public void removeInvitedPlayer(@NotNull FPlayer fPlayer) {
        invitedPlayers.remove(fPlayer);
    }

    public FAccount getFAccount() {
        return fAccount;
    }

    public FStorage getStorage() {
        return fStorage;
    }

    public HashMap<PopulationLevel, Integer> getPopulation() {
        return population;
    }

    public int getPopulation(PopulationLevel level) {
        return population.getOrDefault(level, 0);
    }

    public Set<BuildSite> getFactionBuildings() {
        return buildSites;
    }

    public boolean hasBuilding(Building building) {
        return buildSites.stream().anyMatch(buildSite -> buildSite.getBuilding() == building && buildSite.isFinished() && !buildSite.isDestroyed());
    }

    public Set<BuildingEffect> getBuildingEffects() {
        return buildingEffects;
    }
}
