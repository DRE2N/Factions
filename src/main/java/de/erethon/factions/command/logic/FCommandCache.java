package de.erethon.factions.command.logic;

import de.erethon.bedrock.command.ECommand;
import de.erethon.bedrock.command.ECommandCache;
import de.erethon.bedrock.plugin.EPlugin;
import de.erethon.factions.command.*;

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
        addCommand(new BuildingCommand());
        addCommand(new BuildingTicketCommand());
        addCommand(new ClaimCommand());
        addCommand(new CreateCommand());
        addCommand(new CreateWarFlagCommand());
        addCommand(new DeclineCommand());
        addCommand(new DescriptionCommand());
        addCommand(new DisbandCommand());
        addCommand(new FlagCommand());
        addCommand(new HelpCommand());
        addCommand(new HomeCommand());
        addCommand(new InviteCommand());
        addCommand(new JoinCommand());
        addCommand(new KickCommand());
        addCommand(new LeaveCommand());
        addCommand(new LongNameCommand());
        addCommand(new NameCommand());
        addCommand(new Pos1Command());
        addCommand(new Pos2Command());
        addCommand(new RegionCommand());
        addCommand(new ReloadCommand());
        addCommand(new SetHomeCommand());
        addCommand(new ShortNameCommand());
        addCommand(new ShowCommand());
        addCommand(new StatsCommand());
        addCommand(new UnclaimCommand());
        addCommand(new VersionCommand());
        addCommand(new VoteCommand());
        addCommand(new WarHistoryCommand());
    }

    @Override
    public void addCommand(ECommand command) {
        if (command.getHelp() == null) {
            command.setDefaultHelp();
        }
        super.addCommand(command);
    }
}
