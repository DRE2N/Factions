package de.erethon.factions.economy.gui;

import de.erethon.factions.Factions;
import de.erethon.factions.building.BuildSite;
import de.erethon.factions.building.BuildSitePlacer;
import de.erethon.factions.building.Building;
import de.erethon.factions.building.BuildingEffect;
import de.erethon.factions.building.BuildingEffectData;
import de.erethon.factions.building.BuildingUserConfig;
import de.erethon.factions.building.RequirementFail;
import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.economy.report.ProductionChainReport;
import de.erethon.factions.faction.Faction;
import de.erethon.factions.player.FPlayer;
import de.erethon.factions.region.ClaimableRegion;
import de.erethon.rhea.Rhea;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Catalog and buildsite pages; the building model still owns every mutation. */
public final class FactionBuildingUi {
    private final FactionEconomyUi ui;

    FactionBuildingUi(FactionEconomyUi ui) { this.ui = ui; }

    public void showCatalog(Player player, Faction faction, int listPage, Building selected) {
        List<Building> all = catalog();
        int last = Math.max(0, (all.size() - 1) / 5);
        int current = Math.clamp(listPage, 0, last);
        Building building = selected == null && !all.isEmpty() ? all.get(current * 5) : selected;
        Rhea.DialogBuilder page = ui.page(player, faction, "buildings", "buildings_title",
                click -> showCatalog(click, faction, current, building));
        ui.put(page, "list_heading", ui.tr(player, "building_catalog"));
        for (int i = 0; i < 5 && current * 5 + i < all.size(); i++) {
            Building row = all.get(current * 5 + i);
            Component name = ui.local(player, ui.buildingName(row.getId()));
            ui.put(page, "row_" + i, row == building ? name.color(NamedTextColor.GOLD) : name);
            page.action("building:" + i, click -> showCatalog(click, faction, current, row));
        }
        ui.pager(player, page, current, last,
                click -> showCatalog(click, faction, current - 1, null),
                click -> showCatalog(click, faction, current + 1, null));
        if (building != null) {
            ui.put(page, "detail_title", ui.local(player, ui.buildingName(building.getId())));
            ui.put(page, "detail_0", description(player, building));
            ui.put(page, "subheading", ui.tr(player, "requirements"));
            ui.put(page, "detail_1", cost(player, building));
            Set<RequirementFail> fails = building.checkRequirements(player, faction, player.getLocation());
            ui.put(page, "detail_note", ui.tr(player, fails.isEmpty() ? "ready_to_place" : "blocked_count",
                    Component.text(fails.size())));
            ui.put(page, "button", ui.tr(player, "details"));
            page.action("building:details", click -> showDetail(click, faction, building, current, 0));
        }
        page.show(player);
    }

    public void showDetail(Player player, Faction faction, Building building, int catalogPage, int detailPage) {
        Rhea.DialogBuilder page = ui.page(player, faction, "building-detail", "building_detail_title",
                click -> showDetail(click, faction, building, catalogPage, detailPage));
        ui.put(page, "detail_title", ui.local(player, ui.buildingName(building.getId())));
        ui.put(page, "detail_0", description(player, building));
        ui.put(page, "requirements_label", ui.tr(player, "requirements"));
        ui.put(page, "effects_label", ui.tr(player, "effects"));
        List<Component> requirements = requirements(player, faction, building);
        List<Component> effects = building.getEffects().stream().map(BuildingEffectData::getDisplayName)
                .map(value -> ui.local(player, value)).toList();
        int count = Math.max(1, Math.max((requirements.size() + 3) / 4, (effects.size() + 3) / 4));
        int part = Math.floorMod(detailPage, count);
        ui.put(page, "requirements", lines(requirements, part * 4, 4));
        ui.put(page, "effects", lines(effects, part * 4, 4));
        fillProductionChain(player, faction, building, page);
        Set<RequirementFail> fails = building.checkRequirements(player, faction, player.getLocation());
        ui.put(page, "detail_note", count > 1 ? ui.tr(player, "more_details", Component.text(part + 1), Component.text(count))
                : ui.tr(player, fails.isEmpty() ? "ready_to_place" : "blocked_count", Component.text(fails.size())));
        if (count > 1) page.action("details:next", click -> showDetail(click, faction, building, catalogPage, part + 1));
        ui.put(page, "button", ui.tr(player, fails.isEmpty() ? "place" : "locked"));
        if (fails.isEmpty()) page.action("building:place", click -> {
            FPlayer fPlayer = Factions.get().getFPlayerCache().getByPlayer(click);
            Faction currentFaction = fPlayer.getFaction();
            if (currentFaction != faction || !faction.isPrivileged(fPlayer) || faction.getUnrestLevel() > 0
                    || !(fPlayer.getCurrentRegion() instanceof ClaimableRegion region)
                    || !building.checkRequirements(click, faction, click.getLocation()).isEmpty()) {
                showDetail(click, faction, building, catalogPage, detailPage);
                return;
            }
            click.closeDialog();
            new BuildSitePlacer(building, fPlayer, region, faction);
        });
        if (!fails.isEmpty()) page.hover("building:place", reasons(player, fails));
        page.show(player);
    }

