package de.erethon.factions.command;

import de.erethon.factions.Factions;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildSiteSection;
import de.erethon.factions.command.logic.FCommand;
import de.erethon.factions.data.FMessage;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BuildingSectionCommand extends FCommand {

    public BuildingSectionCommand() {
        setCommand("section");
        setAliases("s");
        setFUsage("/f buildingadmin section <create|delete|list|rename>");
        setHelp("Manage building sections");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            displayHelp(sender);
            return;
        }
        Player player = (Player) sender;
        FPlayer fPlayer = getFPlayer(player);
        switch (args[2].toLowerCase()) {
            case "create" -> {
                createSection(fPlayer, args);
            }
            case "delete" -> {
                deleteSection(fPlayer, args);
            }
            case "list" -> {
                listSections(fPlayer, args);
            }
            case "rename" -> {
                renameSection(fPlayer, args);
            }
            default -> displayHelp(sender);
        }
    }

    private void createSection(FPlayer fPlayer, String[] args) {
        if (fPlayer.getPos1() == null || fPlayer.getPos2() == null) {
            fPlayer.sendMessage(FMessage.ERROR_NO_SELECTION.message());
            return;
        }
        BuildSite buildSite = getSite(fPlayer);
        if (buildSite == null) {
            return;
        }
        if (args.length < 4) {
            fPlayer.sendMessage("/f buildingadmin section create <name>");
            return;
        }
        String name = args[3];
        if (buildSite.getSections().stream().anyMatch(section -> section.name().equalsIgnoreCase(name))) {
            fPlayer.sendMessage(Component.translatable("factions.error.sectionExists", Component.text(name)));
            return;
        }
        BuildSiteSection section = new BuildSiteSection(name, fPlayer.getPos1(), fPlayer.getPos2(), false);
        buildSite.getSections().add(section);
        fPlayer.sendMessage(Component.translatable("factions.cmd.building.section.created", Component.text(name)));
    }

    private void deleteSection(FPlayer fPlayer, String[] args) {
        BuildSite buildSite = getSite(fPlayer);
        if (buildSite == null) {
            return;
        }
        if (args.length < 4) {
            fPlayer.sendMessage("/f buildingadmin section delete <name>");
            return;
        }
        String name = args[3];
        BuildSiteSection section = buildSite.getSections().stream().filter(s -> s.name().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (section == null) {
            fPlayer.sendMessage(Component.translatable("factions.error.sectionNotFound", Component.text(name)));
            return;
        }
        buildSite.getSections().remove(section);
        fPlayer.sendMessage(Component.translatable("factions.cmd.building.section.deleted", Component.text(name)));
    }

    private void listSections(FPlayer fPlayer, String[] args) {
        BuildSite buildSite = getSite(fPlayer);
        if (buildSite == null) {
            return;
        }
        fPlayer.sendMessage(Component.translatable("factions.cmd.building.section.list", Component.text(buildSite.getSections().size())));
        for (BuildSiteSection section : buildSite.getSections()) {
            Component hover = Component.text("Pos1: ", NamedTextColor.GRAY).append(Component.text("X: " + section.corner1().x() + " Y: " + section.corner1().y() + " Z: " + section.corner1().z(), NamedTextColor.GOLD));
            hover = hover.append(Component.newline()).append(Component.text("Pos2: ", NamedTextColor.GRAY)).append(Component.text("X: " + section.corner2().x() + " Y: " + section.corner2().y() + " Z: " + section.corner2().z(), NamedTextColor.GOLD));
            fPlayer.sendMessage(Component.text(section.name()).hoverEvent(HoverEvent.showText(hover)));
        }
    }

    private void renameSection(FPlayer fPlayer, String[] args) {
        BuildSite buildSite = getSite(fPlayer);
        if (buildSite == null) {
            return;
        }
        if (args.length < 5) {
            fPlayer.sendMessage("/f buildingadmin section rename <oldName> <newName>");
            return;
        }
        String oldName = args[3];
        String newName = args[4];
        BuildSiteSection section = buildSite.getSections().stream().filter(s -> s.name().equalsIgnoreCase(oldName)).findFirst().orElse(null);
        if (section == null) {
            fPlayer.sendMessage(Component.translatable("factions.error.sectionNotFound", Component.text(oldName)));
            return;
        }
        if (buildSite.getSections().stream().anyMatch(s -> s.name().equalsIgnoreCase(newName))) {
            fPlayer.sendMessage(Component.translatable("factions.error.sectionExists", Component.text(newName)));
            return;
        }
        section = new BuildSiteSection(newName, section.corner1(), section.corner2(), section.protectedSection());
        buildSite.getSections().removeIf(s -> s.name().equalsIgnoreCase(oldName));
        buildSite.getSections().add(section);
        fPlayer.sendMessage(Component.translatable("factions.cmd.building.section.renamed", Component.text(oldName), Component.text(newName)));
    }

    private BuildSite getSite(FPlayer fPlayer) {
        Region region = fPlayer.getCurrentRegion();
        if (region == null) {
            fPlayer.sendMessage(FMessage.ERROR_REGION_NOT_FOUND.message());
            return null;
        }
        BuildSite buildSite = null;
        for (BuildSite site : plugin.getBuildSiteCache().get(fPlayer.getPlayer().getChunk().getChunkKey())) {
            if (site.isInBuildSite(fPlayer.getPlayer())) {
                buildSite = site;
                break;
            }
        }
        if (buildSite == null) {
            fPlayer.sendMessage(Component.translatable("factions.error.noBuildSite"));
            return null;
        }
        return buildSite;
    }

}
