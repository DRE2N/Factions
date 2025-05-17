package de.erethon.factions.command;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.factions.command.logic.FCommand;
import org.bukkit.command.CommandSender;

public class PaydayCommand extends FCommand {

    public PaydayCommand() {
        setCommand("payday");
        setDescription("Payday command for debugging mostly.");
        setUsage("/f payday");
        setAliases("pd");
        setPermissionFromName();
    }
    @Override
    public void onExecute(CommandSender sender, String[] args) {
        MessageUtil.sendMessage(sender, "Payday time.");
        plugin.getTaxManager().taxFactions();
    }
}