    public void showSite(Player player, BuildSite site) {
        Faction faction = site.getFaction();
        if (faction == null) return;
        Rhea.DialogBuilder page = ui.page(player, faction, "site", "site_title", click -> showSite(click, site));
        ui.put(page, "detail_title", ui.local(player, ui.buildingName(site.getBuilding().getId())));
        ui.put(page, "detail_0", ui.tr(player, "site_state", ui.local(player,
                Component.translatable("factions.building.state." + site.getState().name().toLowerCase(Locale.ROOT)))));
        ui.put(page, "detail_1", ui.tr(player, site.isUpgradeInProgress() ? "upgrade_progress" : "site_progress",
                Component.text(site.isUpgradeInProgress() ? site.getUpgradeProgressPercent() : site.getProgressPercent())));
        ui.put(page, "detail_2", site.requiresInputChest()
                ? ui.tr(player, "input_buffer", Component.text(site.getInputBufferItemCount()),
                    Component.text(site.getInputBufferCapacityItems())) : ui.tr(player, "no_input_buffer"));
        ui.put(page, "detail_3", site.requiresOutputChest()
                ? ui.tr(player, "output_buffer", Component.text(site.getOutputBufferItemCount()),
                    Component.text(site.getOutputBufferCapacityItems())) : ui.tr(player, "no_output_buffer"));
        page.meter("progress", (site.isUpgradeInProgress() ? site.getUpgradeProgressPercent() : site.getProgressPercent()) / 100.0,
                NamedTextColor.AQUA);
        ui.put(page, "requirements_label", ui.tr(player, "work_and_blockers"));
        List<Component> blockers = new ArrayList<>();
        if (!site.getMissingSections().isEmpty()) blockers.add(ui.tr(player, "missing_sections",
                Component.text(String.join(", ", site.getMissingSections()))));
        if (site.getProblemMessage() != null) blockers.add(Component.text(site.getProblemMessage()));
        blockers.addAll(site.getUpgradeReadinessBlockerLines().stream().map(value -> ui.local(player, value)).toList());
        ui.put(page, "requirements", lines(blockers, 0, 3));
        ui.put(page, "effects_label", ui.tr(player, "effects"));
        List<Component> effects = site.getEffects().stream().map(BuildingEffect::getDisplayName)
                .map(value -> ui.local(player, value)).toList();
        ui.put(page, "effects", lines(effects, 0, 3));
        ui.put(page, "detail_note", site.getUpgradeConfig() == null ? ui.tr(player, "no_upgrade")
                : site.isUpgradeInProgress() ? ui.tr(player, "upgrade_progress", Component.text(site.getUpgradeProgressPercent()))
                : site.isUpgradeReady() ? ui.tr(player, "upgrade_ready")
                : ui.tr(player, "upgrade_cycles", Component.text(site.getUpgradeSatisfiedPaydays()),
                    Component.text(site.getUpgradeRequiredSatisfiedPaydays())));
        ui.put(page, "settings_button", ui.tr(player, "settings"));
        if (!site.getBuilding().getUserConfig().settings().isEmpty())
            page.action("site:settings", click -> showSettings(click, site, 0, 0, 0));
        ui.put(page, "button", ui.tr(player, site.canBeginUpgrade() ? "begin_upgrade" : "locked"));
        if (site.canBeginUpgrade() && site.canConfigure(player)) page.action("site:upgrade", click -> {
            if (site.canConfigure(click) && site.canBeginUpgrade() && site.beginUpgrade(click)) showSite(click, site);
            else showSite(click, site);
        });
        page.show(player);
    }

