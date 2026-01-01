package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.aergia.util.TeleportUtil;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingManager;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.ClaimableRegion;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BuildingTicketCommand extends FCommand {
    Factions plugin = Factions.get();
    BuildingManager buildingManager = plugin.getBuildingManager();

    public BuildingTicketCommand() {
        setCommand("buildingticket");
        setAliases("ticket", "tickets");
        setMinArgs(0);
        setMaxArgs(99);
        setPermissionFromName();
        setPlayerCommand(true);
        setConsoleCommand(false);
        setUsage("/f buildingticket");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(player);
        if (!(fPlayer.getCurrentRegion() instanceof ClaimableRegion claimableRegion)) {
            MessageUtil.sendMessage(player, FMessage.ERROR_REGION_IS_NOT_CLAIMABLE.message());
            return;
        }
        ClaimableRegion region = claimableRegion;
        List<String> tickets = new ArrayList<>();
        List<BuildSite> buildSites = plugin.getBuildingManager().getBuildingTickets();
        if (args.length == 1) {
            int i = 0;
            for (BuildSite site : buildSites) {
                String message = "<gray>" + i + ") <green><click:run_command:/f buildingticket tp " + i + "><hover:show_text:'<green>Region: " + site.getRegion().getName() +
                        "\n<green>Faction: " + site.getRegion().getOwner().getName() +
                        "'>" + site.getBuilding().getId() + "</click><reset>";
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
            TeleportUtil.teleportDirect(fPlayer, fPlayer.getEPlayer(), tpLoc);
            return;
        }

        if (args[1].equals("accept")) {
            Set<BuildSite> sites = buildingManager.getBuildSites(player.getLocation(), region);
            for (BuildSite site : sites) {
                if (site == null || site.isFinished()) {
                    MessageUtil.sendMessage(player, "&cÜbersprungen: " + site.getBuilding().getId() + " ist bereits fertig.");
                    continue;
                }
                if (!site.getMissingSections().isEmpty()) {
                    MessageUtil.sendMessage(player, "&cFolgendem Gebäude fehlen Sektionen: " + site.getBuilding().getId());
                    MessageUtil.sendMessage(player, "&cEs fehlen folgende: " + FUtil.stringArrayToString(site.getMissingSections().toArray(new String[0])));
                    return;
                }
                site.finishBuilding();
                plugin.getBuildingManager().getBuildingTickets().remove(site);
                MessageUtil.sendMessage(player, "&aGebäude " + site.getBuilding().getId() + " erfolgreich angenommen.");
                Factions.log(player.getName() + " accepted a BuildSite ticket for " + site.getBuilding().getId() + " in " + site.getRegion().getName());
                return;
            }
        }

        if (args[1].equals("deny")) {
            if (args.length < 3) {
                MessageUtil.sendMessage(player, "&cBitte gebe eine UUID an. /f ticket deny <UUID> <Nachricht>");
                return;
            }
            if (args.length < 4) {
                MessageUtil.sendMessage(player, "&cBitte gebe eine Nachricht an. /f ticket deny <UUID> <Nachricht>");
                return;
            }
            Set<BuildSite> sites = buildingManager.getBuildSites(player.getLocation(), region);
            BuildSite site = null;
            for (BuildSite s : sites) {
                if (s.getUUIDString().equals(args[2])) {
                    site = s;
                    break;
                }
            }
            if (site == null || site.isFinished()) {
                MessageUtil.sendMessage(player, "&cDu stehst nicht in einem unfertigen Gebäude.");
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
            Factions.log(player.getName() + " denied a BuildSite ticket for " + site.getBuilding().getId() + " in " + site.getRegion().getName());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return List.of("tp", "accept", "deny");
        }
        if (args[1].equals("deny")) {
            Player player = (Player) sender;
            FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(player);
            Region region = fPlayer.getCurrentRegion();
            List<String> uuids = new ArrayList<>();
            if (!(region instanceof ClaimableRegion claimableRegion)) {
                return uuids;
            }
            for (BuildSite site : buildingManager.getBuildSites(player.getLocation(), claimableRegion)) {
                uuids.add(site.getUUIDString());
            }
            return uuids;
        }
        return super.onTabComplete(sender, args);
    }
}
