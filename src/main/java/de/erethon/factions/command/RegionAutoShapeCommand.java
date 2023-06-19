package de.erethon.factions.command;

import de.erethon.bedrock.misc.EnumUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.ChunkOperation;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * @author Fyreum
 */
public class RegionAutoShapeCommand extends FCommand {

    public RegionAutoShapeCommand() {
        setCommand("shape");
        setAliases("s");
        setMinMaxArgs(1, 1);
        setPermissionFromName(RegionAutoCommand.PERM_PREFIX);
        setFUsage(RegionCommand.LABEL + " " + RegionAutoCommand.LABEL + " " + getCommand() + " [shape]");
        setDescription("Setzt die geometrische Form von automatischen Operationen");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        ChunkOperation.Shape shape = EnumUtil.getEnumIgnoreCase(ChunkOperation.Shape.class, args[1]);
        assure(shape != null, FMessage.ERROR_ACM_SHAPE_NOT_FOUND, args[1]);
        fPlayer.getAutomatedChunkManager().setShape(shape);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabList(ChunkOperation.Shape.values(), args[1]);
        }
        return null;
    }
}
