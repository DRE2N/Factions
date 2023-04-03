package de.erethon.factions.command;

import de.erethon.aergia.util.DateUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * @author Fyreum
 */
public class JoinCommand extends FCommand {

    public JoinCommand() {
        setCommand("join");
        setAliases("j");
        setMinMaxArgs(1, 1);
        setPermissionFromName();
        setFUsage(getCommand() + " [faction]");
        setDescription("Tritt einer Fraktion bei");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Faction faction = getFaction(args[1]);
        FPlayer fPlayer = getFPlayerRaw(sender);

        assurePlayerIsFactionless(fPlayer);

        if (!fPlayer.isBypassRaw()) {
            assure(!faction.isOpen() && !faction.isInvitedPlayer(fPlayer), FMessage.CMD_JOIN_NOT_INVITED);

            long cooldownExpirationDate = fPlayer.getLastFactionJoinDate() + plugin.getFConfig().getFactionJoinCooldown();
            assure(cooldownExpirationDate >= System.currentTimeMillis(), FMessage.CMD_JOIN_ON_COOLDOWN, DateUtil.formatDateDiff(cooldownExpirationDate));
        }
        faction.playerJoin(fPlayer);
        sender.sendMessage(FMessage.CMD_JOIN_SUCCESS.message(faction.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabFactions(args[1]);
        }
        return null;
    }
}
