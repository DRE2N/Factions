package de.erethon.factions;

import de.erethon.aergia.placeholder.ChatPlaceholder;
import de.erethon.aergia.placeholder.ChatPlaceholders;
import de.erethon.aergia.placeholder.HoverEventBuilder;
import de.erethon.aergia.placeholder.HoverInfo;
import de.erethon.aergia.util.TickUtil;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.compatibility.Internals;
import de.erethon.bedrock.misc.FileUtil;
import de.erethon.bedrock.misc.Registry;
import de.erethon.bedrock.plugin.EPlugin;
import de.erethon.bedrock.plugin.EPluginSettings;
import de.erethon.factions.alliance.AllianceCache;
import de.erethon.factions.building.BuildSiteCache;
import de.erethon.factions.building.BuildingManager;
import de.erethon.factions.command.logic.FCommandCache;
import de.erethon.factions.data.FConfig;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.economy.TaxManager;
import de.erethon.factions.faction.FactionCache;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.player.FPlayerCache;
import de.erethon.factions.player.FPlayerListener;
import de.erethon.factions.poll.Poll;
import de.erethon.factions.poll.polls.CapturedRegionsPoll;
import de.erethon.factions.region.AutomatedChunkManager;
import de.erethon.factions.region.RegionManager;
import de.erethon.factions.region.schematic.RegionSchematicManager;
import de.erethon.factions.ui.UIFactionsListener;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.war.WarHistory;
import de.erethon.factions.war.WarListener;
import de.erethon.factions.war.WarPhaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public final class Factions extends EPlugin {

    private static Factions instance;

    public static final int AERGIA_PLACEHOLDER_WEIGHT = 2;

    /* Folders */
    public static File ALLIANCES;
    public static File BACKUPS;
    public static File BUILDINGS;
    public static File BUILD_SITES;
    public static File FACTIONS;
    public static File REGIONS;
    public static File SCHEMATICS;
    public static File PLAYERS;
    public static File WAR;
    public static File WAR_HISTORY;

    /* Files */
    private File fLoggerFile;
    private File fConfigFile;
    private File warPhaseManagerFile;

    /* Registries */
    private Registry<String, Function<ConfigurationSection, Poll<?>>> pollDeserializerRegistry;

    /* Configs */
    private FConfig fConfig;

    /* Caches */
    private AllianceCache allianceCache;
    private FactionCache factionCache;
    private RegionManager regionManager;
    private FPlayerCache fPlayerCache;
    private FCommandCache fCommandCache;
    private BuildSiteCache buildSiteCache;

    /* Instances */
    private BuildingManager buildingManager;
    private RegionSchematicManager regionSchematicManager;
    private TaxManager taxManager;
    private WarHistory warHistory;
    private WarPhaseManager warPhaseManager;

    /* Tasks */
    private BukkitTask backupTask;
    private BukkitTask saveDataTask;

    /* Listeners */
    private FPlayerListener fPlayerListener;
    private UIFactionsListener uiFactionsListener;
    private WarListener warListener;

    public Factions() {
        settings = EPluginSettings.builder()
                .internals(Internals.v1_20_R1.andHigher())
                .forcePaper(true)
                .economy(true)
                .build();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        loadRegistries();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;
        loadCore();
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTasks(this);
        unregisterAergiaPlaceholders();
        saveData();
    }

    public void loadCore() {
        initFolders();
        initFiles();
        loadFLogger();
        loadConfigs();
        loadFMessages();
        initializeCaches();
        loadCaches();
        if (hasEconomyProvider()) {
            loadTaxManager();
        }
        loadWarHistory();
        loadWarPhaseManager();
        runTasks();
        loadCommands();
        registerListeners();
        registerAergiaPlaceholders();
    }

    public void initFolders() {
        initFolder(getDataFolder());
        initFolder(ALLIANCES = new File(getDataFolder(), "alliances"));
        initFolder(BACKUPS = new File(getDataFolder(), "backups"));
        initFolder(BUILDINGS = new File(getDataFolder(), "buildings"));
        initFolder(BUILD_SITES = new File(getDataFolder(), "sites"));
        initFolder(FACTIONS = new File(getDataFolder(), "factions"));
        initFolder(REGIONS = new File(getDataFolder(), "regions"));
        initFolder(SCHEMATICS = new File(getDataFolder(), "schematics"));
        initFolder(PLAYERS = new File(getDataFolder(), "players"));
        initFolder(WAR = new File(getDataFolder(), "war"));
        initFolder(WAR_HISTORY = new File(WAR, "history"));
    }

    public void initFiles() {
        fLoggerFile = new File(getDataFolder(), "logger.yml");
        fConfigFile = new File(getDataFolder(), "config.yml");
        warPhaseManagerFile = FileUtil.initFile(this, new File(WAR, "schedule.yml"), "defaults/schedule.yml");
    }

    public void loadFLogger() {
        FLogger.load(fLoggerFile);
    }

    public void loadRegistries() {
        pollDeserializerRegistry = new Registry<>();
        pollDeserializerRegistry.add(CapturedRegionsPoll.class.getSimpleName(), CapturedRegionsPoll::new);
    }

    public void loadConfigs() {
        fConfig = new FConfig(fConfigFile);
        fConfig.lateInit();
        fConfig.lateLoad();
    }

    public void loadFMessages() {
        reloadBedrockMessageHandler();
        reloadMessageHandler();
        bedrockMessageHandler.setDefaultLanguage(fConfig.getLanguage());
        messageHandler.setDefaultLanguage(fConfig.getLanguage());
    }

    public void initializeCaches() {
        allianceCache = new AllianceCache(ALLIANCES);
        buildingManager = new BuildingManager(BUILDINGS);
        regionSchematicManager = new RegionSchematicManager(SCHEMATICS);
        factionCache = new FactionCache(FACTIONS);
        regionManager = new RegionManager(REGIONS);
        fPlayerCache = new FPlayerCache(this);
        buildSiteCache = new BuildSiteCache(BUILD_SITES);
    }

    public void loadCaches() {
        allianceCache.loadAll();
        factionCache.loadAll();
        regionManager.loadAll();
        fPlayerCache.loadAll();
    }

    public void loadTaxManager() {
        taxManager = new TaxManager();
    }

    public void loadWarHistory() {
        warHistory = new WarHistory(WAR_HISTORY);
        warHistory.load();
    }

    public void loadWarPhaseManager() {
        warPhaseManager = new WarPhaseManager(warPhaseManagerFile);
        warPhaseManager.load();
    }

    public void runTasks() {
        factionCache.runKickTask();
        if (taxManager != null) {
            taxManager.runFactionTaxTask();
        }
        warPhaseManager.updateCurrentStageTask();
        runBackupTask();
        runSaveDataTask();
    }

    public void loadCommands() {
        setCommandCache(fCommandCache = new FCommandCache(this));
        fCommandCache.register(this);
    }

    public void registerListeners() {
        register(fPlayerListener = new FPlayerListener());
        register(uiFactionsListener = new UIFactionsListener());
        register(warListener = new WarListener());
    }

    private void register(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, this);
    }

    public void registerAergiaPlaceholders() {
        // Add UI components.
        for (FPlayer fPlayer : fPlayerCache.getCachedUsers()) {
            uiFactionsListener.applyToUIUpdater(fPlayer.getUIUpdater());
        }
        // Register faction placeholders.
        ChatPlaceholders.register(ChatPlaceholder.builder()
                .placeHolder("faction")
                .baseBuilder((s, r) -> {
                    FPlayer fSender = fPlayerCache.getByPlayer(s.getPlayer());
                    String faction = fSender.hasFaction() ? fSender.getFaction().getDisplayShortName() : FMessage.GENERAL_LONER.getMessage();
                    return Component.text(fSender.hasAlliance() && fSender.getAlliance().hasIcon() ? fSender.getAlliance().getIcon() + " " : "")
                            .append(Component.text()
                                    .color(fSender.getAllianceColor())
                                    .append(MessageUtil.parse("<dark_gray>[</dark_gray>" + faction + "<dark_gray>]</dark_gray> "))
                                    .build());
                })
                .clickBuilder((s, r) -> {
                    FPlayer fPlayer = fPlayerCache.getByPlayer(s.getPlayer());
                    return !fPlayer.hasFaction() ? null : ClickEvent.suggestCommand("/f show " + fPlayer.getFaction().getName());
                })
                .addHoverInfo((s, r) -> {
                    FPlayer fPlayer = fPlayerCache.getByPlayer(s.getPlayer());
                    return !fPlayer.hasFaction() ? null : MessageUtil.parse("<gold>Fraktion</gold><dark_gray>:</dark_gray><gray> " + fPlayer.getFaction().getName());
                })
                .build());
        ChatPlaceholders.register(ChatPlaceholder.builder()
                .placeHolder("faction-rank")
                .baseStringBuilder((s, r) -> {
                    FPlayer fPlayer = fPlayerCache.getByPlayer(s.getPlayer());
                    if (!fPlayer.hasFaction()) {
                        return "";
                    }
                    return fPlayer.isAdminRaw() ? fConfig.getFactionChatAdminIcon() : fPlayer.isMod() ? fConfig.getFactionChatModIcon() : "";
                })
                .build());
        ChatPlaceholders.register(ChatPlaceholder.builder()
                .placeHolder("faction-title")
                .baseStringBuilder((s, r) -> {
                    FPlayer fPlayer = fPlayerCache.getByPlayer(s.getPlayer());
                    if (!fPlayer.hasFaction()) {
                        return "";
                    }
                    return fPlayer.getTitle();
                })
                .build());
        HoverInfo info = (sender, recipient) -> {
            FPlayer fSender = fPlayerCache.getByPlayer(sender.getPlayer());
            return FMessage.PLACEHOLDER_ALLIANCE_DISPLAY.message(fSender.getAllianceTag())
                    .appendNewline()
                    .append(FMessage.PLACEHOLDER_FACTION_DISPLAY.message(fSender.getFactionTag()))
                    .appendNewline()
                    .appendNewline();
        };
        // Replace default display name placeholder.
        ChatPlaceholder displayNamePlaceholder = ChatPlaceholders.get("player-display-name");
        if (displayNamePlaceholder != null) {
            displayNamePlaceholder.setBaseTextBuilder((s, r) -> Component.text()
                    .color(fPlayerCache.getByPlayer(s.getPlayer()).getAllianceColor())
                    .content(s.getLastName())
                    .build());
        }
        // Add hover info to default placeholders.
        registerPlaceholderInfo("player-name", info);
        registerPlaceholderInfo("player-display-name", info);
        registerPlaceholderInfo("recipient-name", info);
        registerPlaceholderInfo("recipient-display-name", info);
    }

    private void registerPlaceholderInfo(String placeholder, HoverInfo info) {
        ChatPlaceholder chatPlaceholder = ChatPlaceholders.get(placeholder);
        if (chatPlaceholder == null) {
            return;
        }
        HoverEventBuilder hoverBuilder = chatPlaceholder.getHoverEventBuilder();
        if (hoverBuilder == null) {
            return;
        }
        hoverBuilder.addHoverInfo(AERGIA_PLACEHOLDER_WEIGHT, info);
    }

    public void unregisterAergiaPlaceholders() {
        // Remove added UI components.
        for (FPlayer fPlayer : fPlayerCache.getCachedUsers()) {
            fPlayer.getUIBossBar().getCenter().remove(UIFactionsListener.FACTIONS_INFO_ID);
            fPlayer.getUIBossBar().getCenter().remove(UIFactionsListener.REGION_DISPLAY_ID);
            fPlayer.getUIActionBar().getCenter().remove(AutomatedChunkManager.ACTION_BAR_ID);
        }
        // Remove faction placeholders.
        ChatPlaceholders.unregister("faction");
        ChatPlaceholders.unregister("faction-rank");
        ChatPlaceholders.unregister("faction-title");
        // Remove hover info of default placeholders.
        unregisterPlaceholderInfo("player-name");
        unregisterPlaceholderInfo("player-display-name");
        unregisterPlaceholderInfo("recipient-name");
        unregisterPlaceholderInfo("recipient-display-name");
    }

    private void unregisterPlaceholderInfo(String placeholder) {
        ChatPlaceholder chatPlaceholder = ChatPlaceholders.get(placeholder);
        if (chatPlaceholder == null || chatPlaceholder.getHoverEventBuilder() == null) {
            return;
        }
        chatPlaceholder.getHoverEventBuilder().removeHoverInfo(AERGIA_PLACEHOLDER_WEIGHT);
    }

    /* Tasks */

    public void runBackupTask() {
        if (backupTask != null) {
            backupTask.cancel();
        }
        long interval = fConfig.getBackupInterval() * TickUtil.MINUTE;
        backupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::createBackup, interval, interval);
    }

    public void runSaveDataTask() {
        if (saveDataTask != null) {
            saveDataTask.cancel();
        }
        long interval = fConfig.getAutoSaveInterval() * TickUtil.MINUTE;
        saveDataTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::saveData, interval, interval);
    }

    /* Data storing */

    public void createBackup() {
        FLogger.DEBUG.log("Creating backup...");
        File backupDir = new File(BACKUPS, String.valueOf(System.currentTimeMillis()));
        FileUtil.copyDir(getDataFolder(), backupDir, "config.yml", "logger.yml");

        List<File> backupList = FileUtil.getFilesForFolder(backupDir);
        if (backupList.size() <= fConfig.getBackupsBeforeDeletion()) {
            return;
        }
        backupList.sort(Comparator.comparingLong(File::lastModified));
        Iterator<File> iterator = backupList.iterator();

        while (backupList.size() > fConfig.getBackupsBeforeDeletion() && iterator.hasNext()) {
            File current = iterator.next();
            FLogger.DEBUG.log("Deleting old backup: " + current.getName());
            current.delete();
            iterator.remove();
        }
    }

    public void saveData() {
        FLogger.DEBUG.log("Saving data...");
        allianceCache.saveAll();
        factionCache.saveAll();
        regionManager.saveAll();
        regionSchematicManager.saveAll();
        fPlayerCache.saveAll();
        warHistory.saveAll();
        warPhaseManager.saveData();
        buildSiteCache.shutdown();
        FLogger.save();
    }

    /* Getters and setters */

    public @NotNull Registry<String, Function<ConfigurationSection, Poll<?>>> getPollDeserializerRegistry() {
        return pollDeserializerRegistry;
    }

    public @NotNull FConfig getFConfig() {
        return fConfig;
    }

    public @NotNull AllianceCache getAllianceCache() {
        return allianceCache;
    }

    public @NotNull FactionCache getFactionCache() {
        return factionCache;
    }

    public @NotNull RegionManager getRegionManager() {
        return regionManager;
    }

    public @NotNull FPlayerCache getFPlayerCache() {
        return fPlayerCache;
    }

    public @NotNull FCommandCache getFCommandCache() {
        return fCommandCache;
    }

    public @NotNull BuildingManager getBuildingManager() {
        return buildingManager;
    }

    public @NotNull BuildSiteCache getBuildSiteCache() {
        return buildSiteCache;
    }

    public @NotNull RegionSchematicManager getRegionSchematicManager() {
        return regionSchematicManager;
    }

    public @NotNull WarHistory getWarHistory() {
        return warHistory;
    }

    public @NotNull WarPhaseManager getWarPhaseManager() {
        return warPhaseManager;
    }

    public @Nullable TaxManager getTaxManager() {
        return taxManager;
    }

    public @NotNull FPlayerListener getFPlayerListener() {
        return fPlayerListener;
    }

    public @NotNull UIFactionsListener getUIFactionsListener() {
        return uiFactionsListener;
    }

    public @NotNull WarListener getWarListener() {
        return warListener;
    }

    public boolean hasEconomyProvider() {
        return getEconomyProvider() != null;
    }

    /* Statics */

    public static @NotNull File getPlayerFile(@NotNull UUID uuid) {
        return new File(PLAYERS, uuid + ".yml");
    }

    public static @NotNull Factions get() {
        return instance;
    }

}
