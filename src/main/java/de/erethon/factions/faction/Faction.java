package de.erethon.factions.faction;

import de.erethon.aergia.util.BroadcastUtil;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.player.PlayerCollection;
import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.Building;
import de.erethon.factions.building.attributes.FactionAttribute;
import de.erethon.factions.building.attributes.FactionAttributeModifier;
import de.erethon.factions.building.attributes.FactionStatAttribute;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.economy.FAccount;
import de.erethon.factions.economy.FAccountDummy;
import de.erethon.factions.economy.FAccountImpl;
import de.erethon.factions.economy.FEconomy;
import de.erethon.factions.economy.FStorage;
import de.erethon.factions.economy.FactionLevel;
import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.entity.FLegalEntity;
import de.erethon.factions.entity.ShortableNamed;
import de.erethon.factions.event.FPlayerFactionLeaveEvent;
import de.erethon.factions.event.FactionDisbandEvent;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.poll.Poll;
import de.erethon.factions.poll.PollContainer;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FBroadcastUtil;
import de.erethon.factions.util.FException;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.util.FPermissionUtil;
import de.erethon.factions.util.FUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author Fyreum
 */
public class Faction extends FLegalEntity implements ShortableNamed, PollContainer, ForwardingAudience {

    /* Persistent */
    private Alliance alliance;
    private UUID admin;
    private PlayerCollection mods;
    private PlayerCollection members;
    private final Set<Region> regions = new HashSet<>();
    private Region coreRegion;
    private double currentTaxDebt = 0;
    private ItemStack flag;
    private String shortName;
    private String longName;
    private boolean open = false;
    private final Set<Faction> authorisedBuilders = new HashSet<>();
    private final Set<Faction> adjacentFactions = new HashSet<>();
    private final Set<BuildSite> buildSites = new HashSet<>();
    private final Map<String, Poll<?>> polls = new HashMap<>();
    private final Map<PopulationLevel, Integer> population = new HashMap<>();
    private final Map<PopulationLevel, Double> populationHappiness = new HashMap<>();
    private FStorage fStorage;
    private FactionLevel level = FactionLevel.HAMLET;
    /* Temporary */
    private final Set<BuildingEffect> buildingEffects = new HashSet<>();
    private final Set<BuildingEffect> tickingBuildingEffects = new HashSet<>();
    private final Map<String, FactionAttribute> attributes = new HashMap<>();
    private final Set<String> additionalMemberPermissions = new HashSet<>();
    private FAccount fAccount;
    private FEconomy fEconomy;
    private final Set<FPlayer> invitedPlayers = new HashSet<>();

    protected Faction(@NotNull FPlayer admin, @NotNull Region coreRegion, int id, String name, String description) {
        super(new File(Factions.FACTIONS, id + ".yml"), id, name, description);
        this.admin = admin.getUniqueId();
        this.mods = new PlayerCollection();
        this.members = new PlayerCollection();
        this.members.add(admin);
        this.regions.add(coreRegion);
        this.coreRegion = coreRegion;
        this.coreRegion.setOwner(this);
        this.fAccount = plugin.hasEconomyProvider() ? new FAccountImpl(this) : FAccountDummy.INSTANCE;
        this.fStorage = new FStorage(this);
        this.fEconomy = new FEconomy(this, fStorage);
        if (admin.hasAlliance()) {
            alliance = admin.getAlliance();
            alliance.addFaction(this);
        }
        saveData();
    }

    protected Faction(@NotNull File file) throws NumberFormatException {
        super(file);
    }

    protected void addDefaultAttributes() {
        attributes.put("max_players", new FactionStatAttribute(5));
        attributes.put("housing_peasant", new FactionStatAttribute(10));
        addPopulation(PopulationLevel.PEASANT, 5); // Let's start with something at least
    }

    public void applyModifiers() {
        for (FactionAttribute attribute : attributes.values()) {
            attribute.apply();
        }
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
        fPlayer.setFaction(this);
        fPlayer.setLastFactionJoinDate(System.currentTimeMillis());
        sendMessage(FMessage.FACTION_INFO_PLAYER_JOINED.message(fPlayer.getLastName()));
        for (BuildSite buildSite : buildSites) {
            buildSite.onFactionJoin(fPlayer);
        }
        for (String perm : additionalMemberPermissions) {
            fPlayer.getPermissionAttachment().setPermission(perm, true);
        }
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
        sendMessage(event.getMessage());
        fPlayer.removePermissionAttachment();

        if (isAdmin(fPlayer)) {
            if (members.isEmpty()) {
                disband(FactionDisbandEvent.Reason.NO_MEMBERS_LEFT);
                return;
            }
            PlayerCollection possibleSuccessors = mods.isEmpty() ? members : mods;
            FPlayer successor = null;

            for (UUID uuid : possibleSuccessors) {
                FPlayer member = plugin.getFPlayerCache().getByUniqueId(uuid);
                if (member == null) {
                    continue;
                }
                if (successor == null || member.getLastFactionJoinDate() < successor.getLastFactionJoinDate()) {
                    successor = member;
                }
            }
            admin = successor.getUniqueId();
            BroadcastUtil.broadcast(FMessage.FACTION_INFO_NEW_ADMIN.message(successor.getLastName(), name));
        }
    }

