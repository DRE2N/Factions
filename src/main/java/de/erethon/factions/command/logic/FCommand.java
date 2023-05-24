package de.erethon.factions.command.logic;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.CommandFailedException;
import de.erethon.bedrock.command.ECommand;
import de.erethon.factions.Factions;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.entity.FEntityCache;
import de.erethon.factions.entity.FLegalEntity;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.RegionCache;
import de.erethon.factions.region.RegionType;
import de.erethon.factions.util.FException;
import de.erethon.factions.util.FPermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Fyreum
 */
public abstract class FCommand extends ECommand {

    public static final String PERM_PREFIX = FCommandCache.LABEL + ".cmd.";

    protected final Factions plugin = Factions.get();

    public FCommand() {
        setPlayerCommand(true);
        setExecutionPrefix("f ");
    }

    @Override
    public final void onExecute(String[] args, CommandSender sender) {
        try {
            onExecute(sender, args);
        } catch (FException e) {
            sender.sendMessage(e.getPlayerMessage());
        }
    }

    public abstract void onExecute(CommandSender sender, String[] args);

    @Override
    public void addSubCommand(ECommand command) {
        if (command.getHelp() == null) {
            command.setDefaultHelp();
        }
        super.addSubCommand(command);
    }

    @Override
    public void addSubCommands(ECommand... commands) {
        for (ECommand command : commands) {
            addSubCommand(command);
        }
    }

    @Override
    public void setDefaultHelp() {
        setHelp(formatDefaultHelp(getUsage(), getDescription()));
    }

    public void setFUsage(String usage) {
        setUsage("/f " + usage);
    }

    public void setPermissionFromName() {
        setPermission(PERM_PREFIX + getCommand());
    }

    public void setPermissionFromName(String between) {
        setPermission(PERM_PREFIX + between + "." + getCommand());
    }

    /* Utility methods */

    protected @NotNull FPlayer getFPlayer(@NotNull CommandSender sender) {
        assureSenderIsAPlayer(sender);
        return getFPlayerRaw(sender);
    }

    protected FPlayer getFPlayerRaw(@NotNull CommandSender sender) {
        return plugin.getFPlayerCache().getByPlayer((Player) sender);
    }

    protected @NotNull FPlayer getFPlayer(@NotNull String arg) {
        FPlayer fPlayer;
        try {
            UUID uuid = UUID.fromString(arg);
            fPlayer = plugin.getFPlayerCache().getByUniqueId(uuid);
        } catch (IllegalArgumentException e) {
            fPlayer = plugin.getFPlayerCache().getByName(arg);
        }
        assure(fPlayer != null, FMessage.ERROR_PLAYER_NOT_FOUND, arg);
        return fPlayer;
    }

    protected @NotNull FPlayer getFPlayerInFaction(@NotNull String arg) {
        FPlayer fPlayer = getFPlayer(arg);
        assure(fPlayer.getFaction() != null, FMessage.ERROR_TARGET_IS_NOT_IN_A_FACTION, fPlayer.getLastName());
        return fPlayer;
    }

    protected @NotNull FPlayer getOnlineFPlayer(@NotNull String arg) {
        FPlayer fPlayer = getFPlayer(arg);
        assure(fPlayer.isOnline(), FMessage.ERROR_PLAYER_NOT_FOUND, arg);
        return fPlayer;
    }

    protected @NotNull Region getRegion(@NotNull String arg) {
        Region region;
        try {
            int regionId = Integer.parseInt(arg);
            region = plugin.getRegionManager().getRegionById(regionId);
        } catch (NumberFormatException e) {
            region = plugin.getRegionManager().getRegionByName(arg);
        }
        assure(region != null, FMessage.ERROR_REGION_NOT_FOUND, arg);
        return region;
    }

    protected @NotNull Region getRegion(@NotNull FPlayer fPlayer) {
        Region region = fPlayer.getCurrentRegion();
        assure(region != null, FMessage.ERROR_PLAYER_REGION_NULL);
        return region;
    }

    protected @NotNull Region getClaimableRegion(@NotNull String arg) {
        Region region = getRegion(arg);
        assureRegionIsUnowned(region);
        assureRegionIsClaimable(region);
        return region;
    }

    protected @NotNull Region getClaimableRegion(@NotNull FPlayer fPlayer) {
        Region region = getRegion(fPlayer);
        assureRegionIsUnowned(region);
        assureRegionIsClaimable(region);
        return region;
    }

