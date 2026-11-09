package de.erethon.factions.economy.gui;

import de.erethon.rhea.RheaTemplate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionUiTemplateTest {
    @Test
    void sharedBindingsExistOnEveryPage() {
        for (String id : List.of("overview", "population", "resources", "chains", "buildings",
                "building-detail", "site", "settings")) {
            RheaTemplate template = RheaTemplate.read(getClass().getResourceAsStream("/rhea/templates/" + id + ".json"));
            for (String slot : List.of("header", "faction", "view_title", "nav_overview", "nav_population",
                    "nav_resources", "nav_chains", "nav_buildings", "refresh_label")) {
                assertNotNull(template.slot(slot), id + ": " + slot);
            }
            for (String action : List.of("go:overview", "go:population", "go:resources", "go:chains",
                    "go:buildings", "refresh")) {
                assertTrue(template.actions().contains(action), id + ": " + action);
            }
        }
    }

    @Test
    void visualBindingsExistOnTheirPages() {
        RheaTemplate population = RheaTemplate.read(getClass().getResourceAsStream("/rhea/templates/population.json"));
        population.visual("housing");
        population.visual("happiness");
        RheaTemplate resources = RheaTemplate.read(getClass().getResourceAsStream("/rhea/templates/resources.json"));
        resources.visual("storage");
        resources.visual("history");
        RheaTemplate site = RheaTemplate.read(getClass().getResourceAsStream("/rhea/templates/site.json"));
        site.visual("progress");
    }

    @Test
    void missingBuildingTranslationsBecomeMeasuredText() {
        Component value = Component.text("Site: ").append(
                Component.translatable("factions.building.buildings.apple_tree.name"));
        Component printable = FactionEconomyUi.printable(value);
        assertTrue(printable instanceof TextComponent);
        assertTrue(printable.children().getFirst() instanceof TextComponent);
        assertTrue(((TextComponent) printable.children().getFirst()).content().equals("Apple Tree"));
    }
}
