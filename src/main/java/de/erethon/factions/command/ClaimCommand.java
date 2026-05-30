package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.economy.FAccount;
import de.erethon.factions.economy.FEconomy;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.ClaimableRegion;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.WarRegion;
import de.erethon.factions.war.WarPhase;
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
        Region rawRegion = args.length == 2 ? getRegion(args[1]) : getRegion(fPlayer);
        assure(rawRegion.isClaimable(), FMessage.ERROR_REGION_IS_NOT_CLAIMABLE);
        if (rawRegion instanceof WarRegion warRegion) {
            claimWarRegion(sender, fPlayer, faction, warRegion);
            return;
        }
        assureRegionIsClaimable(rawRegion);
        assureRegionIsUnowned(rawRegion);
        ClaimableRegion region = (ClaimableRegion) rawRegion;
        assureSameAlliance(region, fPlayer);

        FAccount fAccount = faction.getFAccount();
        double price = region.calculatePriceFor(faction);

        assure(fAccount.canAfford(price), FMessage.ERROR_FACTION_HAS_NOT_ENOUGH_MONEY, fAccount.getFormatted(price));
        fAccount.withdraw(price, FEconomy.TAX_CURRENCY, "Claimed region " + region.getName() + " by " + fPlayer.getLastName(), fPlayer.getUniqueId());

        region.setOwner(faction);
        faction.addRegion(region);
        region.setLastClaimingPrice(price);

        faction.sendMessage(FMessage.FACTION_INFO_REGION_CLAIMED.message(region.getName(), fAccount.getFormatted(price)));
    }

    private void claimWarRegion(CommandSender sender, FPlayer fPlayer, Faction faction, WarRegion region) {
        assure(plugin.getCurrentWarPhase() == WarPhase.PEACE, FMessage.ERROR_WAR_REQUIRES_PEACE);
        assure(region.getAlliance() == fPlayer.getAlliance(), FMessage.ERROR_PERMITLESS_ALLIANCE);
        assure(region.getRegionalWarTracker().getOperatingFaction() == null, FMessage.ERROR_REGION_ALREADY_CLAIMED);
        region.getRegionalWarTracker().setOperatingFaction(faction);
        region.saveData();
        sender.sendMessage(FMessage.WAR_OBJECTIVE_CLAIMED.message(region.getName()));
    }
}
