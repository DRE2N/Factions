package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.region.ClaimableRegion;
import de.erethon.factions.region.PvERegion;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.WarRegion;
import org.bukkit.command.CommandSender;

import java.util.Iterator;
import java.util.List;

/**
 * @author Fyreum
 */
public class RegionInfoCommand extends FCommand {

    public RegionInfoCommand() {
        setCommand("info");
        setAliases("i");
        setMinMaxArgs(0, 1);
        setConsoleCommand(true);
        setPermissionFromName(RegionCommand.LABEL);
        setFUsage(RegionCommand.LABEL + " " + getCommand() + " ([region])");
        setDescription("Zeigt Informationen der Region an");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Region region = args.length == 2 ? getRegion(args[1]) : getRegion(getFPlayer(sender));
        MessageUtil.sendCenteredMessage(sender, FMessage.CMD_REGION_INFO_HEADER.message(region.getName(true)));
        if (region instanceof PvERegion pveRegion) {
            sender.sendMessage(pveRegion.getFormattedInfluence());
        }
        sender.sendMessage(FMessage.CMD_REGION_INFO_ID.message(String.valueOf(region.getId())));
        sender.sendMessage(FMessage.CMD_REGION_INFO_TYPE.message(region.getType().getName()));
        if (region instanceof WarRegion warRegion) {
            sender.sendMessage(FMessage.CMD_REGION_INFO_WAR_VALUE.message(String.valueOf(warRegion.getRegionalWarTracker().getRegionValue())));
        }
        sender.sendMessage(FMessage.CMD_REGION_INFO_CHUNKS.message(String.valueOf(region.getChunks().size())));
        sender.sendMessage(FMessage.CMD_REGION_INFO_ADJACENT_REGIONS.message(getAdjacentRegions(region)));
        if (region.isOwned()) {
            sender.sendMessage(FMessage.CMD_REGION_INFO_OWNER.message(region.getOwner().asComponent(getFPlayer(sender))));
        } else if (region instanceof ClaimableRegion claimable) {
            sender.sendMessage(FMessage.CMD_REGION_INFO_PRICE.message(String.valueOf(claimable.calculatePriceFor(getFactionRaw(sender)))));
        }
        sender.sendMessage(FMessage.CMD_REGION_INFO_DESCRIPTION.message(region.getDisplayDescription()));
    }

    private String getAdjacentRegions(Region region) {
        if (region.getAdjacentRegions().isEmpty()) {
            return FMessage.GENERAL_NONE.getMessage();
        }
        StringBuilder sb = new StringBuilder();
        Iterator<Region> iterator = region.getAdjacentRegions().iterator();
        while (true) {
            sb.append(iterator.next().getName());
            if (!iterator.hasNext()) {
                return sb.toString();
            }
            sb.append(", ");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabRegions(args[1]);
        }
        return null;
    }
}
