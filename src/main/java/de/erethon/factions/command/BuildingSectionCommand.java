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
        setFUsage("/f building section create|delete|list|rename");
        setHelp("Manage building sections");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            displayHelp(sender);
            return;
        }
        Player player = (Player) sender;
        FPlayer fPlayer = getFPlayer(player);
        switch (args[3].toLowerCase()) {
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
        if (args.length < 2) {
            fPlayer.sendMessage("/f building section create <name>");
            return;
        }
        if (buildSite.getSections().stream().anyMatch(section -> section.name().equalsIgnoreCase(args[1]))) {
            fPlayer.sendMessage(Component.translatable("factions.error.sectionExists", Component.text(args[1])));
            return;
        }
        BuildSiteSection section = new BuildSiteSection(args[1], fPlayer.getPos1(), fPlayer.getPos2(), false);
        buildSite.getSections().add(section);
        fPlayer.sendMessage(Component.translatable("factions.cmd.building.section.created", Component.text(args[1])));
    }

    private void deleteSection(FPlayer fPlayer, String[] args) {
        BuildSite buildSite = getSite(fPlayer);
        if (buildSite == null) {
            return;
        }
        if (args.length < 2) {
            fPlayer.sendMessage("/f building section delete <name>");
            return;
        }
        BuildSiteSection section = buildSite.getSections().stream().filter(s -> s.name().equalsIgnoreCase(args[1])).findFirst().orElse(null);
        if (section == null) {
            fPlayer.sendMessage(Component.translatable("factions.error.sectionNotFound", Component.text(args[1])));
            return;
        }
        buildSite.getSections().remove(section);
        fPlayer.sendMessage(Component.translatable("factions.cmd.building.section.deleted", Component.text(args[1])));
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
        if (args.length < 3) {
            fPlayer.sendMessage("/f building section rename <oldName> <newName>");
            return;
        }
        BuildSiteSection section = buildSite.getSections().stream().filter(s -> s.name().equalsIgnoreCase(args[1])).findFirst().orElse(null);
        if (section == null) {
            fPlayer.sendMessage(Component.translatable("factions.error.sectionNotFound", Component.text(args[1])));
            return;
        }
        if (buildSite.getSections().stream().anyMatch(s -> s.name().equalsIgnoreCase(args[2]))) {
            fPlayer.sendMessage(Component.translatable("factions.error.sectionExists", Component.text(args[2])));
            return;
        }
        section = new BuildSiteSection(args[2], section.corner1(), section.corner2(), section.protectedSection());
        buildSite.getSections().removeIf(s -> s.name().equalsIgnoreCase(args[1]));
        buildSite.getSections().add(section);
        fPlayer.sendMessage(Component.translatable("factions.cmd.building.section.renamed", Component.text(args[1]), Component.text(args[2])));
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
