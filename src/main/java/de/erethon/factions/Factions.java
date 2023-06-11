package de.erethon.factions;

import de.erethon.aergia.placeholder.ChatPlaceholder;
import de.erethon.aergia.placeholder.ChatPlaceholders;
import de.erethon.aergia.placeholder.HoverEventBuilder;
import de.erethon.aergia.placeholder.HoverInfo;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.compatibility.Internals;
import de.erethon.bedrock.misc.FileUtil;
import de.erethon.bedrock.plugin.EPlugin;
import de.erethon.bedrock.plugin.EPluginSettings;
import de.erethon.factions.alliance.AllianceCache;
import de.erethon.factions.building.BuildingManager;
import de.erethon.factions.command.logic.FCommandCache;
import de.erethon.factions.data.FConfig;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.faction.FactionCache;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.player.FPlayerCache;
import de.erethon.factions.player.FPlayerListener;
import de.erethon.factions.region.AutomatedChunkManager;
import de.erethon.factions.region.RegionManager;
import de.erethon.factions.ui.UIFactionsListener;
import de.erethon.factions.util.FLogger;
import de.erethon.factions.war.WarListener;
import de.erethon.factions.war.WarObjectiveManager;
import de.erethon.factions.war.WarPhaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.UUID;

public final class Factions extends EPlugin {

    private static Factions instance;

    /* Folders */
    public static File ALLIANCES;
    public static File BUILDINGS;
    public static File FACTIONS;
    public static File REGIONS;
    public static File PLAYERS;
    public static File WAR;

    /* Files */
    private File fLoggerFile;
    private File fConfigFile;
    private File warObjectiveManagerFile;
    private File warPhaseManagerFile;

    /* Configs */
    private FConfig fConfig;

    /* Caches */
    private AllianceCache allianceCache;
    private FactionCache factionCache;
    private RegionManager regionManager;
    private FPlayerCache fPlayerCache;
    private FCommandCache fCommandCache;

    /* Instances */
    private BuildingManager buildingManager;
    private WarObjectiveManager warObjectiveManager;
    private WarPhaseManager warPhaseManager;

    /* Listeners */
    private FPlayerListener fPlayerListener;
    private UIFactionsListener uiFactionsListener;
    private WarListener warListener;

    public Factions() {
        settings = EPluginSettings.builder()
                .internals(Internals.v1_19_R3.andHigher())
                .forcePaper(true)
                .economy(true)
                .build();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;
        loadCore();
        registerAergiaPlaceholders();
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTasks(this);
        for (FPlayer fPlayer : fPlayerCache.getCachedUsers()) {
            fPlayer.getUIBossBar().getCenter().remove(UIFactionsListener.REGION_DISPLAY_ID);
            fPlayer.getUIActionBar().getCenter().remove(AutomatedChunkManager.ACTION_BAR_ID);
        }
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
        loadWarObjectiveManager();
        loadWarPhaseManager();
        runTasks();
        loadCommands();
        registerListeners();
    }

    public void initFolders() {
        initFolder(getDataFolder());
        initFolder(ALLIANCES = new File(getDataFolder(), "alliances"));
        initFolder(BUILDINGS = new File(getDataFolder(), "buildings"));
        initFolder(FACTIONS = new File(getDataFolder(), "factions"));
        initFolder(REGIONS = new File(getDataFolder(), "regions"));
        initFolder(PLAYERS = new File(getDataFolder(), "players"));
        initFolder(WAR = new File(getDataFolder(), "war"));
    }

    public void initFiles() {
        fLoggerFile = new File(getDataFolder(), "logger.yml");
        fConfigFile = new File(getDataFolder(), "config.yml");
        warPhaseManagerFile = FileUtil.initFile(this, new File(WAR, "war.yml"), "defaults/war.yml");
        warObjectiveManagerFile = FileUtil.initFile(this, new File(getDataFolder(), "objectives.yml"), "defaults/objectives.yml");
    }

    public void loadFLogger() {
        FLogger.load(fLoggerFile);
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
        factionCache = new FactionCache(FACTIONS);
        regionManager = new RegionManager(REGIONS);
        fPlayerCache = new FPlayerCache(this);
    }

    public void loadCaches() {
        allianceCache.loadAll();
        factionCache.loadAll();
        regionManager.loadAll();
        fPlayerCache.loadAll();
    }

    public void loadWarObjectiveManager() {
        warObjectiveManager = new WarObjectiveManager(warObjectiveManagerFile);
        warObjectiveManager.load();
    }

    public void loadWarPhaseManager() {
        warPhaseManager = new WarPhaseManager(warPhaseManagerFile);
        warPhaseManager.load();
    }

    public void runTasks() {
        factionCache.runKickTask();
        warPhaseManager.updateCurrentStageTask();
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
        ChatPlaceholders.register(ChatPlaceholder.builder()
                .placeHolder("faction")
                .baseBuilder((s, r) -> {
                    FPlayer fSender = fPlayerCache.getByPlayer(s.getPlayer());
                    String faction = fSender.hasFaction() ? fSender.getFaction().getDisplayShortName() : FMessage.GENERAL_LONER.getMessage();
                    return Component.text()
                            .color(fSender.hasAlliance() ? fSender.getAlliance().getColor() : NamedTextColor.WHITE)
                            .append(MessageUtil.parse("<dark_gray>[</dark_gray>" + faction + "<dark_gray>]</dark_gray>"))
                            .build();
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
                    return fPlayer.isAdminRaw() ? "<dark_gray>**" : fPlayer.isMod() ? "<green>*" : "";
                })
                .build());
        HoverInfo info = (sender, recipient) -> {
            FPlayer fSender = fPlayerCache.getByPlayer(sender.getPlayer());
            return FMessage.PLACEHOLDER_ALLIANCE_DISPLAY.message(fSender.getAllianceTag())
                    .appendNewline()
                    .append(FMessage.PLACEHOLDER_FACTION_DISPLAY.message(fSender.getFactionTag()))
                    .appendNewline();
        };
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
        hoverBuilder.addHoverInfo(2, info);
    }

    public void saveData() {
        allianceCache.saveAll();
        factionCache.saveAll();
        regionManager.saveAll();
        fPlayerCache.saveAll();
        warObjectiveManager.saveAll();
        warPhaseManager.saveData();
        FLogger.save();
    }

    /* Getters and setters */

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

    public @NotNull WarObjectiveManager getWarObjectiveManager() {
        return warObjectiveManager;
    }

    public @NotNull WarPhaseManager getWarPhaseManager() {
        return warPhaseManager;
    }

    public @NotNull FPlayerListener getFPlayerListener() {
        return fPlayerListener;
    }

    public @NotNull UIFactionsListener getUiFactionsListener() {
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
