package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.misc.InfoUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.region.Region;
import de.erethon.factions.util.FUtil;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class RegionStructureListCommand extends FCommand {

    public RegionStructureListCommand() {
        setCommand("list");
        setAliases("l");
        setMinMaxArgs(0, 1);
        setConsoleCommand(true);
        setPermissionFromName(RegionStructureCommand.CMD_PREFIX);
        setFUsage(RegionStructureCommand.CMD_PREFIX + " " + getCommand());
        setDescription("Listet alle Regionsstrukturen auf");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Region region = args.length == 2 ? getRegion(args[1]) : getRegion(getFPlayer(sender));
        assure(!region.getStructures().isEmpty(), FMessage.CMD_REGION_STRUCTURE_LIST_EMPTY, region.getName());
        InfoUtil.sendListedInfo(sender, region.getStructures(), s -> MessageUtil.parse("<gold>" + FUtil.toString(s.getCenterPosition()))
                .hoverEvent(HoverEvent.showText(FMessage.CMD_REGION_STRUCTURE_LIST_TYPE.message(s.getClass().getSimpleName())))
                .clickEvent(ClickEvent.suggestCommand("/tp " + FUtil.toString(s.getCenterPosition(), " "))));
    }
}
