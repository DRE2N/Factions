package de.erethon.factions.util;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fyreum
 */
public class FPermissionUtil {

    public static final String BYPASS_PERM = "factions.bypass";

    public static boolean isBypass(@NotNull CommandSender sender) {
        return sender.hasPermission(BYPASS_PERM);
    }

}