    protected @NotNull Alliance getAlliance(@NotNull String arg) {
        Alliance alliance = getAllianceRaw(arg);
        assure(alliance != null, FMessage.ERROR_FACTION_NOT_FOUND, arg);
        return alliance;
    }

    protected @NotNull Alliance getAlliance(@NotNull FPlayer fPlayer) {
        Alliance alliance = fPlayer.getAlliance();
        assure(alliance != null, FMessage.ERROR_PLAYER_IS_NOT_IN_A_FACTION);
        return alliance;
    }

    protected @Nullable Alliance getAllianceRaw(@NotNull String arg) {
        Alliance alliance;
        try {
            int factionId = Integer.parseInt(arg);
            alliance = plugin.getAllianceCache().getById(factionId);
        } catch (NumberFormatException e) {
            alliance = plugin.getAllianceCache().getByName(arg);
        }
        return alliance;
    }

    protected @NotNull Faction getFaction(@NotNull String arg) {
        Faction faction = getFactionRaw(arg);
        assure(faction != null, FMessage.ERROR_FACTION_NOT_FOUND, arg);
        return faction;
    }

    protected @NotNull Faction getFaction(@NotNull FPlayer fPlayer) {
        Faction faction = fPlayer.getFaction();
        assure(faction != null, FMessage.ERROR_PLAYER_IS_NOT_IN_A_FACTION);
        return faction;
    }

    protected @Nullable Faction getFactionRaw(@NotNull String arg) {
        Faction faction;
        try {
            int factionId = Integer.parseInt(arg);
            faction = plugin.getFactionCache().getById(factionId);
        } catch (NumberFormatException e) {
            faction = plugin.getFactionCache().getByName(arg);
            if (faction == null) {
                faction = getPlayerFaction(arg);
            }
        }
        return faction;
    }

    protected @Nullable Faction getFactionRaw(@NotNull CommandSender sender) {
        return sender instanceof Player ? getFPlayerRaw(sender).getFaction() : null;
    }

    protected @Nullable Faction getPlayerFaction(@NotNull String playerName) {
        FPlayer fPlayer = plugin.getFPlayerCache().getByName(playerName);
        return fPlayer == null ? null : fPlayer.getFaction();
    }

    // Map.Entry#getValue() == true, when the faction was found through the arg.
    protected @NotNull Map.Entry<Faction, Boolean> getSenderFactionOrFromArgs(@NotNull CommandSender sender, @NotNull String arg) {
        Faction faction = getFactionRaw(arg);
        return faction == null ? new AbstractMap.SimpleEntry<>(getFaction(getFPlayer(sender)), false) : new AbstractMap.SimpleEntry<>(faction, true);
    }

    protected boolean sameFaction(@NotNull FPlayer player, @NotNull FPlayer target) {
        return player.getFaction() != null && player.getFaction() == target.getFaction();
    }

    protected @NotNull List<String> getTabList(@NotNull Collection<String> list, @NotNull String arg) {
        return list.stream().filter(s -> s.toLowerCase().startsWith(arg.toLowerCase())).collect(Collectors.toList());
    }

    protected <E> @NotNull List<String> getTabList(@NotNull Collection<E> list, @NotNull Function<E, String> converter, @NotNull String arg) {
        return list.stream().filter(e -> converter.apply(e).toLowerCase().startsWith(arg.toLowerCase())).map(converter).collect(Collectors.toList());
    }

    protected <E> @NotNull List<String> getTabList(@NotNull E[] array, @NotNull Function<E, String> converter, @NotNull String arg) {
        return Arrays.stream(array).filter(e -> converter.apply(e).toLowerCase().startsWith(arg.toLowerCase())).map(converter).collect(Collectors.toList());
    }

    protected @NotNull List<String> getTabEntities(@NotNull FEntityCache<?> cache, @NotNull String arg) {
        return getTabList(cache.getCache().values(), FLegalEntity::getName, arg);
    }

    protected @NotNull List<String> getTabAlliances(@NotNull String arg) {
        return getTabEntities(plugin.getAllianceCache(), arg);
    }

    protected @NotNull List<String> getTabFactions(@NotNull String arg) {
        return getTabEntities(plugin.getFactionCache(), arg);
    }

