package de.erethon.factions.command;

import de.erethon.bedrock.misc.ComponentConverter;
import de.erethon.bedrock.misc.InfoUtil;
import de.erethon.bedrock.misc.SimpleDateUtil;
import de.erethon.factions.alliance.Alliance;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.war.WarHistory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.Date;

/**
 * @author Fyreum
 */
public class WarHistoryCommand extends FCommand {

    public WarHistoryCommand() {
        setCommand("warhistory");
        setAliases("wh", "history");
        setMinMaxArgs(0, Integer.MAX_VALUE);
        setConsoleCommand(true);
        setPermissionFromName();
        setFUsage(getCommand());
        setDescription("Zeigt die Ergebnisse früherer Kriege");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        ComponentConverter<WarHistory.Entry> converter = entry -> {
            Alliance winner = plugin.getAllianceCache().getById(entry.getWinner());
            Date endDate = new Date(entry.getEndDate());
            Component hover = FMessage.CMD_WAR_HISTORY_WINNER.message(getAllianceComponent(entry.getWinner()))
                    .appendNewline()
                    .append(FMessage.CMD_WAR_HISTORY_ENDE_DATE.message(SimpleDateUtil.ddMMMMyyyyhhmmss(endDate)))
                    .appendNewline();

            for (int id : entry.getAllianceScores().keySet()) {
                hover = hover.appendNewline()
                        .append(getAllianceComponent(id))
                        .append(FMessage.CMD_WAR_HISTORY_ENTRY.message(String.valueOf(entry.getAllianceScores().get(id))));
            }
            return Component.text().content(SimpleDateUtil.ddMMyyyy(endDate)).color(winner == null ? NamedTextColor.WHITE : winner.getColor()).build()
                    .hoverEvent(HoverEvent.showText(hover));
        };
        InfoUtil.sendListedInfo(sender, plugin.getWarHistory().getEntries(), "Kriegshistorie", converter);
    }

    private Component getAllianceComponent(int id) {
        Alliance alliance = plugin.getAllianceCache().getById(id);
        return alliance == null ? FMessage.GENERAL_UNKNOWN.message() : alliance.getColoredName();
    }
}
