package de.erethon.factions.command;

import de.erethon.aergia.bedrock.misc.EnumUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.war.WarPhase;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * @author Fyreum
 */
public class DebugWarPhaseCommand extends FCommand {

    public DebugWarPhaseCommand() {
        setCommand("warphase");
        setConsoleCommand(true);
        setMinMaxArgs(0, 1);
        setPermissionFromName();
        setFUsage(getCommand() + " [phase]");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        if (args[0].equalsIgnoreCase("none")) {
            assure(plugin.getWarPhaseManager().isDebugMode(), FMessage.ERROR_DEBUG_MODE_NOT_ENABLED);
            plugin.getWarPhaseManager().disableDebugMode();
            return;
        }
        WarPhase warPhase = EnumUtil.getEnumIgnoreCase(WarPhase.class, args[0]);
        assure(warPhase != null, FMessage.ERROR_WAR_PHASE_NOT_FOUND);
        plugin.getWarPhaseManager().debugWarPhase(warPhase);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> completes = getTabList(WarPhase.values(), args[0]);
            if ("none".startsWith(args[0].toLowerCase())) {
                completes.add("none");
            }
            return completes;
        }
        return null;
    }
}
