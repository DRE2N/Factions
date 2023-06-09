package de.erethon.factions.util;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * @author Fyreum
 */
public enum FLogger {

    DEBUG(false),
    ALLIANCE(false),
    BUILDING(false),
    FACTION(false),
    REGION(false),
    PLAYER(true),
    INFO(true),
    WAR(true),
    WARN(true, NamedTextColor.GOLD),
    ERROR(true, NamedTextColor.RED);

    private static File file;
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
        if (color != null) {
            MessageUtil.log(Factions.get(), Component.text().color(color).content("[" + name() + "] " + msg).build());
        } else {
            MessageUtil.log(Factions.get(), "[" + name() + "] " + msg);
        }
    }

    public void log(Component msg) {
        log(() -> msg);
    }

    public void log(Supplier<Component> msg) {
        if (!isEnabled()) {
            return;
        }
        if (color != null) {
            MessageUtil.log(Factions.get(), Component.text().color(color).content("[" + name() + "] ").append(msg.get()).build());
        } else {
            MessageUtil.log(Factions.get(), Component.text("[" + name() + "] ").append(msg.get()));
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

    public TextColor getColor() {
        return color;
    }

    public void setColor(TextColor color) {
        this.color = color;
    }

    /* Statics */

    public static void load(File file) {
        FLogger.file = file;
        FLogger.config = YamlConfiguration.loadConfiguration(file);
        for (FLogger logger : FLogger.values()) {
            logger.setEnabled(config.getBoolean(logger.name(), logger.isEnabled()));
        }
    }

    public static void save() {
        DEBUG.log("Saving logger settings...");
        for (FLogger logger : FLogger.values()) {
            config.set(logger.name(), logger.isEnabled());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            ERROR.log("Couldn't save logger settings: ");
            e.printStackTrace();
        }
        DEBUG.log("Saved logger settings.");
    }
}
