package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.economy.FAccount;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class ClaimCommand extends FCommand {

    public ClaimCommand() {
        setCommand("claim");
        setMinMaxArgs(0, 1);
        setPermissionFromName();
        setFUsage(getCommand() + " ([region])");
        setDescription("Beansprucht eine Region");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        Faction faction = getFaction(fPlayer);
        Region region = args.length == 2 ? getClaimableRegion(args[1]) : getClaimableRegion(fPlayer);
        assureSameAlliance(region, fPlayer);

        FAccount fAccount = faction.getFAccount();
        double price = region.calculatePriceFor(faction);

        assure(fAccount.canAfford(price), FMessage.ERROR_FACTION_HAS_NOT_ENOUGH_MONEY, fAccount.getFormatted(price));
        fAccount.withdraw(price);

        region.setOwner(faction);
        faction.addRegion(region);
        region.setLastClaimingPrice(price);

        faction.sendMessage(FMessage.FACTION_INFO_REGION_CLAIMED.message(region.getName(), fAccount.getFormatted(price)));
    }
}
