package de.erethon.factions.command;

import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.region.Region;
import org.bukkit.command.CommandSender;

/**
 * @author Fyreum
 */
public class RegionDamageReductionCommand extends FCommand {

    public RegionDamageReductionCommand() {
        setCommand("damagereduction");
        setAliases("dr");
        setMinMaxArgs(1, 2);
        setConsoleCommand(true);
        setPermissionFromName(RegionCommand.LABEL);
        setFUsage(RegionCommand.LABEL + " " + getCommand() + " [reduction]");
        setDescription("Setzt die Schadensreduzierung einer Region");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Region region = args.length == 2 ? getRegion(getFPlayer(sender)) : getRegion(args[2]);
        double reduction = NumberUtil.parseDouble(args[1], -1);
        assure(reduction > 0 && reduction < 1, FMessage.ERROR_WRONG_DOUBLE_VALUE, "0.0", "1");
        region.setDamageReduction(reduction);
        sender.sendMessage(FMessage.CMD_REGION_DAMAGE_REDUCTION_SUCCESS.message(region.getName(), String.valueOf(reduction)));
    }
}
