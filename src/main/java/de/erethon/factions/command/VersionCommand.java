package de.erethon.factions.command;

import de.erethon.bedrock.misc.JavaUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;

/**
 * @author Fyreum
 */
public class VersionCommand extends FCommand {

    public VersionCommand() {
        setCommand("version");
        setAliases("v");
        setConsoleCommand(true);
        setPermissionFromName();
        setFUsage(getCommand());
        setDescription("Zeigt Informationen über das Plugin an");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        PluginDescriptionFile description = plugin.getDescription();
        sender.sendMessage(FMessage.CMD_VERSION_HEADER.message(description.getName(), description.getVersion()));
        sender.sendMessage(FMessage.CMD_VERSION_AUTHORS.message(JavaUtil.toString(description.getAuthors())));
        sender.sendMessage(FMessage.CMD_VERSION_STATUS.message(
                String.valueOf(plugin.getFPlayerCache().getCachedUsersAmount()),
                String.valueOf(plugin.getRegionManager().getCachedRegionsAmount()),
                String.valueOf(plugin.getFactionCache().getCache().size()),
                String.valueOf(plugin.getAllianceCache().getCache().size())
        ));
    }
}
