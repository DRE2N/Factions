package de.erethon.factions.building;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class BuildingUserConfigTest {
    private static final String DEFINITION = """
            userConfig:
              product:
                default: none
                options:
                  none: {}
                  iron:
                    requiredBuildings:
                      geologist: 1
            """;

    @Test
    void choiceSurvivesSiteYamlRoundTripAndDoesNotAffectAnotherSite() throws Exception {
        YamlConfiguration definition = new YamlConfiguration();
        definition.loadFromString(DEFINITION);
        var settings = new BuildingUserConfig(definition.getConfigurationSection("userConfig"), new HashSet<>());
        var setting = settings.setting("product");
        YamlConfiguration firstSite = new YamlConfiguration();
        firstSite.set("userConfig.product", "iron");
        YamlConfiguration reloaded = new YamlConfiguration();
        reloaded.loadFromString(firstSite.saveToString());
        assertEquals("iron", setting.selected(reloaded.getString("userConfig.product")).id());
        assertEquals("none", setting.selected(new YamlConfiguration().getString("userConfig.product")).id());
        assertEquals(1, setting.options().get("iron").requiredBuildings().get("geologist"));
    }

    @Test
    void removedChoiceFallsBackToDefault() throws Exception {
        YamlConfiguration definition = new YamlConfiguration();
        definition.loadFromString(DEFINITION);
        var setting = new BuildingUserConfig(definition.getConfigurationSection("userConfig"), new HashSet<>()).setting("product");
        assertEquals("none", setting.selected("removed-option").id());
    }

    @Test
    void invalidDefaultIsRejected() throws Exception {
        YamlConfiguration definition = new YamlConfiguration();
        definition.loadFromString(DEFINITION.replace("default: none", "default: missing"));
        assertThrows(IllegalArgumentException.class, () ->
                new BuildingUserConfig(definition.getConfigurationSection("userConfig"), new HashSet<>()));
    }
}

