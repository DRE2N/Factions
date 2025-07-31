package de.erethon.factions.util;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Supplier;

/**
 * @author Fyreum
 */
public enum FLogger {

    DEBUG(false),
    ALLIANCE(false),
    BUILDING(false),
    ECONOMY(true),
    FACTION(true),
    REGION(false),
    PLAYER(true),
    INFO(true),
    WAR(true),
    WARN(true, NamedTextColor.GOLD),
    WEB(true), // todo: -> false
    ERROR(true, NamedTextColor.RED);

    private static File configFile;
    private static PrintWriter debugWriter;
    private static YamlConfiguration config;

    private final boolean defaultEnabled;
    private boolean enabled;
    private TextColor color;

    FLogger(boolean defaultEnabled) {
        this(defaultEnabled, null);
    }

    FLogger(boolean defaultEnabled, TextColor color) {
        this.defaultEnabled = defaultEnabled;
        this.enabled = defaultEnabled;
        this.color = color;
    }

    /* Logging */

    public void log(String msg) {
        if (!isEnabled()) {
            return;
        }
        debugWriter.println(msg);
        if (color != null) {
            Factions.log(Component.text().color(color).content("[" + name() + "] " + msg).build());
            sendToAdminsInGame(Component.text().color(color).content("[" + name() + "] " + msg).build());
        } else {
            Factions.log("[" + name() + "] " + msg);
            sendToAdminsInGame(Component.text("[" + name() + "] ").append(Component.text(msg)));
        }
    }

    public void log(Component msg) {
        log(() -> msg);
    }

    public void log(Supplier<Component> msg) {
        if (!isEnabled()) {
            return;
        }
        Component comp = msg.get();
        debugWriter.println("[" + name() + "] " + MessageUtil.serializePlain(comp));
        if (color != null) {
            Factions.log(Component.text().color(color).content("[" + name() + "] ").append(comp).build());
            sendToAdminsInGame(Component.text().color(color).content("[" + name() + "] ").append(comp).build());
        } else {
            Factions.log(Component.text("[" + name() + "] ").append(comp));
            sendToAdminsInGame(Component.text("[" + name() + "] ").append(comp));
        }
    }

    private void sendToAdminsInGame(Component msg) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("factions.admin") || player.isOp()) {
                MessageUtil.sendMessage(player, msg);
            }
        }
    }

    /* Getters and setters */

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public @Nullable TextColor getColor() {
        return color;
    }

    public void setColor(@Nullable TextColor color) {
        this.color = color;
    }

    /* Statics */

    public static void load(@NotNull File configFile, @NotNull File debugFile) {
        FLogger.configFile = configFile;
        FLogger.config = YamlConfiguration.loadConfiguration(configFile);
        for (FLogger logger : FLogger.values()) {
            logger.setEnabled(config.getBoolean(logger.name(), logger.isEnabled()));
        }
        try {
            FLogger.debugWriter = new PrintWriter(debugFile);
        } catch (IOException e) {
            FLogger.ERROR.log("Couldn't create debug file: " + e.getMessage());
        }
    }

    public static void save() {
        DEBUG.log("Saving logger settings...");
        for (FLogger logger : FLogger.values()) {
            config.set(logger.name(), logger.isEnabled());
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            ERROR.log("Couldn't save logger settings: ");
            e.printStackTrace();
        }
        DEBUG.log("Saved logger settings.");
    }

    public static void closeWriter() {
        debugWriter.close();
    }
}