    public void showSettings(Player player, BuildSite site, int settingPage, int selectedIndex, int optionPage) {
        Faction faction = site.getFaction();
        if (faction == null) return;
        List<BuildingUserConfig.Setting> settings = site.getBuilding().getUserConfig().settings();
        if (settings.isEmpty()) { showSite(player, site); return; }
        int last = (settings.size() - 1) / 4;
        int current = Math.clamp(settingPage, 0, last);
        int selected = Math.clamp(selectedIndex, 0, settings.size() - 1);
        BuildingUserConfig.Setting setting = settings.get(selected);
        Rhea.DialogBuilder page = ui.page(player, faction, "settings", "settings_title",
                click -> showSettings(click, site, current, selected, optionPage));
        ui.put(page, "list_heading", ui.tr(player, "settings"));
        for (int i = 0; i < 4 && current * 4 + i < settings.size(); i++) {
            int index = current * 4 + i;
            BuildingUserConfig.Setting row = settings.get(index);
            ui.put(page, "row_" + i, ui.local(player, row.displayName())
                    .color(index == selected ? NamedTextColor.GOLD : NamedTextColor.WHITE));
            page.action("setting:" + i, click -> showSettings(click, site, current, index, 0));
        }
        ui.pager(player, page, current, last,
                click -> showSettings(click, site, current - 1, selected, 0),
                click -> showSettings(click, site, current + 1, selected, 0));
        ui.put(page, "detail_title", ui.local(player, setting.displayName()));
        ui.put(page, "subheading", ui.tr(player, "choose_option"));
        String saved = site.getString("userConfig." + setting.id(), setting.defaultOption());
        String chosen = setting.selected(saved).id();
        List<BuildingUserConfig.Option> options = List.copyOf(setting.options().values());
        int optionPages = Math.max(1, (options.size() + 3) / 4);
        int offset = Math.floorMod(optionPage, optionPages) * 4;
        for (int i = 0; i < 4 && offset + i < options.size(); i++) {
            BuildingUserConfig.Option option = options.get(offset + i);
            boolean available = option.available(faction);
            Component label = ui.local(player, option.displayName());
            ui.put(page, "option_" + i, label.color(option.id().equals(chosen) ? NamedTextColor.GOLD
                    : available ? NamedTextColor.WHITE : NamedTextColor.GRAY));
            if (available && site.canConfigure(player)) page.action("option:" + i, click -> {
                if (site.canConfigure(click) && option.available(site.getFaction()))
                    site.configure(click, setting.id(), option.id());
                showSettings(click, site, current, selected, optionPage);
            });
        }
        ui.put(page, "detail_note", optionPages > 1 ? ui.tr(player, "more_options", Component.text(optionPage % optionPages + 1),
                Component.text(optionPages)) : site.canConfigure(player) ? ui.tr(player, "setting_hint") : ui.tr(player, "read_only"));
        if (optionPages > 1) page.action("options:next", click -> showSettings(click, site, current, selected, optionPage + 1));
        page.show(player);
    }

    private List<Building> catalog() {
        return Factions.get().getBuildingManager().getBuildings().stream().filter(Building::isShownInBuildingMenu)
                .sorted(Comparator.comparing(Building::getId)).toList();
    }