    protected @NotNull List<String> getTabFactions(@NotNull CommandSender sender, @NotNull String arg) {
        if (FPermissionUtil.isBypass(sender)) {
            return getTabEntities(plugin.getFactionCache(), arg);
        }
        FPlayer fPlayer = getFPlayerRaw(sender);
        if (fPlayer == null) {
            return List.of();
        }
        Faction faction = fPlayer.getFaction();
        return faction != null && faction.getName().toLowerCase().startsWith(arg.toLowerCase()) ? List.of(faction.getName()) : List.of();
    }

    protected @NotNull List<String> getTabRegions(@NotNull Player player, @NotNull String arg) {
        return getTabRegions(player.getWorld(), arg);
    }

    protected @NotNull List<String> getTabRegions(@NotNull World world, @NotNull String arg) {
        RegionCache cache = plugin.getRegionManager().getCache(world);
        if (cache == null) {
            return List.of();
        }
        return getTabEntities(cache, arg);
    }

    protected @NotNull List<String> getTabRegions(@NotNull String arg) {
        List<String> regions = new ArrayList<>();
        for (RegionCache cache : plugin.getRegionManager().getCaches().values()) {
            regions.addAll(getTabEntities(cache, arg));
        }
        return regions;
    }

    protected @NotNull List<String> getTabRegionTypes(@NotNull String arg) {
        return getTabList(RegionType.values(), Enum::name, arg);
    }

    protected @NotNull List<String> getTabPlayers(@NotNull String arg) {
        return getTabList(Bukkit.getOnlinePlayers(), Player::getName, arg);
    }

    protected @NotNull List<String> getTabObjectives(@NotNull String arg) {
        return getTabList(plugin.getWarObjectiveManager().getObjectives().keySet(), arg);
    }

    /* Assure methods */

    protected void fAssure(boolean b, @NotNull Supplier<String> message) {
        if (!b) {
            throw new CommandFailedException(message.get());
        }
    }

    protected void assureSenderIsAPlayer(@NotNull CommandSender sender) {
        assure(sender instanceof Player, FMessage.ERROR_SENDER_IS_NO_PLAYER);
    }

    protected void assureSenderHasAdminPerms(@NotNull CommandSender sender, @NotNull Faction faction) {
        if (FPermissionUtil.isBypass(sender)) {
            return;
        }
        assure(sender instanceof Player player && faction.isAdmin(plugin.getFPlayerCache().getByPlayer(player)), FMessage.ERROR_NO_PERMISSION);
    }

    protected void assurePlayerIsFactionless(@NotNull FPlayer fPlayer) {
        assure(!fPlayer.hasFaction(), FMessage.ERROR_PLAYER_ALREADY_IN_A_FACTION);
    }

    protected void assurePlayerHasFaction(@NotNull FPlayer fPlayer) {
        assure(fPlayer.hasFaction(), FMessage.ERROR_PLAYER_IS_NOT_IN_A_FACTION);
    }

    protected void assureChunkIsNoRegion(@NotNull World world, @NotNull Chunk chunk) {
        RegionCache cache = plugin.getRegionManager().getCache(world);
        assure(cache == null || cache.getByChunk(chunk) == null, FMessage.ERROR_CHUNK_ALREADY_A_REGION);
    }

    protected void assureChunkIsNoRegion(@NotNull RegionCache cache, @NotNull Chunk chunk) {
        assure(cache.getByChunk(chunk) == null, FMessage.ERROR_CHUNK_ALREADY_A_REGION);
    }

    protected void assureRegionIsUnowned(@NotNull Region region) {
        assure(region.getOwner() == null, FMessage.ERROR_REGION_ALREADY_CLAIMED);
    }

    protected void assureRegionIsClaimable(@NotNull Region region) {
        assure(region.isClaimable(), FMessage.ERROR_REGION_IS_NOT_CLAIMABLE);
    }

    protected void assureSameWorld(@NotNull Region region, @NotNull Player player) {
        assure(region.getWorldId().equals(player.getWorld().getUID()), FMessage.ERROR_REGION_IN_ANOTHER_WORLD);
    }

    /* Statics */

    public static @NotNull String colorUsage(@Nullable String usage) {
        if (usage == null) {
            return "MISSING_USAGE";
        }
        return "<dark_green>" + MessageUtil.stripColor(usage)
                .replace("[", "[<green>")
                .replace("|", "</green>|<green>")
                .replace("]", "</green>]");
    }

    public static @NotNull String formatDefaultHelp(@Nullable String usage, @Nullable String description) {
        return description != null ? colorUsage(usage) + " <dark_gray>-<gray> " + description : colorUsage(usage);
    }
}
