package de.erethon.factions.building;

import de.erethon.factions.dialog.FDialogFactory;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class BuildingSettingsDialog {

    private BuildingSettingsDialog() {
    }

    public static void show(Player player, BuildSite site) {
        List<Component> body = new ArrayList<>();
        List<ActionButton> actions = new ArrayList<>();
        for (BuildingUserConfig.Setting setting : site.getBuilding().getUserConfig().settings()) {
            BuildingUserConfig.Option selected = setting.selected(site.getString("userConfig." + setting.id(), setting.defaultOption()));
            body.add(Component.translatable("factions.building.settings.value", setting.displayName(), selected.displayName()));
            if (site.canConfigure(player)) {
                actions.add(FDialogFactory.button(setting.displayName(), null,
                        DialogAction.customClick((response, audience) -> {
                            if (audience instanceof Player clicker) showOptions(clicker, site, setting.id());
                        }, ClickCallback.Options.builder().build())));
            }
        }
        actions.add(back(site));
        FDialogFactory.showMultiAction(player, Component.translatable("factions.building.settings.title"), body, actions);
    }

    private static void showOptions(Player player, BuildSite site, String settingId) {
        BuildingUserConfig.Setting setting = site.getBuilding().getUserConfig().setting(settingId);
        if (setting == null || !site.canConfigure(player)) {
            show(player, site);
            return;
        }
        List<ActionButton> actions = new ArrayList<>();
        List<Component> body = new ArrayList<>();
        String current = setting.selected(site.getString("userConfig." + setting.id(), setting.defaultOption())).id();
        for (BuildingUserConfig.Option option : setting.options().values()) {
            boolean available = option.available(site.getFaction());
            Component label = option.id().equals(current)
                    ? Component.translatable("factions.building.settings.selected", option.displayName()) : option.displayName();
            if (!available) {
                label = Component.translatable("factions.building.settings.locked", label);
                for (var required : option.requiredBuildings().entrySet()) {
                    body.add(Component.translatable("factions.building.settings.requires", option.displayName(),
                            Component.translatable("factions.building.buildings." + required.getKey() + ".name"),
                            Component.text(required.getValue())));
                }
            }
            actions.add(FDialogFactory.button(label, null, !available ? null :
                    DialogAction.customClick((response, audience) -> {
                        if (audience instanceof Player clicker) {
                            if (!site.configure(clicker, settingId, option.id())) {
                                FDialogFactory.showNotice(clicker, Component.translatable("factions.building.settings.title"),
                                        List.of(Component.translatable("factions.building.settings.failed")));
                                return;
                            }
                            show(clicker, site);
                        }
                    }, ClickCallback.Options.builder().build())));
        }
        actions.add(back(site));
        FDialogFactory.showMultiAction(player, setting.displayName(), body, actions);
    }

    private static ActionButton back(BuildSite site) {
        return FDialogFactory.button(Component.translatable("factions.gui.back"), null,
                DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player clicker) BuildingDialogs.showBuildSiteDetails(clicker, site);
                }, ClickCallback.Options.builder().build()));
    }
}