    private void fillProductionChain(Player player, Faction faction, Building building, Rhea.DialogBuilder page) {
        Map<Resource, Integer> inputs = new EnumMap<>(Resource.class);
        Map<Resource, Integer> outputs = new EnumMap<>(Resource.class);
        for (BuildingEffectData effect : building.getEffects()) {
            String section = switch (effect.getId()) {
                case "ResourceConsumption" -> "consumption";
                case "ResourceProduction" -> "production";
                default -> null;
            };
            if (section == null || effect.getConfigurationSection(section) == null) continue;
            for (String key : effect.getConfigurationSection(section).getKeys(false)) {
                try {
                    Resource resource = Resource.valueOf(key.toUpperCase(Locale.ROOT));
                    (section.equals("consumption") ? inputs : outputs).merge(resource, effect.getInt(section + "." + key), Integer::sum);
                } catch (IllegalArgumentException ignored) {
                    // A building may define non-economy items in the same effect section.
                }
            }
        }
        ProductionChainReport placed = faction.getEconomy().getLastReport().productionChains().stream()
                .filter(chain -> chain.buildingId().equals(building.getId())).findFirst().orElse(null);
        chainGood(player, faction, page, "input", inputs);
        chainGood(player, faction, page, "output", outputs);
        ui.put(page, "chain_state", placed == null
                ? Component.text(player.locale().getLanguage().equals("de") ? "Geplant" : "Planned")
                : ui.tr(player, placed.active() ? "active" : "inactive"));
    }

    private void chainGood(Player player, Faction faction, Rhea.DialogBuilder page, String side,
                           Map<Resource, Integer> goods) {
        if (goods.isEmpty()) {
            ui.put(page, "chain_" + side, ui.tr(player, "none"));
            return;
        }
        Resource first = goods.keySet().stream().sorted().findFirst().orElseThrow();
        Component label = ui.local(player, first.displayName()).append(Component.text(" " + goods.get(first)
                + (goods.size() > 1 ? " +" + (goods.size() - 1) : "")));
        ui.put(page, "chain_" + side, label);
        page.action("detail:" + side, click -> ui.showResources(click, faction, first.ordinal() / 5, first));
        if (goods.size() > 1) page.hover("detail:" + side, lines(goods.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).map(entry -> ui.local(player, entry.getKey().displayName())
                        .append(Component.text(" " + entry.getValue()))).toList(), 0, goods.size()));
    }

    private Component description(Player player, Building building) {
        List<Component> lines = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            String key = "factions.building.buildings." + building.getId() + ".description." + i;
            Component value = ui.local(player, Component.translatable(key));
            if (!PlainTextComponentSerializer.plainText().serialize(value).equals(key)) lines.add(value);
        }
        return lines(lines, 0, 3);
    }

    private Component cost(Player player, Building building) {
        if (building.getUnlockCost().isEmpty()) return ui.tr(player, "no_cost");
        List<Component> costs = building.getUnlockCost().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> ui.local(player, entry.getKey().displayName()).append(Component.text(" " + entry.getValue())))
                .toList();
        return lines(costs, 0, 2);
    }

    private List<Component> requirements(Player player, Faction faction, Building building) {
        List<Component> lines = new ArrayList<>();
        for (var cost : building.getUnlockCost().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            Resource resource = cost.getKey();
            lines.add(ui.local(player, resource.displayName()).append(Component.text(" "
                    + faction.getStorage().getResource(resource) + "/" + cost.getValue())));
        }
        for (var requirement : building.getRequiredPopulation().entrySet())
            lines.add(ui.local(player, requirement.getKey().displayName()).append(Component.text(" " + requirement.getValue())));
        Set<RequirementFail> fails = building.checkRequirements(player, faction, player.getLocation());
        for (RequirementFail fail : fails) lines.add(ui.local(player, Component.translatable(fail.getTranslationKey())));
        if (lines.isEmpty()) lines.add(ui.tr(player, "none"));
        return lines;
    }

    private Component reasons(Player player, Set<RequirementFail> fails) {
        return lines(fails.stream().map(fail -> ui.local(player, Component.translatable(fail.getTranslationKey()))).toList(), 0, 8);
    }

    private Component lines(List<Component> values, int offset, int limit) {
        Component result = Component.empty();
        for (int i = offset; i < Math.min(values.size(), offset + limit); i++) {
            if (i > offset) result = result.append(Component.newline());
            result = result.append(values.get(i));
        }
        return result;
    }
}
