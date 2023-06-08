package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class CreateCommand extends FCommand {

    public CreateCommand() {
        setCommand("create");
        setAliases("c");
        setMinMaxArgs(1, 1);
        setPermissionFromName();
        setFUsage(getCommand() + " [name]");
        setDescription("Erstellt eine neue Fraktion");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        assurePlayerIsFactionless(fPlayer);
        assurePlayerHasAlliance(fPlayer);

        Region region = getClaimableRegion(fPlayer);
        assureSameAlliance(region, fPlayer);

        int maximumChars = plugin.getFConfig().getMaximumNameChars();
        assure(args[1].length() <= maximumChars, FMessage.ERROR_TEXT_IS_TOO_LONG, String.valueOf(maximumChars));

        if (plugin.hasEconomyProvider()) {
            Economy economy = plugin.getEconomyProvider();
            double regionPrice = region.calculatePriceFor(null);
            assure(economy.has(fPlayer.getPlayer(), regionPrice), FMessage.ERROR_NOT_ENOUGH_MONEY, economy.format(regionPrice));
            plugin.getFactionCache().create(fPlayer, region, args[1]);
            economy.withdrawPlayer(fPlayer.getPlayer(), regionPrice);
        } else {
            plugin.getFactionCache().create(fPlayer, region, args[1]);
        }
    }
}
