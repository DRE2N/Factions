package de.erethon.factions.economy.gui;

import de.erethon.factions.Factions;
import de.erethon.factions.building.Building;
import de.erethon.factions.dialog.FDialogFactory;
import de.erethon.factions.economy.EconomyHistory;
import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.economy.report.EconomyCycleReport;
import de.erethon.factions.economy.report.PopulationLevelReport;
import de.erethon.factions.economy.report.ProductionChainReport;
import de.erethon.factions.economy.report.ResourceFlowReport;
import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.faction.Faction;
import de.erethon.rhea.Rhea;
import de.erethon.rhea.RheaTemplate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** Player-facing economy pages. All numbers are read again when a page opens. */
public final class FactionEconomyUi {
    private static FactionEconomyUi instance;
    private static final NamedTextColor COPPER = NamedTextColor.GOLD;
    private static final NamedTextColor SEA = NamedTextColor.AQUA;

    private final Rhea rhea;
    private final Map<String, RheaTemplate> templates = new HashMap<>();
    private final FactionBuildingUi buildings;

    private FactionEconomyUi() {
        Factions plugin = Factions.get();
        rhea = new Rhea(plugin);
        for (String id : List.of("overview", "population", "resources", "buildings", "building-detail", "site", "settings")) {
            templates.put(id, RheaTemplate.read(plugin, "rhea/templates/" + id + ".json"));
        }
        buildings = new FactionBuildingUi(this);
    }

    public static FactionEconomyUi get() {
        if (instance == null) instance = new FactionEconomyUi();
        return instance;
    }

    public FactionBuildingUi buildings() {
        return buildings;
    }

    Rhea rhea() { return rhea; }

    Component tr(Player player, String key, Component... args) {
        return FDialogFactory.localize(player, Component.translatable("factions.rhea." + key, args));
    }

    Component local(Player player, Component value) {
        return FDialogFactory.localize(player, value);
    }

    String number(Player player, double value) {
        return String.format(player.locale().getLanguage().equals("de") ? Locale.GERMAN : Locale.ENGLISH, "%.1f", value);
    }

    void put(Rhea.DialogBuilder page, String id, Component text) {
        page.slot(id, printable(text));
    }

