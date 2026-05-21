package de.erethon.factions.building;

import de.erethon.factions.dialog.FDialogFactory;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.ClaimableRegion;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class BuildingDialogs {

    private BuildingDialogs() {
    }

    public static void showBuildingDetails(@NotNull Player player, @NotNull FPlayer fPlayer, @NotNull Faction faction,
                                           @NotNull ClaimableRegion region, @NotNull Building building) {
        Location location = player.getLocation();
        Set<RequirementFail> fails = building.checkRequirements(player, faction, location);
        List<Component> body = new ArrayList<>();
        appendDescription(body, building);
        appendRequirements(body, faction, building, fails);
        appendEffects(body, building);

        List<ActionButton> actions = new ArrayList<>();
        if (fails.isEmpty()) {
            actions.add(FDialogFactory.button(
                    Component.translatable("factions.building.dialog.button.place"),
                    Component.translatable("factions.building.dialog.button.place_tooltip"),
                    DialogAction.customClick((response, audience) -> {
                        if (audience instanceof Player clickedPlayer) {
                            new BuildSitePlacer(building, fPlayer, region, faction);
                        }
                    }, ClickCallback.Options.builder().build())));
        }
        actions.add(FDialogFactory.button(
                Component.translatable("factions.building.dialog.button.requirements"),
                Component.translatable("factions.building.dialog.button.requirements_tooltip"),
                DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player clickedPlayer) {
                        showBuildingRequirements(clickedPlayer, faction, building, fails);
                    }
                }, ClickCallback.Options.builder().build())));
        actions.add(FDialogFactory.button(Component.translatable("factions.dialog.button.close"), null, null));

        FDialogFactory.showMultiAction(player,
                Component.translatable("factions.building.dialog.title",
                        Component.translatable("factions.building.buildings." + building.getId() + ".name")),
                body,
                actions);
    }

    public static void showBuildingRequirements(@NotNull Player player, @NotNull Faction faction,
                                                @NotNull Building building, @NotNull Set<RequirementFail> fails) {
        List<Component> body = new ArrayList<>();
        appendRequirements(body, faction, building, fails);
        FDialogFactory.showNotice(player,
                Component.translatable("factions.building.dialog.requirements.title",
                        Component.translatable("factions.building.buildings." + building.getId() + ".name")),
                body);
    }

    public static void showBuildSiteDetails(@NotNull Player player, @NotNull BuildSite site) {
        List<Component> body = new ArrayList<>();
        body.add(Component.translatable("factions.building.dialog.site.state",
                Component.translatable("factions.building.state." + site.getState().name().toLowerCase())));
        body.add(Component.translatable("factions.building.dialog.site.progress", Component.text(site.getProgressPercent())));
        if (!site.getMissingSections().isEmpty()) {
            body.add(Component.translatable("factions.building.dialog.site.missing_sections",
                    Component.text(String.join(", ", site.getMissingSections()))));
        }
        if (site.getProblemMessage() != null) {
            body.add(Component.translatable("factions.building.dialog.site.problem", Component.text(site.getProblemMessage())));
        }
        if (site.requiresInputChest()) {
            body.add(Component.translatable("factions.building.dialog.site.input_buffer",
                    Component.text(site.getInputBufferItemCount()), Component.text(site.getInputBufferCapacityItems())));
        }
        if (site.requiresOutputChest()) {
            body.add(Component.translatable("factions.building.dialog.site.output_buffer",
                    Component.text(site.getOutputBufferItemCount()), Component.text(site.getOutputBufferCapacityItems())));
        }
        appendUpgradeStatus(player, body, site);
        List<Component> configuredEffectLines = site.getConfiguredEffectDetailLines();
        if (!site.getEffects().isEmpty() || !configuredEffectLines.isEmpty()) {
            body.add(Component.empty());
            body.add(Component.translatable("factions.building.dialog.site.effects").color(NamedTextColor.GOLD));
            for (BuildingEffect effect : site.getEffects()) {
                body.add(Component.text("- ", NamedTextColor.GRAY).append(effect.getDisplayName()));
                for (Component line : effect.getDetailLines()) {
                    body.add(Component.text("  ").append(line));
                }
            }
            if (site.getEffects().isEmpty()) {
                body.addAll(configuredEffectLines);
            }
        }
        List<ActionButton> actions = new ArrayList<>();
        if (site.canBeginUpgrade()) {
            actions.add(FDialogFactory.button(
                    Component.translatable("factions.building.dialog.site.button.begin_upgrade"),
                    Component.translatable("factions.building.dialog.site.button.begin_upgrade_tooltip"),
                    DialogAction.customClick((response, audience) -> {
                        if (audience instanceof Player clickedPlayer && site.beginUpgrade(clickedPlayer)) {
                            showBuildSiteDetails(clickedPlayer, site);
                        }
                    }, ClickCallback.Options.builder().build())));
        }
        actions.add(FDialogFactory.button(Component.translatable("factions.dialog.button.close"), null, null));
        FDialogFactory.showMultiAction(player,
                Component.translatable("factions.building.dialog.site.title",
                        Component.translatable("factions.building.buildings." + site.getBuilding().getId() + ".name")),
                body,
                actions);
    }

    private static void appendUpgradeStatus(@NotNull Player player, @NotNull List<Component> body, @NotNull BuildSite site) {
        BuildingUpgradeConfig config = site.getUpgradeConfig();
        AddHousingView housing = AddHousingView.from(site);
        if (housing != null) {
            body.add(Component.empty());
            body.add(Component.translatable("factions.building.dialog.site.housing",
                    Component.text(housing.level()), Component.text(housing.amount())));
        }
        if (config == null) {
            return;
        }
        body.add(Component.translatable("factions.building.dialog.site.upgrade_target",
                Component.translatable("factions.building.buildings." + config.targetBuildingId() + ".name")));
        if (site.isUpgradeInProgress()) {
            body.add(Component.translatable("factions.building.dialog.site.upgrade_progress",
                    Component.text(site.getUpgradeProgressPercent())));
            for (BlockRequirement req : site.getUpgradeRequirements()) {
                body.add(Component.text("- ", NamedTextColor.GRAY).append(site.getRequirementProgressLine(req, true)));
            }
            return;
        }
        if (site.isUpgradeReady()) {
            body.add(Component.translatable("factions.building.dialog.site.upgrade_ready"));
            return;
        }
        body.add(Component.translatable("factions.building.dialog.site.upgrade_cycles",
                Component.text(site.getUpgradeSatisfiedPaydays()),
                Component.text(site.getUpgradeRequiredSatisfiedPaydays())));
        List<Component> blockers = site.getUpgradeReadinessBlockerLines();
        if (!blockers.isEmpty()) {
            body.add(Component.translatable("factions.building.dialog.site.upgrade_blockers").color(NamedTextColor.RED));
            for (Component blocker : blockers) {
                body.add(Component.text("- ", NamedTextColor.RED).append(flattenForDialog(player, blocker).color(NamedTextColor.RED)));
            }
        } else {
            body.add(Component.translatable("factions.building.dialog.site.upgrade_requirements_satisfied"));
        }
    }

    private static @NotNull Component flattenForDialog(@NotNull Player player, @NotNull Component component) {
        Component localized = FDialogFactory.localize(player, component);
        return Component.text(PlainTextComponentSerializer.plainText().serialize(localized));
    }

    private record AddHousingView(String level, int amount) {
        private static AddHousingView from(@NotNull BuildSite site) {
            de.erethon.factions.building.effects.AddHousing housing = site.getHousingEffect();
            if (housing == null) {
                return null;
            }
            return new AddHousingView(housing.getLevel().name().toLowerCase(), housing.getAmount());
        }
    }

    private static void appendDescription(@NotNull List<Component> body, @NotNull Building building) {
        for (int i = 1; i <= 5; i++) {
            body.add(Component.translatable("factions.building.buildings." + building.getId() + ".description." + i));
        }
    }

    private static void appendRequirements(@NotNull List<Component> body, @NotNull Faction faction,
                                           @NotNull Building building, @NotNull Set<RequirementFail> fails) {
        body.add(Component.empty());
        body.add(Component.translatable("factions.building.dialog.requirements.heading").color(NamedTextColor.GOLD));
        if (fails.isEmpty()) {
            body.add(Component.text("- ", NamedTextColor.GREEN).append(Component.translatable("factions.building.requirement.fulfilled")));
        } else {
            for (RequirementFail fail : fails) {
                body.add(Component.text("- ", NamedTextColor.RED).append(Component.translatable(fail.getTranslationKey())));
            }
        }
        if (!building.getRequiredPopulation().isEmpty()) {
            String requiredPopulation = building.getRequiredPopulation().entrySet().stream()
                    .map(entry -> entry.getKey().name().toLowerCase() + ": " + faction.getPopulation(entry.getKey()) + "/" + entry.getValue())
                    .collect(Collectors.joining(", "));
            body.add(Component.translatable("factions.building.dialog.requirements.population", Component.text(requiredPopulation)));
        }
        if (!building.getUnlockCost().isEmpty()) {
            String cost = building.getUnlockCost().entrySet().stream()
                    .map(entry -> entry.getKey().getId() + ": " + faction.getStorage().getResource(entry.getKey()) + "/" + entry.getValue())
                    .collect(Collectors.joining(", "));
            body.add(Component.translatable("factions.building.dialog.requirements.cost", Component.text(cost)));
        }
    }

    private static void appendEffects(@NotNull List<Component> body, @NotNull Building building) {
        if (building.getEffects().isEmpty()) {
            return;
        }
        body.add(Component.empty());
        body.add(Component.translatable("factions.building.dialog.effects.heading").color(NamedTextColor.GOLD));
        for (BuildingEffectData effect : building.getEffects()) {
            body.add(Component.text("- ", NamedTextColor.GRAY).append(effect.getDisplayName()));
        }
    }
}
