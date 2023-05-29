package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.poll.Poll;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * @author Fyreum
 */
public class VoteCommand extends FCommand {

    public VoteCommand() {
        setCommand("vote");
        setMinMaxArgs(1, 1);
        setPermissionFromName();
        setFUsage(getCommand() + " [poll]");
        setDescription("Öffnet das Menü zum abstimmen");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayer(sender);
        Poll<?> poll = fPlayer.getParticipativePolls().get(args[1]);
        assure(poll != null, FMessage.ERROR_POLL_NOT_FOUND, args[1]);
        poll.show(fPlayer.getPlayer(), 0);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabPolls(sender, args[1]);
        }
        return null;
    }
}
