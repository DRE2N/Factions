package de.erethon.factions.command;

import de.erethon.aergia.util.BroadcastUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Fyreum
 */
public class AdminCommand extends FCommand {

    public AdminCommand() {
        setCommand("admin");
        setAliases("leader");
        setMinMaxArgs(1, 1);
        setConsoleCommand(true);
        setPermissionFromName();
        setFUsage(getCommand() + " [player]");
        setDescription("Überträgt die Führungsposition einer Fraktion");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fTarget = getFPlayerInFaction(args[1]);
        Faction faction = fTarget.getFaction();
        fAssure(!fTarget.isAdmin(), () -> FMessage.ERROR_TARGET_IS_ALREADY_ADMIN.getMessage(fTarget.getLastName(), faction.getName()));
        faction.setAdmin(fTarget);
        BroadcastUtil.broadcast(FMessage.FACTION_INFO_ADMIN_CHANGED.message(sender.getName(), fTarget.getLastName(), faction.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            return null;
        }
        if (args.length == 2) {
            FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(player);
            Faction faction = fPlayer.getFaction();
            if (faction == null || fPlayer.isBypassRaw()) {
                return getTabPlayers(args[1]);
            }
            List<String> completes = new ArrayList<>();
            for (FPlayer member : faction.getMembers()) {
                if (member.getLastName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completes.add(member.getLastName());
                }
            }
            return completes;
        }
        return null;
    }
}
