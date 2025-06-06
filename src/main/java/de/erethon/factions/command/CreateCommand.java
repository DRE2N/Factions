package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.CommandSender;

import java.util.HashMap;

/**
 * @author Fyreum
 */
public class CreateCommand extends FCommand {

    private final HashMap<FPlayer, String> pendingCreations = new HashMap<>();

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
        if (args[1].equalsIgnoreCase("confirm")) {
            assure(pendingCreations.containsKey(fPlayer), FMessage.ERROR_FACTION_NOT_FOUND);
            String factionName = pendingCreations.get(fPlayer);
            if (plugin.hasEconomyProvider()) {
                Economy economy = plugin.getEconomyProvider();
                double regionPrice = region.calculatePriceFor(null);
                assure(economy.has(fPlayer.getPlayer(), regionPrice), FMessage.ERROR_NOT_ENOUGH_MONEY, economy.format(regionPrice));
                plugin.getFactionCache().create(fPlayer, region, factionName);
                economy.withdrawPlayer(fPlayer.getPlayer(), regionPrice);
                region.setLastClaimingPrice(regionPrice);
            } else {
                plugin.getFactionCache().create(fPlayer, region, factionName);
            }
            pendingCreations.remove(fPlayer);
            return;
        }
        if (args[1].equalsIgnoreCase("cancel")) {
            assure(pendingCreations.containsKey(fPlayer), FMessage.ERROR_FACTION_NOT_FOUND);
            pendingCreations.remove(fPlayer);
            fPlayer.sendMessage(Component.translatable("factions.cmd.create.cancel"));
            return;
        }
        if (pendingCreations.containsKey(fPlayer)) {
            fPlayer.sendMessage(Component.translatable("factions.cmd.create.alreadyPending"));
            return;
        }

        int maximumChars = plugin.getFConfig().getMaximumNameChars();
        assure(args[1].length() <= maximumChars, FMessage.ERROR_TEXT_IS_TOO_LONG, String.valueOf(maximumChars));
        pendingCreations.put(fPlayer, args[1]);
        fPlayer.sendMessage(Component.translatable("factions.cmd.create.notice"));
        fPlayer.sendMessage(Component.translatable("factions.cmd.create.confirm", args[1]));
    }
}
