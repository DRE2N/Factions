package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Set;

/**
 * @author Fyreum
 */
public class ReloadCommand extends FCommand {

    private final Set<String> parts = Set.of("all", "caches", "configs", "messages");

    public ReloadCommand() {
        setCommand("reload");
        setAliases("rl");
        setMinMaxArgs(1, 1);
        setConsoleCommand(true);
        setPermissionFromName();
        setFUsage(getCommand() + " [part]");
        setDescription("Lädt Teile des Plugins neu");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        String part = args[1].toLowerCase();
        assure(parts.contains(part));

        switch (part) {
            case "all" -> {
                sender.sendMessage(FMessage.CMD_RELOAD_ALL_START.message());
                long start = System.currentTimeMillis();

                plugin.onDisable();
                plugin.loadCore();

                long end = System.currentTimeMillis();
                sender.sendMessage(FMessage.CMD_RELOAD_ALL_END.message(String.valueOf(end - start)));
            }
            case "caches" -> {
                plugin.initializeCaches();
                plugin.loadCaches();
                sender.sendMessage(FMessage.CMD_RELOAD_CACHES.message());
            }
            case "configs" -> {
                plugin.loadConfigs();
                sender.sendMessage(FMessage.CMD_RELOAD_CONFIGS.message());
            }
            case "messages" -> {
                plugin.loadFMessages();
                sender.sendMessage(FMessage.CMD_RELOAD_MESSAGES.message());
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabList(parts, args[1]);
        }
        return null;
    }
}