    // Faction building packs can define IDs without language entries. Rhea's
    // measured text needs concrete text even when a translation is missing.
    static Component printable(Component value) {
        if (value instanceof TranslatableComponent translated) {
            String key = translated.key();
            String name = key.startsWith("factions.building.buildings.") && key.endsWith(".name")
                    ? key.substring("factions.building.buildings.".length(), key.length() - ".name".length())
                    : key.substring(key.lastIndexOf('.') + 1);
            String label = Arrays.stream(name.split("[_-]"))
                    .filter(part -> !part.isEmpty())
                    .map(part -> part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1))
                    .reduce((first, next) -> first + " " + next).orElse(name);
            return Component.text(label).style(value.style()).children(
                    value.children().stream().map(FactionEconomyUi::printable).toList());
        }
        if (value instanceof TextComponent text) return text.children(
                text.children().stream().map(FactionEconomyUi::printable).toList());
        return Component.text(PlainTextComponentSerializer.plainText().serialize(value))
                .style(value.style()).children(value.children().stream().map(FactionEconomyUi::printable).toList());
    }

    Rhea.DialogBuilder page(Player player, Faction faction, String id, String title, Consumer<Player> refresh) {
        Rhea.DialogBuilder page = rhea.dialog(templates.get(id), tr(player, "native_title"), tr(player, "close"));
        put(page, "header", tr(player, "header"));
        put(page, "faction", Component.text(faction.getName()));
        put(page, "view_title", tr(player, title));
        put(page, "nav_overview", tr(player, "nav_overview"));
        put(page, "nav_population", tr(player, "nav_population"));
        put(page, "nav_resources", tr(player, "nav_resources"));
        put(page, "nav_buildings", tr(player, "nav_buildings"));
        put(page, "refresh_label", tr(player, "refresh"));
        page.action("go:overview", click -> showOverview(click, faction));
        page.action("go:population", click -> showPopulation(click, faction, PopulationLevel.PEASANT, 0));
        page.action("go:resources", click -> showResources(click, faction, 0, null));
        page.action("go:buildings", click -> buildings.showCatalog(click, faction, 0, null));
        page.action("refresh", refresh);
        return page;
    }

    public void showOverview(Player player, Faction faction) {
        EconomyCycleReport report = faction.getEconomy().getLastReport();
        Rhea.DialogBuilder page = page(player, faction, "overview", "overview_title", click -> showOverview(click, faction));
        int people = Arrays.stream(PopulationLevel.values()).mapToInt(faction::getPopulation).sum();
        long shortages = report.resources().values().stream().filter(flow -> flow.net() < 0).count();
        long full = report.resources().values().stream().filter(ResourceFlowReport::storageFull).count();
        long available = report.buildings().values().stream().filter(unlock -> unlock.available() && !unlock.built()).count();
        Resource warning = Arrays.stream(Resource.values()).filter(resource -> {
            ResourceFlowReport flow = report.resources().get(resource);
            return flow != null && (flow.net() < 0 || flow.storageFull());
        }).findFirst().orElse(null);
        card(player, page, "a", "population", Component.text(people), tr(player, "unrest_value", Component.text(number(player, faction.getUnrestLevel()))));
        card(player, page, "b", "resources", Component.text(shortages), tr(player, "full_storage", Component.text(full)));
        card(player, page, "d", "buildings", Component.text(available), tr(player, "available_buildings"));
        put(page, "overview_status", report.resources().isEmpty() ? tr(player, "no_cycle") :
                tr(player, "overview_status", Component.text(number(player, report.totalTaxRevenue())),
                        Component.text(number(player, report.unrestAfter())), Component.text(shortages + full)));
        List<EconomyHistory.Sample> history = faction.getEconomy().getHistory().samples();
        List<Double> produced = new ArrayList<>();
        List<Double> consumed = new ArrayList<>();
        for (EconomyHistory.Sample sample : history.subList(Math.max(0, history.size() - 14), history.size())) {
            produced.add(sample.resources().values().stream().mapToDouble(EconomyHistory.ResourceSample::produced).sum());
            consumed.add(sample.resources().values().stream().mapToDouble(EconomyHistory.ResourceSample::consumed).sum());
        }
        page.bars("overview_history", produced, consumed, COPPER, SEA);
        put(page, "overview_graph_label", tr(player, "graph_legend", Component.text(history.size())));
        if (warning != null) {
            page.action("overview:warning", click -> showResources(click, faction, warning.ordinal() / 5, warning));
            page.hover("overview:warning", local(player, warning.displayName()));
        }
        page.action("card:population", click -> showPopulation(click, faction, PopulationLevel.PEASANT, 0));
        page.action("card:resources", click -> showResources(click, faction, 0, null));
        page.action("card:buildings", click -> buildings.showCatalog(click, faction, 0, null));
        page.show(player);
    }

    private void card(Player player, Rhea.DialogBuilder page, String letter, String title, Component value, Component note) {
        put(page, "card_" + letter + "_title", tr(player, title));
        put(page, "card_" + letter + "_value", value);
        put(page, "card_" + letter + "_note", note);
    }

    public void showPopulation(Player player, Faction faction, PopulationLevel selected, int needPage) {
        Rhea.DialogBuilder page = page(player, faction, "population", "population_title",
                click -> showPopulation(click, faction, selected, needPage));
        put(page, "list_heading", tr(player, "tiers"));
        PopulationLevel[] levels = PopulationLevel.values();
        for (int i = 0; i < levels.length; i++) {
            PopulationLevel level = levels[i];
            Component label = local(player, level.displayName()).append(Component.text("  " + faction.getPopulation(level)));
            put(page, "row_" + i, level == selected ? label.color(COPPER) : label);
            page.action("tier:" + i, click -> showPopulation(click, faction, level, 0));
            List<Building> unlocks = Factions.get().getBuildingManager().getBuildings().stream()
                    .filter(building -> building.getRequiredPopulation().containsKey(level)).toList();
            if (!unlocks.isEmpty()) {
                Component tip = tr(player, "unlocks");
                for (Building building : unlocks) tip = tip.append(Component.newline())
                        .append(local(player, buildingName(building.getId())));
                page.hover("tier:" + i, tip);
            }
        }
        PopulationLevelReport report = faction.getEconomy().getLastReport().population().get(selected);
        int count = faction.getPopulation(selected);
        int housing = (int) faction.getAttributeValue("housing_" + selected.name().toLowerCase(Locale.ROOT), 0);
        double happiness = faction.getHappiness(selected);
        put(page, "detail_title", local(player, selected.displayName()));
        put(page, "detail_0", tr(player, "housing_value", Component.text(count), Component.text(housing)));
        page.meter("housing", housing <= 0 ? 0 : (double) count / housing,
                count > housing ? NamedTextColor.RED : SEA);
        put(page, "detail_1", report == null ? tr(player, "happiness_value", Component.text(number(player, happiness))) :
                tr(player, "happiness_target", Component.text(number(player, happiness)),
                        Component.text(number(player, report.targetHappiness()))));
        put(page, "meter_label", tr(player, "happiness"));
        page.meter("happiness", happiness, happiness < .4 ? NamedTextColor.RED : SEA);
        put(page, "subheading", tr(player, "needs"));
        List<Resource> needs = selected.getResources().stream().sorted().toList();
        int offset = Math.floorMod(needPage, Math.max(1, (needs.size() + 2) / 3)) * 3;
        for (int i = 0; report != null && i < 3; i++) {
            if (offset + i >= needs.size()) continue;
            Resource resource = needs.get(offset + i);
            double required = report == null ? 0 : report.required().getOrDefault(resource, 0.0);
            double consumed = report == null ? 0 : report.consumed().getOrDefault(resource, 0.0);
            Component line = local(player, resource.displayName()).append(Component.text("  " + number(player, consumed)
                    + "/" + number(player, required), required > 0 && consumed < required ? NamedTextColor.RED : SEA));
            put(page, "detail_" + (i + 2), line);
            page.action("need:" + i, click -> showResources(click, faction, resource.ordinal() / 5, resource));
        }
        if (report == null) {
            put(page, "detail_note", tr(player, "no_cycle"));
        } else if (needs.size() > 3) {
            put(page, "detail_note", tr(player, "more_needs", Component.text(offset / 3 + 1),
                    Component.text((needs.size() + 2) / 3)));
            page.action("needs:next", click -> showPopulation(click, faction, selected, needPage + 1));
        } else {
            int blockers = report.levelUpBlockers().size() + report.levelDownRisks().size();
            put(page, "detail_note", tr(player, "population_issues", Component.text(blockers)));
        }
        if (report != null && (!report.levelUpBlockers().isEmpty() || !report.levelDownRisks().isEmpty())) {
            Component reasons = Component.empty();
            for (String reason : report.levelUpBlockers())
                reasons = reasons.append(local(player, Component.translatable("factions.reason." + reason.replace(':', '.'))))
                        .append(Component.newline());
            for (String reason : report.levelDownRisks())
                reasons = reasons.append(local(player, Component.translatable("factions.reason." + reason.replace(':', '.'))))
                        .append(Component.newline());
            page.hover("needs:next", reasons);
        }
        page.show(player);
    }

    public void showResources(Player player, Faction faction, int listPage, Resource selected) {
        List<Resource> all = List.of(Resource.values());
        int lastPage = (all.size() - 1) / 5;
        int current = Math.clamp(listPage, 0, lastPage);
        Resource resource = selected == null ? all.get(current * 5) : selected;
        Rhea.DialogBuilder page = page(player, faction, "resources", "resources_title",
                click -> showResources(click, faction, current, resource));
        put(page, "list_heading", tr(player, "all_goods"));
        for (int i = 0; i < 5 && current * 5 + i < all.size(); i++) {
            Resource row = all.get(current * 5 + i);
            Component label = local(player, row.displayName()).append(Component.text("  " + faction.getStorage().getResource(row)));
            put(page, "row_" + i, row == resource ? label.color(COPPER) : label);
            page.action("resource:" + i, click -> showResources(click, faction, current, row));
        }
        pager(player, page, current, lastPage,
                click -> showResources(click, faction, current - 1, null),
                click -> showResources(click, faction, current + 1, null));
        ResourceFlowReport flow = faction.getEconomy().getLastReport().resources().get(resource);
        int stored = faction.getStorage().getResource(resource);
        int capacity = faction.getStorage().getResourceLimit(resource);
        put(page, "detail_title", local(player, resource.displayName()));
        put(page, "detail_0", tr(player, "storage_value", Component.text(stored), Component.text(capacity)));
        page.meter("storage", capacity <= 0 ? 0 : (double) stored / capacity, stored >= capacity && capacity > 0 ? COPPER : SEA);
        if (flow == null) {
            put(page, "detail_1", tr(player, "no_cycle"));
        } else {
            put(page, "detail_1", tr(player, "produced", Component.text(number(player, flow.acceptedProduction()))));
            put(page, "detail_2", tr(player, "consumed", Component.text(number(player, flow.consumed()))));
            put(page, "detail_3", tr(player, "lost", Component.text(number(player, flow.lostProduction()))));
            put(page, "detail_4", tr(player, "net", Component.text(number(player, flow.net()))));
        }
        List<EconomyHistory.Sample> history = faction.getEconomy().getHistory().samples();
        List<Double> production = new ArrayList<>();
        List<Double> consumption = new ArrayList<>();
        for (EconomyHistory.Sample sample : history.subList(Math.max(0, history.size() - 14), history.size())) {
            EconomyHistory.ResourceSample value = sample.resources().get(resource);
            production.add(value == null ? 0.0 : Math.max(0, value.produced()));
            consumption.add(value == null ? 0.0 : Math.max(0, value.consumed()));
        }
        page.bars("history", production, consumption, COPPER, SEA);
        put(page, "detail_note", tr(player, "graph_legend", Component.text(history.size())));
        page.show(player);
    }

    void pager(Player player, Rhea.DialogBuilder page, int current, int last,
               Consumer<Player> previous, Consumer<Player> next) {
        put(page, "page_number", Component.text((current + 1) + " / " + (last + 1)));
        if (current > 0) {
            put(page, "page_prev", tr(player, "previous"));
            page.action("page:prev", previous);
        }
        if (current < last) {
            put(page, "page_next", tr(player, "next"));
            page.action("page:next", next);
        }
    }

    Component buildingName(String id) {
        return Component.translatable("factions.building.buildings." + id + ".name");
    }
}
