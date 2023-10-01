package de.erethon.factions.command;

import de.erethon.aergia.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingManager;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BuildingTicketCommand extends FCommand {
    Factions plugin = Factions.get();
    BuildingManager buildingManager = plugin.getBuildingManager();

    public BuildingTicketCommand() {
        setCommand("buildingticket");
        setAliases("ticket", "tickets");
        setMinArgs(-1);
        setMaxArgs(-1);
        setPermissionFromName();
        setPlayerCommand(true);
        setConsoleCommand(false);
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(player);
        Region region = fPlayer.getLastRegion();
        List<String> tickets = new ArrayList<>();
        List<BuildSite> buildSites = plugin.getBuildingManager().getBuildingTickets();

        if (args.length == 1) {
            int i = 0;
            for (BuildSite site : buildSites) {
                String message = "<gray>" + i + ") <green><click:run_command:/fxl buildingticket tp " + i + "><hover:show_text:'<green>Region: " + site.getRegion().getName() +
                        "\n<green>Faction: " + site.getRegion().getOwner().getName() +
                        "'>" + site.getBuilding().getName() + "</click><reset>";
                tickets.add(message);
                i++;
            }
            MessageUtil.sendCenteredMessage(player, "&a&lBuilding-Tickets");
            for (String text : tickets) {
                MessageUtil.sendMessage(player, text);
            }
            MessageUtil.sendMessage(player, "\n&7&oKlicke einen Eintrag an, um dich zu teleportieren.");
            return;
        }

        if (args[1].equals("tp")) {
            Location tpLoc = buildSites.get(Integer.parseInt(args[2])).getInteractive();
            MessageUtil.sendMessage(player, "&aTeleportiere zum Gebäude...");
            player.teleportAsync(tpLoc);
            return;
        }

        if (args[1].equals("accept")) {
            BuildSite site = buildingManager.getBuildSite(player.getLocation(), region);
            if (site == null || site.isFinished()) {
                MessageUtil.sendMessage(player, "&cDu stehst nicht in einem unfertigen Gebäude.");
                return;
            }
            site.finishBuilding();
            plugin.getBuildingManager().getBuildingTickets().remove(site);
            MessageUtil.sendMessage(player, "&aGebäude akzeptiert.");
            MessageUtil.log(player.getName() + " accepted a BuildSite ticket for " + site.getBuilding().getName() + " in " + site.getRegion().getName());
            return;
        }

        if (args[1].equals("deny")) {
            BuildSite site = buildingManager.getBuildSite(player.getLocation(), region);
            if (site == null || site.isFinished()) {
                MessageUtil.sendMessage(player, "&cDu stehst nicht in einem unfertigen Gebäude.");
                return;
            }
            if (args.length < 3) {
                MessageUtil.sendMessage(player, "&cBitte gebe eine Nachricht an. /f ticket deny <Nachricht>");
                return;
            }
            String msg = "";
            int i = 2;
            for (String arg : args) {
                if (args[0] != arg && args[i - 1] != arg) {
                    if (!msg.isEmpty()) {
                        msg += " ";
                    }
                    msg += arg;
                }
            }
            site.setProblemMessage(msg);
            MessageUtil.sendMessage(player, "&aDas Gebäude wurde erfolgreich abgelehnt.\n&aNachricht: &7&o" + msg);
            MessageUtil.log(player.getName() + " denied a BuildSite ticket for " + site.getBuilding().getName() + " in " + site.getRegion().getName());
        }

    }
}
