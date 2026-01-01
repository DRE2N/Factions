package de.erethon.factions.alliance;

import de.erethon.bedrock.misc.EnumUtil;
import de.erethon.factions.building.attributes.FactionStatAttribute;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.economy.FAccount;
import de.erethon.factions.economy.FAccountDummy;
import de.erethon.factions.economy.FAccountImpl;
import de.erethon.factions.entity.FEntity;
import de.erethon.factions.entity.FLegalEntity;
import de.erethon.factions.entity.ShortableNamed;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.poll.Poll;
import de.erethon.factions.poll.PollContainer;
import de.erethon.factions.region.ClaimableRegion;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionStructure;
import de.erethon.factions.region.WarRegion;
import de.erethon.factions.util.FBroadcastUtil;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.util.FUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Base of every faction and region.
 *
 * @author Fyreum
 */
public class Alliance extends FLegalEntity implements ShortableNamed, PollContainer {

    /* Persistent */
    private final Set<Region> coreRegions = new HashSet<>();
    private final Set<Region> temporaryRegions = new HashSet<>();
    private final Set<Faction> factions = new HashSet<>();
    private final Map<String, Poll<?>> polls = new HashMap<>();
    private BossBar.Color bossBarColor;
    private TextColor color;
    private boolean currentEmperor;
    private String icon;
    private String shortName;
    private String longName;
    private double warScore;
    private long discordCategoryId = -1;
    private long discordArchiveCategoryId = -1;
    private long discordRoleId = -1;
    /* Temporary */
    private FAccount fAccount;

    protected Alliance(@NotNull File file, int id, @NotNull String name, @Nullable String description) {
        super(file, id, name, description);
    }

    protected Alliance(@NotNull File file) throws NumberFormatException {
        super(file);
    }

    @Override
    protected void addDefaultAttributes() {
        attributes.put("tax_rate", new FactionStatAttribute(1.0));
    }

    public void temporaryOccupy(@NotNull WarRegion region) {
        FLogger.WAR.log("Region '" + region.getId() + "' was temporarily occupied by alliance '" + id + "'");
        if (region.hasAlliance()) {
            region.getAlliance().removeTemporaryRegion(region);
        }
        temporaryRegions.add(region);
        region.getRegionalWarTracker().reset(true);
        for (RegionStructure structure : region.getStructures().values()) {
            structure.onTemporaryOccupy(this);
        }
        FBroadcastUtil.broadcastWar(FMessage.WAR_REGION_OCCUPIED, name, region.getName());
    }

    /* Messages */

    @Override
    public @NotNull Iterable<? extends Audience> audiences() {
        return factions;
    }

    public void sendMessage(@NotNull Component msg) {
        sendMessage(msg, true);
    }

