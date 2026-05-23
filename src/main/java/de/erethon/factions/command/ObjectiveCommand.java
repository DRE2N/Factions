package de.erethon.factions.command;

import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import de.erethon.factions.region.WarRegion;
import de.erethon.factions.util.FPermissionUtil;
import de.erethon.factions.util.FBroadcastUtil;
import de.erethon.factions.war.RegionalWarTracker;
import de.erethon.factions.war.WarObjectiveTier;
import de.erethon.factions.war.WarObjectiveUpgrade;
import de.erethon.factions.war.structure.CrystalWarStructure;
import de.erethon.factions.war.structure.WarCastleStructure;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class ObjectiveCommand extends FCommand {

    private static final List<String> SUBCOMMANDS = List.of("info", "claim", "upgrade", "repair", "waypoint", "tier", "debug");

    public ObjectiveCommand() {
        setCommand("objective");
        setAliases("obj");
        setMinMaxArgs(0, Integer.MAX_VALUE);
        setPermissionFromName();
        setFUsage(getCommand() + " [info|claim|upgrade|repair|waypoint|tier|debug]");
        setDescription("War objective commands");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        String sub = args.length >= 2 ? args[1].toLowerCase() : "info";
        switch (sub) {
            case "info" -> sendInfo(sender, getWarRegion(sender, args.length >= 3 ? args[2] : null));
            case "claim" -> claim(sender, getWarRegion(sender, args.length >= 3 ? args[2] : null));
            case "upgrade" -> upgrade(sender, args, getWarRegion(sender, args.length >= 4 ? args[3] : null));
            case "repair" -> repair(sender, getWarRegion(sender, args.length >= 3 ? args[2] : null));
            case "waypoint" -> waypoint(sender, getWarRegion(sender, args.length >= 3 ? args[2] : null));
            case "tier" -> tier(sender, args, getWarRegion(sender, args.length >= 4 ? args[3] : null));
            case "debug" -> debug(sender, args, getWarRegion(sender, args.length >= 4 ? args[3] : null));
            default -> displayHelp(sender);
        }
    }

    private WarRegion getWarRegion(CommandSender sender, String arg) {
        Region region = arg == null ? getRegion(getFPlayer(sender)) : getRegion(arg);
        assure(region instanceof WarRegion, FMessage.ERROR_REGION_IS_NOT_A_WARZONE);
        return (WarRegion) region;
    }

    private void sendInfo(CommandSender sender, WarRegion region) {
        RegionalWarTracker tracker = region.getRegionalWarTracker();
        Faction operator = tracker.getOperatingFaction();
        sender.sendMessage(FMessage.WAR_OBJECTIVE_INFO_NAME.message(region.getName()));
        sender.sendMessage(region.getAlliance() == null
                ? FMessage.WAR_OBJECTIVE_INFO_ALLIANCE_NONE.message()
                : FMessage.WAR_OBJECTIVE_INFO_ALLIANCE.message(region.getAlliance().getName()));
        sender.sendMessage(operator == null
                ? FMessage.WAR_OBJECTIVE_INFO_OPERATOR_NONE.message()
                : FMessage.WAR_OBJECTIVE_INFO_OPERATOR.message(operator.getName()));
        sender.sendMessage(FMessage.WAR_OBJECTIVE_INFO_TIER.message(tracker.getTier().name()));
        sender.sendMessage(FMessage.WAR_OBJECTIVE_INFO_POINT_VALUE.message(String.valueOf(tracker.getRegionValue())));
        sender.sendMessage(tracker.isCaptureUnlocked()
                ? FMessage.WAR_OBJECTIVE_INFO_CAPTURE_UNLOCKED_YES.message()
                : FMessage.WAR_OBJECTIVE_INFO_CAPTURE_UNLOCKED_NO.message());
        sender.sendMessage(tracker.isContested()
                ? FMessage.WAR_OBJECTIVE_INFO_CONTESTED_YES.message(formatPercent(tracker.getHighestCaptureProgressPercent()))
                : FMessage.WAR_OBJECTIVE_INFO_CONTESTED_NO.message(formatPercent(tracker.getHighestCaptureProgressPercent())));
        sender.sendMessage(waypointState(tracker));
        sender.sendMessage(FMessage.WAR_OBJECTIVE_INFO_REPAIR_SUPPLIES.message(String.valueOf(tracker.getRepairSupplies())));
        sender.sendMessage(FMessage.WAR_OBJECTIVE_INFO_UPGRADES.message(Arrays.toString(Arrays.stream(WarObjectiveUpgrade.values()).filter(upgrade -> tracker.getUpgradeLevel(upgrade) > 0).map(upgrade -> upgrade.name() + "=" + tracker.getUpgradeLevel(upgrade)).toArray())));
        sender.sendMessage(FMessage.WAR_OBJECTIVE_INFO_CRYSTALS.message(String.valueOf(region.getStructures(CrystalWarStructure.class).values().stream().filter(c -> !c.isDepleted()).count())));
    }

    private void claim(CommandSender sender, WarRegion region) {
        FPlayer fPlayer = getFPlayer(sender);
        assurePlayerHasFaction(fPlayer);
        assure(region.getAlliance() == fPlayer.getAlliance(), FMessage.ERROR_PERMITLESS_ALLIANCE);
        RegionalWarTracker tracker = region.getRegionalWarTracker();
        assure(tracker.getOperatingFaction() == null, FMessage.ERROR_REGION_ALREADY_OCCUPIED, region.getName());
        tracker.setOperatingFaction(fPlayer.getFaction());
        region.saveData();
        sender.sendMessage(FMessage.WAR_OBJECTIVE_CLAIMED.message(region.getName()));
    }

    private void upgrade(CommandSender sender, String[] args, WarRegion region) {
        assure(args.length >= 3, FMessage.ERROR_WAR_OBJECTIVE_TYPE_NOT_FOUND, "");
        FPlayer fPlayer = getFPlayer(sender);
        assurePlayerHasFaction(fPlayer);
        WarObjectiveUpgrade upgrade = WarObjectiveUpgrade.getByName(args[2]);
        assure(upgrade != null, FMessage.ERROR_WAR_OBJECTIVE_TYPE_NOT_FOUND, args[2]);
        double cost = region.getRegionalWarTracker().getModifiedUpgradeCost(fPlayer.getAlliance(), upgrade, region.getRegionalWarTracker().getUpgradeLevel(upgrade) + 1);
        boolean success = region.getRegionalWarTracker().buyUpgrade(fPlayer.getFaction(), upgrade, fPlayer.getPlayer());
        assure(success, FMessage.ERROR_FACTION_HAS_NOT_ENOUGH_MONEY, String.valueOf(cost));
        region.saveData();
        sender.sendMessage(FMessage.WAR_OBJECTIVE_UPGRADE_BOUGHT.message(upgrade.name(), region.getName()));
        if (upgrade == WarObjectiveUpgrade.WAYPOINT) {
            FBroadcastUtil.broadcastWar(FMessage.WAR_OBJECTIVE_WAYPOINT_ENABLED, region.getName());
        }
    }

    private void repair(CommandSender sender, WarRegion region) {
        FPlayer fPlayer = getFPlayer(sender);
        assurePlayerHasFaction(fPlayer);
        assure(region.getAlliance() == fPlayer.getAlliance(), FMessage.ERROR_PERMITLESS_ALLIANCE);
        assure(region.getRegionalWarTracker().getRepairSupplies() > 0, FMessage.ERROR_NOT_ENOUGH_MONEY, "repair supplies");
        WarCastleStructure castle = region.getStructures(WarCastleStructure.class).values().stream().findFirst().orElse(null);
        assure(castle != null, FMessage.ERROR_WAR_OBJECTIVE_NOT_FOUND, "castle");
        castle.startRepair();
        region.getRegionalWarTracker().addContribution(fPlayer.getFaction(), "repairs_started", 1);
        region.saveData();
        sender.sendMessage(FMessage.WAR_OBJECTIVE_REPAIR_STARTED.message(region.getName()));
    }

    private Component waypointState(RegionalWarTracker tracker) {
        if (tracker.getWaypointSpawn() == null) {
            return FMessage.WAR_OBJECTIVE_INFO_WAYPOINT_NOT_SET.message();
        }
        if (tracker.getUpgradeLevel(WarObjectiveUpgrade.WAYPOINT) > 0) {
            return FMessage.WAR_OBJECTIVE_INFO_WAYPOINT_ENABLED.message();
        }
        return FMessage.WAR_OBJECTIVE_INFO_WAYPOINT_LOCKED.message();
    }

    private String formatPercent(double percent) {
        return String.format(java.util.Locale.ROOT, "%.1f", percent);
    }

    private void waypoint(CommandSender sender, WarRegion region) {
        assure(FPermissionUtil.isBypass(sender), FMessage.ERROR_NO_PERMISSION);
        Player player = getFPlayer(sender).getPlayer();
        region.getRegionalWarTracker().setWaypointSpawn(player.getLocation());
        region.saveData();
        sender.sendMessage(Component.text("Set waypoint spawn for " + region.getName() + "."));
    }

    private void tier(CommandSender sender, String[] args, WarRegion region) {
        assure(FPermissionUtil.isBypass(sender), FMessage.ERROR_NO_PERMISSION);
        assure(args.length >= 3, FMessage.ERROR_WAR_OBJECTIVE_TYPE_NOT_FOUND, "");
        WarObjectiveTier tier = WarObjectiveTier.getByName(args[2]);
        assure(tier != null, FMessage.ERROR_WAR_OBJECTIVE_TYPE_NOT_FOUND, args[2]);
        region.getRegionalWarTracker().setTier(tier);
        region.saveData();
        sender.sendMessage(Component.text("Set objective tier for " + region.getName() + " to " + tier.name() + "."));
    }

    private void debug(CommandSender sender, String[] args, WarRegion region) {
        assure(FPermissionUtil.isBypass(sender), FMessage.ERROR_NO_PERMISSION);
        assure(args.length >= 3, FMessage.ERROR_WAR_OBJECTIVE_TYPE_NOT_FOUND, "");
        switch (args[2].toLowerCase()) {
            case "scoretick" -> plugin.getWar().getScore().rebuildPotentialPoints();
            case "deplete" -> region.getStructures(CrystalWarStructure.class).values().forEach(c -> c.forceDeplete(null));
            case "recharge" -> region.getStructures(CrystalWarStructure.class).values().forEach(CrystalWarStructure::forceRecharge);
            case "reset" -> region.getRegionalWarTracker().clearCycleState();
            default -> {
                displayHelp(sender);
                return;
            }
        }
        region.saveData();
        long crystals = region.getStructures(CrystalWarStructure.class).size();
        long charged = region.getStructures(CrystalWarStructure.class).values().stream().filter(c -> !c.isDepleted()).count();
        sender.sendMessage(Component.text("Objective debug action applied. Capture unlocked: "
                + region.getRegionalWarTracker().isCaptureUnlocked()
                + " (" + (crystals - charged) + "/" + crystals + " crystals depleted)."));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return getTabList(SUBCOMMANDS, args[1]);
        }
        if (args.length == 3 && "upgrade".equalsIgnoreCase(args[1])) {
            return getTabList(Arrays.stream(WarObjectiveUpgrade.values()).map(Enum::name).toList(), args[2]);
        }
        if (args.length == 3 && "tier".equalsIgnoreCase(args[1])) {
            return getTabList(Arrays.stream(WarObjectiveTier.values()).map(Enum::name).toList(), args[2]);
        }
        if (args.length == 3 && "debug".equalsIgnoreCase(args[1])) {
            return getTabList(List.of("scoretick", "deplete", "recharge", "reset"), args[2]);
        }
        if ((args.length == 3 && List.of("info", "claim", "repair", "waypoint").contains(args[1].toLowerCase()))
                || (args.length == 4 && List.of("upgrade", "tier", "debug").contains(args[1].toLowerCase()))) {
            if (sender instanceof Player player) {
                return getTabRegions(player, args[args.length - 1]);
            }
            return getTabRegions(args[args.length - 1]);
        }
        return null;
    }
}
