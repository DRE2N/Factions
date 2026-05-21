package de.erethon.factions.building;

import de.erethon.aergia.util.TeleportUtil;
import de.erethon.factions.Factions;
import de.erethon.factions.dialog.FDialogFactory;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.util.FUtil;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class BuildingTicketDialogs {

    private BuildingTicketDialogs() {
    }

    public static void showTickets(@NotNull Player player, @NotNull FPlayer fPlayer, @NotNull List<BuildSite> tickets) {
        if (tickets.isEmpty()) {
            FDialogFactory.showNotice(player, Component.translatable("factions.building.ticket.dialog.title"),
                    List.of(Component.translatable("factions.building.ticket.empty")));
            return;
        }
        List<Component> body = List.of(Component.translatable("factions.building.ticket.dialog.summary", Component.text(tickets.size())));
        List<ActionButton> actions = new ArrayList<>();
        for (int i = 0; i < tickets.size(); i++) {
            BuildSite site = tickets.get(i);
            int index = i;
            actions.add(FDialogFactory.button(
                    Component.translatable("factions.building.ticket.dialog.button.ticket",
                            Component.text(index),
                            Component.translatable("factions.building.buildings." + site.getBuilding().getId() + ".name"),
                            Component.text(site.getRegion().getName())),
                    Component.translatable("factions.building.ticket.dialog.button.ticket_tooltip"),
                    DialogAction.customClick((response, audience) -> {
                        if (audience instanceof Player clickedPlayer) {
                            showTicket(clickedPlayer, fPlayer, site);
                        }
                    }, ClickCallback.Options.builder().build())));
        }
        actions.add(FDialogFactory.button(Component.translatable("factions.dialog.button.close"), null, null));
        FDialogFactory.showMultiAction(player, Component.translatable("factions.building.ticket.dialog.title"), body, actions);
    }

    public static void showTicket(@NotNull Player player, @NotNull FPlayer fPlayer, @NotNull BuildSite site) {
        List<Component> body = new ArrayList<>();
        body.add(Component.translatable("factions.building.ticket.dialog.building",
                Component.translatable("factions.building.buildings." + site.getBuilding().getId() + ".name")));
        body.add(Component.translatable("factions.building.ticket.dialog.region", Component.text(site.getRegion().getName())));
        body.add(Component.translatable("factions.building.ticket.dialog.faction",
                Component.text(site.getRegion().getOwner() == null ? "-" : site.getRegion().getOwner().getName())));
        body.add(Component.translatable("factions.building.ticket.dialog.state",
                Component.translatable("factions.building.state." + site.getState().name().toLowerCase())));
        if (!site.getMissingSections().isEmpty()) {
            body.add(Component.translatable("factions.building.ticket.missingSections",
                    Component.text(site.getBuilding().getId()),
                    Component.text(FUtil.stringArrayToString(site.getMissingSections().toArray(new String[0])))));
        }
        List<ActionButton> actions = new ArrayList<>();
        actions.add(FDialogFactory.button(Component.translatable("factions.building.ticket.dialog.button.teleport"), null,
                DialogAction.customClick((response, audience) -> {
                    TeleportUtil.teleportDirect(fPlayer, fPlayer.getEPlayer(), site.getInteractive());
                    player.sendMessage(Component.translatable("factions.building.ticket.teleported"));
                }, ClickCallback.Options.builder().build())));
        actions.add(FDialogFactory.button(Component.translatable("factions.building.ticket.dialog.button.approve"), null,
                DialogAction.customClick((response, audience) -> approve(player, site), ClickCallback.Options.builder().build())));
        actions.add(FDialogFactory.button(Component.translatable("factions.building.ticket.dialog.button.deny"), null,
                DialogAction.customClick((response, audience) -> showDeny(player, site), ClickCallback.Options.builder().build())));
        actions.add(FDialogFactory.button(Component.translatable("factions.dialog.button.close"), null, null));
        FDialogFactory.showMultiAction(player, Component.translatable("factions.building.ticket.dialog.detail_title"), body, actions);
    }

    private static void showDeny(@NotNull Player player, @NotNull BuildSite site) {
        FDialogFactory.showTextInput(player,
                Component.translatable("factions.building.ticket.dialog.deny.title"),
                List.of(Component.translatable("factions.building.ticket.dialog.deny.body")),
                "reason",
                Component.translatable("factions.building.ticket.dialog.deny.input"),
                Component.empty(),
                256,
                Component.translatable("factions.building.ticket.dialog.button.deny"),
                (reason, clickedPlayer) -> {
                    site.setProblemMessage(reason);
                    clickedPlayer.sendMessage(Component.translatable("factions.building.ticket.denied",
                            Component.text(site.getBuilding().getId()), Component.text(reason)));
                });
    }

    private static void approve(@NotNull Player player, @NotNull BuildSite site) {
        if (!site.getMissingSections().isEmpty()) {
            player.sendMessage(Component.translatable("factions.building.ticket.missingSections",
                    Component.text(site.getBuilding().getId()),
                    Component.text(FUtil.stringArrayToString(site.getMissingSections().toArray(new String[0])))));
            return;
        }
        site.finishBuilding();
        Factions.get().getBuildingManager().getBuildingTickets().remove(site);
        player.sendMessage(Component.translatable("factions.building.ticket.accepted", Component.text(site.getBuilding().getId())));
    }
}