    public void sendMessage(@NotNull Component msg, boolean prefix) {
        Component message = prefix ? FMessage.ALLIANCE_INFO_PREFIX.message().append(msg) : msg;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getFPlayerCache().getByPlayer(player).getAlliance() != this) {
                continue;
            }
            player.sendMessage(String.valueOf(message));
        }
    }

    @Override
    public @NotNull Component asComponent(@NotNull FEntity viewer) {
        Component component = Component.text(getName(true));
        Component hoverMessage = Component.translatable("factions.alliance.info.header", getColoredName());
        hoverMessage = hoverMessage.append(Component.translatable("factions.alliance.info.header", getColoredName()));
        hoverMessage = hoverMessage.append(Component.translatable("factions.alliance.info.members", Component.text(factions.size())));
        hoverMessage = hoverMessage.append(Component.translatable("factions.alliance.info.regions", Component.text(coreRegions.size())));
        hoverMessage = hoverMessage.append(Component.translatable("factions.general.clickHints.alliance"));
        component = component.hoverEvent(HoverEvent.showText(hoverMessage));
        component = component.clickEvent(ClickEvent.runCommand("/f alliance show " + name));
        return component;
    }

    /* Serialization */

    @Override
    public void load() {
        loadRegions("coreRegions", coreRegions);
        loadRegions("temporaryRegions", temporaryRegions);
        for (int factionId : config.getIntegerList("factions")) {
            Faction faction = plugin.getFactionCache().getById(factionId);
            if (faction == null) {
                FLogger.ERROR.log("Unknown faction ID in alliance '" + id + "' found: " + factionId);
                continue;
            }
            this.factions.add(faction);
        }
        this.polls.putAll(loadPolls(config.getConfigurationSection("polls")));
        this.bossBarColor = EnumUtil.getEnumIgnoreCase(BossBar.Color.class, config.getString("bossBarColor"), BossBar.Color.WHITE);
        String colorString = config.getString("color", NamedTextColor.GRAY.toString());
        this.color = FUtil.getNotNullOr(NamedTextColor.GRAY, () -> NamedTextColor.NAMES.value(colorString), () -> TextColor.fromHexString(colorString));
        this.currentEmperor = config.getBoolean("currentEmperor");
        this.icon = config.getString("icon", "");
        this.shortName = config.getString("shortName");
        this.longName = config.getString("longName");
        this.fAccount = plugin.hasEconomyProvider() ? new FAccountImpl(this) : FAccountDummy.INSTANCE;
        this.warScore = config.getDouble("warScore", warScore);
        this.discordCategoryId = config.getLong("discordCategoryId", discordCategoryId);
        this.discordArchiveCategoryId = config.getLong("discordArchiveCategoryId", discordArchiveCategoryId);
        this.discordRoleId = config.getLong("discordRoleId", discordRoleId);
    }

    private void loadRegions(String key, Collection<Region> into) {
        for (int regionId : config.getIntegerList(key)) {
            Region region = plugin.getRegionManager().getRegionById(regionId);
            if (region == null) {
                FLogger.ERROR.log("Unknown region ID in alliance '" + id + "' found: " + regionId);
                continue;
            }
            into.add(region);
        }
    }

    @Override
    protected void serializeData() {
        saveEntities("coreRegions", coreRegions);
        saveEntities("temporaryRegions", temporaryRegions);
        saveEntities("factions", factions);
        config.set("polls", serializePolls());
        config.set("bossBarColor", bossBarColor.name());
        config.set("color", color.toString());
        config.set("currentEmperor", currentEmperor);
        config.set("icon", icon);
        config.set("shortName", shortName);
        config.set("longName", longName);
        config.set("warScore", warScore);
        config.set("discordCategoryId", discordCategoryId);
        config.set("discordArchiveCategoryId", discordArchiveCategoryId);
        config.set("discordRoleId", discordRoleId);
    }

    /* Dummy getters and setters */

    @Override
    public @Nullable Alliance getAlliance() {
        return this;
    }

    /**
     * @return always null
     * @deprecated there is no point in using this method, as it does not provide anything
     */
    @Override
    @Deprecated
    @Contract("-> null")
    public @Nullable Faction getFaction() {
        return null;
    }

    /* Getters and setters */

    public @NotNull Set<Region> getCoreRegions() {
        return coreRegions;
    }

    /**
     * Returns a Set of regions that the alliance temporarily owns.
     * These regions may also be occupied by a faction.
     *
     * @return a Set of regions that the alliance temporarily owns
     */
    public @NotNull Set<Region> getTemporaryRegions() {
        return temporaryRegions;
    }

    public void removeTemporaryRegion(@NotNull Region region) {
        if (!temporaryRegions.remove(region)) {
            return;
        }
        region.setAlliance(null);

        if (region.hasFaction() && region instanceof ClaimableRegion claimable) {
            claimable.getOwner().setOccupiedRegion(null);
            claimable.setOwner(null);
        }
        sendMessage(FMessage.ALLIANCE_INFO_REGION_LOST.message(region.getName()));
    }

    public @NotNull Set<Faction> getFactions() {
        return factions;
    }

    public void addFaction(@NotNull Faction faction) {
        factions.add(faction);
    }

    public void removeFaction(@NotNull Faction faction) {
        factions.remove(faction);
    }

    public @NotNull BossBar.Color getBossBarColor() {
        return bossBarColor;
    }

    public void setBossBarColor(@NotNull BossBar.Color bossBarColor) {
        this.bossBarColor = bossBarColor;
    }

    public @NotNull TextColor getColor() {
        return color;
    }

    public void setColor(@NotNull TextColor color) {
        this.color = color;
    }

    public boolean isCurrentEmperor() {
        return currentEmperor;
    }

    public void setCurrentEmperor(boolean currentEmperor) {
        this.currentEmperor = currentEmperor;
    }

    public @NotNull String getIcon() {
        return icon;
    }

    public boolean hasIcon() {
        return !icon.isEmpty();
    }

    public void setIcon(@NotNull String icon) {
        this.icon = icon;
    }

    public @NotNull Component getColoredName() {
        return Component.text().color(color).content(name.replace("_", " ")).build();
    }

    @Override
    public @Nullable String getShortName() {
        return shortName;
    }

    @Override
    public void setShortName(@Nullable String shortName) {
        this.shortName = shortName;
    }

    public @NotNull Component getColoredShortName() {
        return Component.text().color(color).content(getDisplayShortName()).build();
    }

    @Override
    public @Nullable String getLongName() {
        return longName;
    }

    @Override
    public void setLongName(@Nullable String longName) {
        this.longName = longName;
    }

    public @NotNull Component getColoredLongName() {
        return Component.text().color(color).content(getDisplayLongName()).build();
    }

    @Override
    public boolean matchingName(@NotNull String name) {
        return super.matchingName(name) || this.name.equalsIgnoreCase(shortName);
    }

    public double getWarScore() {
        return warScore;
    }

    public void setWarScore(double warScore) {
        this.warScore = warScore;
    }

    public void addWarScore(double amount) {
        this.warScore += amount;
    }

    public void removeWarScore(double amount) {
        this.warScore -= amount;
    }

    public long getDiscordCategoryId() {
        return discordCategoryId;
    }

    public void setDiscordCategoryId(long categoryId) {
        this.discordCategoryId = categoryId;
    }

    public long getDiscordArchiveCategoryId() {
        return discordArchiveCategoryId;
    }

    public void setDiscordArchiveCategoryId(long categoryId) {
        this.discordArchiveCategoryId = categoryId;
    }

    public long getDiscordRoleId() {
        return discordRoleId;
    }

    public void setDiscordRoleId(long roleId) {
        this.discordRoleId = roleId;
    }

    public @NotNull FAccount getFAccount() {
        return fAccount;
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
        FBroadcastUtil.broadcastIf(FMessage.ALLIANCE_INFO_NEW_POLL.message(poll.getName()), fPlayer -> fPlayer.getAlliance() == this && poll.canParticipate(fPlayer));
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
        return obj instanceof Alliance other && id == other.id;
    }
}
