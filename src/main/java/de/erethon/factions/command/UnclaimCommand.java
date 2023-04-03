package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class UnclaimCommand extends FCommand {

    public UnclaimCommand() {
        setCommand("unclaim");
        setMinMaxArgs(0, 1);
        setPermissionFromName();
        setFUsage(getCommand() + " ([region])");
        setDescription("Entfernt eine beanspruchte Region");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        Faction faction = getFaction(fPlayer);
        Region region = args.length == 2 ? getRegion(fPlayer) : getRegion(args[1]);

        assure(!faction.isCoreRegion(region), FMessage.ERROR_CANNOT_UNCLAIM_CORE_REGION);

        region.setOwner(null);
        faction.removeRegion(region);
    }
}
