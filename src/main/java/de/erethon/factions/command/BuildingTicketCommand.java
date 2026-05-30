package de.erethon.factions.command;

import de.erethon.aergia.util.TeleportUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildingTicketDialogs;
import de.erethon.factions.building.BuildingManager;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.ClaimableRegion;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
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
        setAliases("bticket", "btickets");
        setMinArgs(0);
        setMaxArgs(99);
        setPermissionFromName();
        setPlayerCommand(true);
        setConsoleCommand(false);
        setUsage("/buildingticket");
        setRegisterSeparately(true);
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        FPlayer fPlayer = plugin.getFPlayerCache().getByPlayer(player);
        if (!(fPlayer.getCurrentRegion() instanceof ClaimableRegion claimableRegion)) {
            player.sendMessage(FMessage.ERROR_REGION_IS_NOT_CLAIMABLE.message());
            return;
        }
        ClaimableRegion region = claimableRegion;
        List<BuildSite> buildSites = plugin.getBuildingManager().getBuildingTickets();
        if (args.length <= 1) {
            BuildingTicketDialogs.showTickets(player, fPlayer, buildSites);
            return;
        }

        if (args[1].equals("tp")) {
            if (args.length < 3) {
                player.sendMessage(FMessage.BUILDING_TICKET_MISSING_ID.message());
                return;
            }
            int ticketId = Integer.parseInt(args[2]);
            if (ticketId < 0 || ticketId >= buildSites.size()) {
                player.sendMessage(FMessage.BUILDING_TICKET_INVALID.message());
                return;
            }
            Location tpLoc = buildSites.get(ticketId).getInteractive();
            TeleportUtil.teleportDirect(fPlayer, fPlayer.getEPlayer(), tpLoc);
            player.sendMessage(FMessage.BUILDING_TICKET_TELEPORTED.message());
            return;
        }

        if (args[1].equals("accept")) {
            Set<BuildSite> sites = buildingManager.getBuildSites(player.getLocation(), region);
            for (BuildSite site : sites) {
                if (site == null || site.isFinished()) {
                    player.sendMessage(FMessage.BUILDING_TICKET_INVALID.message());
                    continue;
                }
                if (!site.getMissingSections().isEmpty()) {
                    player.sendMessage(Component.translatable("factions.building.ticket.missingSections",
                            Component.text(site.getBuilding().getId()),
                            Component.text(FUtil.stringArrayToString(site.getMissingSections().toArray(new String[0])))));
                    return;
                }
                site.finishBuilding();
                plugin.getBuildingManager().getBuildingTickets().remove(site);
                player.sendMessage(FMessage.BUILDING_TICKET_ACCEPTED.message(Component.text(site.getBuilding().getId())));
                Factions.log(player.getName() + " accepted a BuildSite ticket for " + site.getBuilding().getId() + " in " + site.getRegion().getName());
                return;
            }
        }

        if (args[1].equals("deny")) {
            if (args.length < 3) {
                player.sendMessage(FMessage.BUILDING_TICKET_MISSING_ID.message());
                return;
            }
            if (args.length < 4) {
                player.sendMessage(FMessage.BUILDING_TICKET_MISSING_MESSAGE.message());
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
                player.sendMessage(FMessage.BUILDING_TICKET_INVALID.message());
                return;
            }
            String msg = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
            site.setProblemMessage(msg);
            player.sendMessage(FMessage.BUILDING_TICKET_DENIED.message(Component.text(site.getBuilding().getId()), Component.text(msg)));
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
