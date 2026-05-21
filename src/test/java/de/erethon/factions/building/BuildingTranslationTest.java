package de.erethon.factions.building;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BuildingTranslationTest {

    @Test
    void buildingV2TranslationKeysExistInEnglishAndGerman() {
        YamlConfiguration english = YamlConfiguration.loadConfiguration(new File("src/main/resources/languages/english.yml"));
        YamlConfiguration german = YamlConfiguration.loadConfiguration(new File("src/main/resources/languages/german.yml"));

        for (String key : requiredKeys()) {
            assertTrue(english.contains(key), "Missing english translation: " + key);
            assertFalse(english.getString(key, "").isBlank(), "Blank english translation: " + key);
            assertTrue(german.contains(key), "Missing german translation: " + key);
            assertFalse(german.getString(key, "").isBlank(), "Blank german translation: " + key);
        }
    }

    private String[] requiredKeys() {
        return new String[]{
                "building.container.protected",
                "building.hologram.progress",
                "building.hologram.inputBuffer",
                "building.hologram.outputBuffer",
                "building.storage.input_empty",
                "building.storage.output_full",
                "building.ticket.accepted",
                "building.ticket.denied",
                "building.ticket.empty",
                "building.ticket.entry",
                "building.ticket.header",
                "building.ticket.invalid",
                "building.ticket.missingId",
                "building.ticket.missingMessage",
                "building.ticket.missingSections",
                "building.ticket.teleported",
                "buildingSite.created"
        };
    }
}
