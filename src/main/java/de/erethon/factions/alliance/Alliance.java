package de.erethon.factions.alliance;

import de.erethon.factions.data.FMessage;
import de.erethon.factions.entity.FLegalEntity;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.poll.Poll;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FBroadcastUtil;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.war.WarScores;
import net.kyori.adventure.text.Component;
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
public class Alliance extends FLegalEntity {

    /* Data */
    private final Set<Region> coreRegions = new HashSet<>();
    private final Set<Region> temporaryRegions = new HashSet<>();
    private final Set<Region> unconfirmedTemporaryRegions = new HashSet<>();
    private final Set<Faction> factions = new HashSet<>();
    private String shortName;
    private WarScores warScores;
    /* Functionality */
    private final Map<String, Poll<?>> activePolls = new HashMap<>();

    protected Alliance(@NotNull File file, int id, @NotNull String name, @Nullable String description) {
        super(file, id, name, description);
    }

    protected Alliance(@NotNull File file) throws NumberFormatException {
        super(file);
    }

    /* Messages */

    public void sendMessage(@NotNull Component msg) {
        sendMessage(msg, true);
    }

    public void sendMessage(@NotNull Component msg, boolean prefix) {
        Component message = prefix ? FMessage.ALLIANCE_INFO_PREFIX.message().append(msg) : msg;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getFPlayerCache().getByPlayer(player).getAlliance() != this) {
                continue;
            }
            player.sendMessage(message);
        }
    }

    /* Serialization */

    @Override
    public void load() {
        loadRegions("coreRegions", coreRegions);
        loadRegions("temporaryRegions", temporaryRegions);
        loadRegions("unconfirmedTemporaryRegions", unconfirmedTemporaryRegions);
        for (int factionId : config.getIntegerList("factions")) {
            Faction faction = plugin.getFactionCache().getById(factionId);
            if (faction == null) {
                FLogger.ERROR.log("Unknown faction ID in alliance '" + id + "' found: " + factionId);
                continue;
            }
            this.factions.add(faction);
        }
        this.shortName = config.getString("shortName");
        this.warScores = new WarScores(this, config.getConfigurationSection("warScores"));
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
        saveEntities("unconfirmedTemporaryRegions", unconfirmedTemporaryRegions);
        saveEntities("factions", factions);
        config.set("shortName", shortName);
        config.set("warScores", warScores.serialize());
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

    public @NotNull Set<Region> getTemporaryRegions() {
        return temporaryRegions;
    }

    public @NotNull Set<Region> getUnconfirmedTemporaryRegions() {
        return unconfirmedTemporaryRegions;
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

    public @NotNull WarScores getWarScores() {
        return warScores;
    }

    public @NotNull Map<String, Poll<?>> getActivePolls() {
        return activePolls;
    }

    public void addPoll(@NotNull Poll<?> poll) {
        activePolls.put(poll.getName(), poll);
        FBroadcastUtil.broadcastIf(FMessage.ALLIANCE_INFO_NEW_POLL.message(poll.getName()), fPlayer -> fPlayer.getAlliance() == this && poll.canParticipate(fPlayer));
    }

    public void removePoll(@NotNull Poll<?> poll) {
        activePolls.remove(poll.getName());
        HandlerList.unregisterAll(poll);
    }
}
