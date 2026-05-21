package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.ClaimableRegion;
import de.erethon.factions.util.FException;
import de.erethon.tyche.EconomyService;
import de.erethon.tyche.models.OwnerType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.UUID;

/**
 * @author Fyreum
 */
public class CreateCommand extends FCommand {

    private final HashMap<UUID, String> pendingCreations = new HashMap<>();

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
        UUID playerId = fPlayer.getUniqueId();

        if (args[1].equalsIgnoreCase("cancel")) {
            if (!pendingCreations.containsKey(playerId)) {
                fPlayer.sendMessage(Component.translatable("factions.cmd.create.noPending"));
                return;
            }
            pendingCreations.remove(playerId);
            fPlayer.sendMessage(Component.translatable("factions.cmd.create.cancel"));
            return;
        }

        if (args[1].equalsIgnoreCase("confirm")) {
            if (!pendingCreations.containsKey(playerId)) {
                fPlayer.sendMessage(Component.translatable("factions.cmd.create.noPending"));
                return;
            }
            assurePlayerIsFactionless(fPlayer);
            assurePlayerHasAlliance(fPlayer);

            ClaimableRegion region = getClaimableRegion(fPlayer);
            assureSameAlliance(region, fPlayer);
            String factionName = pendingCreations.get(playerId);
            if (plugin.getFactionCache().getByName(factionName) != null) {
                pendingCreations.remove(playerId);
                fPlayer.sendMessage(FMessage.ERROR_NAME_IN_USE.message(factionName));
                return;
            }
            if (plugin.hasEconomyProvider()) {
                EconomyService economy = plugin.getEconomyService();
                double regionPrice = region.calculatePriceFor(null);
                long price = Math.round(regionPrice);
                Component formattedPrice = Component.text(price + " herone");

                economy.getBalance(fPlayer.getUniqueId(), OwnerType.PLAYER, "herone")
                        .thenAccept(playerMoney -> {
                            if (playerMoney < price) {
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    fPlayer.sendMessage(FMessage.ERROR_NOT_ENOUGH_MONEY.message(formattedPrice));
                                    pendingCreations.remove(playerId);
                                });
                                return;
                            }
                            economy.withdraw(fPlayer.getUniqueId(), OwnerType.PLAYER, "herone", price, "Faction Creation", fPlayer.getUniqueId())
                                    .thenRun(() -> {
                                        Bukkit.getScheduler().runTask(plugin, () -> {
                                            try {
                                                if (plugin.getFactionCache().getByName(factionName) != null) {
                                                    fPlayer.sendMessage(FMessage.ERROR_NAME_IN_USE.message(factionName));
                                                    refundFactionCreation(economy, fPlayer, price, "Faction name became unavailable before creation");
                                                    return;
                                                }
                                                plugin.getLogger().info("Creating faction '" + factionName + "' for player " + fPlayer.getName() +
                                                        " with region '" + region.getName() + "' at price " + price);
                                                plugin.getFactionCache().create(fPlayer, region, factionName);
                                                region.setLastClaimingPrice(price);
                                            } catch (FException e) {
                                                fPlayer.sendMessage(e.getPlayerMessage());
                                                refundFactionCreation(economy, fPlayer, price, "Faction creation failed");
                                            } finally {
                                                pendingCreations.remove(playerId);
                                            }
                                        });
                                    })
                                    .exceptionally(throwable -> {
                                        plugin.getLogger().severe("Failed to withdraw money for faction creation: " + throwable.getMessage());
                                        Bukkit.getScheduler().runTask(plugin, () -> {
                                            fPlayer.sendMessage(Component.translatable("factions.cmd.create.withdrawFailed", Component.text(throwable.getMessage())));
                                            pendingCreations.remove(playerId);
                                        });
                                        return null;
                                    });
                        })
                        .exceptionally(throwable -> {
                            plugin.getLogger().severe("Failed to get balance for faction creation: " + throwable.getMessage());
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                fPlayer.sendMessage(Component.translatable("factions.cmd.create.balanceFailed", Component.text(throwable.getMessage())));
                                pendingCreations.remove(playerId);
                            });
                            return null;
                        });
            } else {
                try {
                    plugin.getFactionCache().create(fPlayer, region, factionName);
                } finally {
                    pendingCreations.remove(playerId);
                }
            }
            return;
        }

        assurePlayerIsFactionless(fPlayer);
        assurePlayerHasAlliance(fPlayer);

        ClaimableRegion region = getClaimableRegion(fPlayer);
        assureSameAlliance(region, fPlayer);
        if (pendingCreations.containsKey(playerId)) {
            fPlayer.sendMessage(Component.translatable("factions.cmd.create.alreadyPending"));
            return;
        }

        int maximumChars = plugin.getFConfig().getMaximumNameChars();
        assure(args[1].length() <= maximumChars, FMessage.ERROR_TEXT_IS_TOO_LONG, String.valueOf(maximumChars));
        assure(plugin.getFactionCache().getByName(args[1]) == null, FMessage.ERROR_NAME_IN_USE, args[1]);
        pendingCreations.put(playerId, args[1]);
        plugin.getLogger().info("Player " + fPlayer.getName() + " is creating faction '" + args[1] + "'");
        fPlayer.sendMessage(Component.translatable("factions.cmd.create.notice"));
        fPlayer.sendMessage(Component.translatable("factions.cmd.create.confirm", Component.text(args[1])));
    }

    private void refundFactionCreation(EconomyService economy, FPlayer fPlayer, long price, String reason) {
        economy.deposit(fPlayer.getUniqueId(), OwnerType.PLAYER, "herone", price, reason, fPlayer.getUniqueId())
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Failed to refund faction creation money for " + fPlayer.getName() + ": " + throwable.getMessage());
                    return null;
                });
    }
}
