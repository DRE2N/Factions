package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * @author Fyreum
 */
public class InviteCommand extends FCommand {

    public InviteCommand() {
        setCommand("invite");
        setAliases("i");
        setMinMaxArgs(1, 2);
        setPermissionFromName();
        setFUsage(getCommand() + " [player] ([faction])");
        setDescription("Lädt den Spieler in die Fraktion ein");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayer(args[1]);
        Faction faction;
        if (args.length == 3) {
            faction = getFaction(args[2]);
            assure(faction.isPrivileged(sender), FMessage.ERROR_NO_PERMISSION);
        } else {
            faction = getFaction(fPlayer);
        }
        double maxPlayers = faction.getAttribute("max_players").getValue();
        int members = faction.getMembers().size();
        if (maxPlayers <= members) {
            MessageUtil.sendTranslatable(sender, "factions.error.max_players", Component.text(members), Component.text(maxPlayers));
            return;
        }
        faction.invitePlayer(fPlayer);
        sender.sendMessage(FMessage.CMD_INVITE_SUCCESS.message(fPlayer.getLastName(), faction.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabPlayers(args[1]);
        }
        if (args.length == 3) {
            return getTabFactions(sender, args[2]);
        }
        return null;
    }
}
