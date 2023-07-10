package de.erethon.factions.command;

import de.erethon.bedrock.command.CommandFailedException;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.RegionType;
import de.erethon.factions.war.objective.CrystalWarObjective;
import de.erethon.factions.war.objective.OccupyWarObjective;
import de.erethon.factions.war.objective.WarObjective;
import de.erethon.factions.war.objective.WarObjectiveManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

/**
 * @author Fyreum
 */
public class ObjectiveCreateCommand extends FCommand {

    public static final List<String> OBJECTIVE_TYPES = List.of("crystal", "occupy");
    public static final List<String> CRYSTAL_OPTIONS = List.of("damagePerTick:", "maxHealth:", "radius:", "tickInterval:");
    public static final List<String> OCCUPY_OPTIONS = List.of("occupyDuration:", "occupiedInterval:", "radius:", "tickInterval:",
            "warProgressDecline:", "warProgressDeclineContested:", "warProgressPerOccupiedInterval:");

    public ObjectiveCreateCommand() {
        setCommand("create");
        setAliases("c");
        setMinMaxArgs(2, Integer.MAX_VALUE);
        setPermissionFromName(ObjectiveCommand.LABEL);
        setFUsage(ObjectiveCommand.LABEL + " " + getCommand() + " [type] [name] ([options...])");
        setDescription("Erstellt ein weiteres Kriegsziel");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        FPlayer fPlayer = getFPlayerRaw(sender);
        assure(getRegion(fPlayer).getType() == RegionType.WAR_ZONE, FMessage.ERROR_REGION_IS_NOT_A_WARZONE);
        WarObjectiveManager objectiveManager = plugin.getWarObjectiveManager();
        WarObjective objective = switch (args[1].toLowerCase()) {
            case "crystal" -> objectiveManager.instantiateObjective(args[2], cfg -> new CrystalWarObjective(applyArgs(fPlayer, args, cfg))).setAlliance(fPlayer.getAlliance());
            case "occupy" -> objectiveManager.instantiateObjective(args[2], cfg -> new OccupyWarObjective(applyArgs(fPlayer, args, cfg)));
            default -> throw new CommandFailedException(FMessage.ERROR_WAR_OBJECTIVE_TYPE_NOT_FOUND, args[1]);
        };
        objective.load();
        objective.activate();
        sender.sendMessage(FMessage.CMD_OBJECTIVE_CREATE_SUCCESS.message());
    }

    private ConfigurationSection applyArgs(FPlayer fPlayer, String[] args, ConfigurationSection config) {
        config.set("location", fPlayer.getPlayer().getLocation().serialize());
        if (args.length <= 3) {
            return config;
        }
        for (int i = 3; i < args.length; i++) {
            String[] split = args[i].split(":", 2);
            assure(split.length == 2, FMessage.ERROR_WAR_OBJECTIVE_WRONG_ARG_FORMAT);
            config.set(split[0], split[1]);
        }
        return config;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            return null;
        }
        if (args.length == 2) {
            return getTabList(OBJECTIVE_TYPES, args[1]);
        }
        String current = args[args.length - 1];
        return switch (args[1].toLowerCase()) {
            case "crystal" -> getTabList(CRYSTAL_OPTIONS, current);
            case "occupy" -> getTabList(OCCUPY_OPTIONS, current);
            default -> null;
        };
    }
}
