package de.erethon.factions.command.logic;

import de.erethon.bedrock.command.ECommand;
import de.erethon.bedrock.command.ECommandCache;
import de.erethon.bedrock.plugin.EPlugin;
import de.erethon.factions.command.AdminCommand;
import de.erethon.factions.command.AllianceCommand;
import de.erethon.factions.command.AuthoriseCommand;
import de.erethon.factions.command.ClaimCommand;
import de.erethon.factions.command.CreateCommand;
import de.erethon.factions.command.DeclineCommand;
import de.erethon.factions.command.DescriptionCommand;
import de.erethon.factions.command.DisbandCommand;
import de.erethon.factions.command.HelpCommand;
import de.erethon.factions.command.InviteCommand;
import de.erethon.factions.command.JoinCommand;
import de.erethon.factions.command.KickCommand;
import de.erethon.factions.command.LeaveCommand;
import de.erethon.factions.command.LongNameCommand;
import de.erethon.factions.command.NameCommand;
import de.erethon.factions.command.RegionCommand;
import de.erethon.factions.command.ReloadCommand;
import de.erethon.factions.command.ShortNameCommand;
import de.erethon.factions.command.ShowCommand;
import de.erethon.factions.command.StatsCommand;
import de.erethon.factions.command.UnclaimCommand;
import de.erethon.factions.command.VersionCommand;
import de.erethon.factions.command.VoteCommand;

/**
 * @author Fyreum
 */
public class FCommandCache extends ECommandCache {

    public static final String LABEL = "factions";

    public FCommandCache(EPlugin plugin) {
        super(LABEL, plugin);
        addCommand(new AdminCommand());
        addCommand(new AllianceCommand());
        addCommand(new AuthoriseCommand());
        addCommand(new ClaimCommand());
        addCommand(new CreateCommand());
        addCommand(new DeclineCommand());
        addCommand(new DescriptionCommand());
        addCommand(new DisbandCommand());
        addCommand(new HelpCommand());
        addCommand(new InviteCommand());
        addCommand(new JoinCommand());
        addCommand(new KickCommand());
        addCommand(new LeaveCommand());
        addCommand(new LongNameCommand());
        addCommand(new NameCommand());
        addCommand(new RegionCommand());
        addCommand(new ReloadCommand());
        addCommand(new ShortNameCommand());
        addCommand(new ShowCommand());
        addCommand(new StatsCommand());
        addCommand(new UnclaimCommand());
        addCommand(new VersionCommand());
        addCommand(new VoteCommand());
    }

    @Override
    public void addCommand(ECommand command) {
        if (command.getHelp() == null) {
            command.setDefaultHelp();
        }
        super.addCommand(command);
    }
}
