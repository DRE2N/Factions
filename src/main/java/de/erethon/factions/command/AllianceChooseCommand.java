package de.erethon.factions.command;

import de.erethon.aergia.util.DateUtil;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * @author Fyreum
 */
public class AllianceChooseCommand extends FCommand {

    public AllianceChooseCommand() {
        setCommand("choose");
        setAliases("c");
        setMinMaxArgs(1, 1);
        setPermissionFromName();
        setFUsage(AllianceCommand.LABLE + " " + getCommand() + " [alliance]");
        setDescription("Wählt eine Allianz aus");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        Alliance alliance = getAlliance(args[1]);

        if (!fPlayer.isBypassRaw()) {
            long cooldownExpirationDate = fPlayer.getLastAllianceJoinDate() + plugin.getFConfig().getAllianceJoinCooldown();
            assure(cooldownExpirationDate >= System.currentTimeMillis(), FMessage.CMD_ALLIANCE_CHOOSE_ON_COOLDOWN, DateUtil.formatDateDiff(cooldownExpirationDate));
        }
        assure(fPlayer.setAlliance(alliance), FMessage.ERROR_CANNOT_CHOOSE_ALLIANCE);
        fPlayer.setLastAllianceJoinDate(System.currentTimeMillis());
        sender.sendMessage(FMessage.CMD_ALLIANCE_CHOOSE_SUCCESS.message(alliance.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabAlliances(args[1]);
        }
        return null;
    }
}