    public void disband(@NotNull FactionDisbandEvent.Reason reason) {
        FLogger.FACTION.log("Disbanding faction " + id + " (" + name + ") reason: " + reason.name() + "...");
        new FactionDisbandEvent(this, reason).callEvent();
        plugin.getFactionCache().getCache().remove(id);
        if (alliance != null) {
            alliance.removeFaction(this);
        }
        for (UUID uuid : members) {
            FPlayer member = plugin.getFPlayerCache().getByUniqueId(uuid);
            if (member == null) {
                continue;
            }
            member.setFaction(null);
        }
        admin = null;
        mods.clear();
        members.clear();
        for (Region region : regions) {
            region.setOwner(null);
        }
        fAccount.setBalance(0);
        try {
            file.delete();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        BroadcastUtil.broadcast(FMessage.FACTION_INFO_DISBANDED.message(name));
    }

    private void updateMemberDisplayNames() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (Player online : members.getOnlinePlayers()) {
                FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(online);
                fPlayer.updateDisplayNames();
            }
        });
    }

    /* Messages */

    @Override
    public @NotNull Iterable<? extends Audience> audiences() {
        return members.getOnlinePlayers();
    }

    public void sendMessage(@NotNull Component msg) {
        sendMessage(msg, true);
    }

    public void sendMessage(@NotNull Component msg, boolean prefix) {
        Component message = prefix ? FMessage.FACTION_INFO_PREFIX.message(name).append(msg) : msg;
        for (Player online : members.getOnlinePlayers()) {
            online.sendMessage(message);
        }
    }

    public void sendTranslatable(String key) {
        sendMessage(Component.translatable(key, key));
    }

    public void sendTranslatable(String key, boolean prefix) {
        sendMessage(Component.translatable(key, key), prefix);
    }

    public void sendTranslatable(String key, Component... args) {
        sendMessage(Component.translatable(key, key, args));
    }

    public void sendTranslatable(String key, boolean prefix, Component... args) {
        sendMessage(Component.translatable(key, key, args), prefix);
    }

    /* Serialization */

    @Override
    public void load() {
        FLogger.FACTION.log("Loading faction '" + id + "'...");
        this.alliance = plugin.getAllianceCache().getById(config.getInt("alliance", -1));
        String adminId = config.getString("admin", "null");
        try {
            this.admin = UUID.fromString(adminId);
        } catch (IllegalArgumentException e) {
            FLogger.ERROR.log("Illegal UUID in faction '" + id + "' found: '" + adminId + "'");
        }
        this.mods = new PlayerCollection(config.getStringList("mods"));
        this.members = new PlayerCollection(config.getStringList("members"));
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
        this.currentTaxDebt = config.getDouble("currentTaxDebt");
        this.flag = config.getItemStack("flag");
        this.shortName = config.getString("shortName");
        this.longName = config.getString("longName");
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
        this.polls.putAll(loadPolls(config.getConfigurationSection("polls")));
        this.level = FactionLevel.getByName(config.getString("level"), FactionLevel.HAMLET);
        for (PopulationLevel level : PopulationLevel.values()) {
            this.population.put(level, config.getInt("population." + level.name(), 0));
        }
        this.fAccount = plugin.hasEconomyProvider() ? new FAccountImpl(this) : FAccountDummy.INSTANCE;
        this.fStorage = new FStorage(this, config.getConfigurationSection("storage"));
    }

    @Override
    protected void serializeData() {
        config.set("alliance", alliance == null ? null : alliance.getId());
        config.set("admin", admin.toString());
        config.set("mods", mods.serialize());
        config.set("members", members.serialize());
        saveEntities("regions", regions);
        config.set("coreRegion", coreRegion == null ? null : coreRegion.getId());
        config.set("currentTaxDebt", currentTaxDebt);
        config.set("flag", flag);
        config.set("shortName", shortName);
        config.set("longName", longName);
        config.set("open", open);
        config.set("level", level.name());
        config.set("polls", serializePolls());
        for (PopulationLevel level : PopulationLevel.values()) {
            config.set("population." + level.name(), population.get(level));
        }
        config.set("storage", fStorage.save());
        saveEntities("authorisedBuilders", authorisedBuilders);
        saveEntities("adjacentFactions", adjacentFactions);
    }

    /* Permission stuff */

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

    public @NotNull UUID getAdmin() {
        return admin;
    }

    public boolean isAdmin(@NotNull FPlayer fPlayer) {
        return isAdmin(fPlayer.getUniqueId());
    }

    public boolean isAdmin(@NotNull UUID uuid) {
        return admin.equals(uuid);
    }

    public void setAdmin(@NotNull FPlayer fPlayer) {
        setAdmin(fPlayer.getUniqueId());
    }

    public void setAdmin(@NotNull UUID uuid) {
        this.admin = uuid;
    }

    public @NotNull PlayerCollection getMods() {
        return mods;
    }

    public boolean isMod(@NotNull FPlayer fPlayer) {
        return mods.contains(fPlayer);
    }

    public boolean isMod(@NotNull UUID uuid) {
        return mods.contains(uuid);
    }

    public void addMod(@NotNull FPlayer fPlayer) {
        mods.add(fPlayer);
    }

    public @NotNull PlayerCollection getMembers() {
        return members;
    }

    public boolean isMember(@NotNull FPlayer fPlayer) {
        return members.contains(fPlayer);
    }

    public @NotNull Region getCoreRegion() {
        return coreRegion;
    }

    public boolean isCoreRegion(@NotNull Region region) {
        return coreRegion == region;
    }

    public void setCoreRegion(@NotNull Region coreRegion) {
        this.coreRegion = coreRegion;
    }

    public boolean hasCurrentTaxDebt() {
        return currentTaxDebt > 0;
    }

    public double getCurrentTaxDebt() {
        return currentTaxDebt;
    }

    public void setCurrentTaxDebt(double currentTaxDebt) {
        this.currentTaxDebt = Math.max(currentTaxDebt, 0);
    }

    public void addCurrentTaxDebt(double taxDebt) {
        this.currentTaxDebt += taxDebt;
    }

    public void removeCurrentTaxDebt(double taxDebt) {
        this.currentTaxDebt = Math.max(currentTaxDebt - taxDebt, 0);
    }

    public double calculateRegionTaxes() {
        double amount = 0;
        for (Region region : regions) {
            amount += region.getLastClaimingPrice();
        }
        return amount * plugin.getFConfig().getRegionPriceTaxRate();
    }

    public @Nullable ItemStack getFlag() {
        return flag;
    }

    public void setFlag(@Nullable ItemStack flag) {
        if (flag == null) {
            this.flag = null;
            return;
        }
        if (!(flag.getItemMeta() instanceof BannerMeta bannerMeta)) {
            throw new FException("Flag ItemStack must be a banner item", FMessage.ERROR_NO_BANNER_ITEM_IN_HAND);
        }
        ItemStack copy = new ItemStack(flag.getType());
        BannerMeta copyMeta = (BannerMeta) copy.getItemMeta();
        copyMeta.setBaseColor(bannerMeta.getBaseColor());
        copyMeta.setPatterns(bannerMeta.getPatterns());
        copy.setItemMeta(copyMeta);
        this.flag = copy;
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
            if (!FUtil.isAdjacent(this, other)) {
                removeAdjacentFaction(other);
            }
        }
    }

    @Override
    public void setName(@NotNull String name) {
        super.setName(name);
        updateMemberDisplayNames();
    }

    @Override
    public @Nullable String getShortName() {
        return shortName;
    }

    @Override
    public void setShortName(@Nullable String shortName) {
        this.shortName = shortName;
        updateMemberDisplayNames();
    }

    @Override
    public @Nullable String getLongName() {
        return longName;
    }

    @Override
    public void setLongName(@Nullable String longName) {
        this.longName = longName;
    }

    @Override
    public boolean matchingName(@NotNull String name) {
        return super.matchingName(name) || this.name.equalsIgnoreCase(shortName);
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

    public boolean toggleAuthorisedBuilder(@NotNull Faction faction) {
        if (faction.isAuthorisedBuilder(faction)) {
            faction.removeAuthorisedBuilder(faction);
            return false;
        } else {
            faction.addAuthorisedBuilder(faction);
            return true;
        }
    }

    public void addAuthorisedBuilder(@NotNull Faction faction) {
        if (faction == this) {
            return;
        }
        if (!authorisedBuilders.add(faction)) {
            return;
        }
        sendMessage(FMessage.FACTION_INFO_ADDED_BUILDER_AUTHORITY.message(faction.getDisplayShortName()));
        faction.sendMessage(FMessage.FACTION_INFO_ADDED_BUILDER_AUTHORITY_OTHER.message(getDisplayShortName()));
    }

    public void removeAuthorisedBuilder(@NotNull Faction faction) {
        if (!authorisedBuilders.remove(faction)) {
            return;
        }
        sendMessage(FMessage.FACTION_INFO_ADDED_BUILDER_AUTHORITY.message(faction.getDisplayShortName()));
        faction.sendMessage(FMessage.FACTION_INFO_ADDED_BUILDER_AUTHORITY_OTHER.message(getDisplayShortName()));
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

    public @NotNull Set<BuildSite> getFactionBuildings() {
        return buildSites;
    }

    public boolean hasBuilding(@NotNull Building building) {
        return buildSites.stream().anyMatch(buildSite -> buildSite.getBuilding() == building && buildSite.isFinished() && !buildSite.isDestroyed());
    }

    public boolean hasBuilding(@NotNull String buildingId) {
        return buildSites.stream().anyMatch(buildSite -> buildSite.getBuilding().getId().equals(buildingId) && buildSite.isFinished() && !buildSite.isDestroyed());
    }

    public @NotNull Map<PopulationLevel, Integer> getPopulation() {
        return population;
    }

    public int getPopulation(@NotNull PopulationLevel level) {
        return population.getOrDefault(level, 0);
    }

    public void addPopulation(@NotNull PopulationLevel level, int amount) {
        population.put(level, population.getOrDefault(level, 0) + amount);
    }

    public double getHappiness(@NotNull PopulationLevel level) {
        return populationHappiness.getOrDefault(level, 0.0);
    }

    public void setHappiness(@NotNull PopulationLevel level, double happiness) {
        populationHappiness.put(level, populationHappiness.getOrDefault(level, 0.0) + happiness);
    }

    public @NotNull FStorage getStorage() {
        return fStorage;
    }

    public @NotNull FEconomy getEconomy() {
        return fEconomy;
    }

    public @NotNull FactionLevel getLevel() {
        return level;
    }

    public void setLevel(@NotNull FactionLevel level) {
        this.level = level;
    }

    public @NotNull Set<BuildingEffect> getBuildingEffects() {
        return buildingEffects;
    }

    public @NotNull Set<BuildingEffect> getTickingBuildingEffects() {
        return tickingBuildingEffects;
    }

    public @NotNull Set<String> getAdditionalMemberPermissions() {
        return additionalMemberPermissions;
    }

    public @NotNull Map<String, FactionAttribute> getAttributes() {
        return attributes;
    }

    public void addModifier(FactionAttribute attribute, FactionAttributeModifier modifier) {
        attribute.addModifier(modifier);
        applyModifiers();
    }

    public void removeModifier(FactionAttribute attribute, FactionAttributeModifier modifier) {
        attribute.getModifiers().remove(modifier);
        applyModifiers();
    }

    public void removeModifier(FactionAttributeModifier modifier) {
        for (FactionAttribute attribute : attributes.values()) {
            attribute.getModifiers().remove(modifier);
        }
        applyModifiers();
    }

    public @Nullable FactionAttribute getAttribute(@NotNull String name) {
        FactionAttribute attribute = attributes.get(name);
        return attribute == null ? null : attribute.apply();
    }

    public FactionAttribute getOrCreateStatAttribute(@NotNull String name) {
        attributes.putIfAbsent(name, new FactionStatAttribute(0));
        return attributes.get(name);
    }

    public double getAttributeValue(@NotNull String name) {
        FactionAttribute attribute = attributes.get(name);
        return attribute == null ? 0 : attribute.apply().getValue();
    }

    public @NotNull FAccount getFAccount() {
        return fAccount;
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

    @Override
    public @NotNull Map<String, Poll<?>> getPolls() {
        return polls;
    }

    @Override
    public void addPoll(@NotNull Poll<?> poll) {
        addPoll(poll, Poll.DEFAULT_DURATION);
    }

    @Override
    public void addPoll(@NotNull Poll<?> poll, long duration) {
        if (!poll.isOpen()) {
            poll.openPoll(duration);
        }
        polls.put(poll.getName(), poll);
        FBroadcastUtil.broadcastIf(FMessage.FACTION_INFO_NEW_POLL.message(poll.getName()), fPlayer -> fPlayer.getFaction() == this && poll.canParticipate(fPlayer));
    }

    @Override
    public void removePoll(@NotNull Poll<?> poll) {
        polls.remove(poll.getName());
        if (poll.isOpen()) {
            poll.closePoll();
        }
        HandlerList.unregisterAll(poll);
    }

    /* Object methods */

    @Override
    public int hashCode() {
        return id; // IDs are unique
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Faction other && id == other.id;
    }
}
