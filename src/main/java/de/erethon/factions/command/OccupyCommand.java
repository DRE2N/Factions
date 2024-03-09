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
public class OccupyCommand extends FCommand {

    public OccupyCommand() {
        setCommand("occupy");
        setAliases("o");
        setPermissionFromName();
        setFUsage(getCommand());
        setDescription("Setzt den Namen der Fraktion");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayer(sender);
        assurePlayerHasFaction(fPlayer);
        Faction faction = fPlayer.getFaction();
        assureSenderHasModPerms(sender, faction);
        Region region = getRegion(fPlayer);
        assure(fPlayer.getAlliance().getTemporaryRegions().contains(region), FMessage.ERROR_REGION_IS_NOT_OCCUPIABLE, region.getName());
        assure(region.getOwner() == null, FMessage.ERROR_REGION_ALREADY_OCCUPIED, region.getName());
        assure(faction.getOccupiedRegion() == null, FMessage.ERROR_FACTION_ALREADY_OCCUPIED_REGION, faction.getOccupiedRegion().getName());
        region.setOwner(faction);
        faction.setOccupiedRegion(region);
    }
}
