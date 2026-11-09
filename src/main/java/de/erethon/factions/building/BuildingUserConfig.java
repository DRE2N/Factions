package de.erethon.factions.building;

import de.erethon.factions.faction.Faction;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Data-defined choices whose selected effects are resolved independently for each build site. */
public final class BuildingUserConfig {

    public record Option(String id, String label, Map<String, Integer> requiredBuildings,
                         Map<String, BuildingEffectData> effects) {
        public boolean available(Faction faction) {
            return faction != null && requiredBuildings.entrySet().stream().allMatch(requirement ->
                    faction.getFactionBuildings().stream()
                            .filter(site -> site.isFinished() && site.isActive())
                            .filter(site -> site.getBuilding().getId().equals(requirement.getKey()))
                            .count() >= requirement.getValue());
        }

        public Component displayName() {
            return Component.translatable(label, label);
        }
    }

    public record Setting(String id, String label, String defaultOption, Map<String, Option> options) {
        public Option selected(String saved) {
            return saved == null ? options.get(defaultOption) : options.getOrDefault(saved, options.get(defaultOption));
        }

        public Component displayName() {
            return Component.translatable(label, label);
        }
    }

    private final List<Setting> settings;

    public BuildingUserConfig(ConfigurationSection section, Set<Material> blocksOfInterest) {
        List<Setting> loaded = new ArrayList<>();
        if (section != null) {
            for (String id : section.getKeys(false)) {
                requireId(id);
                ConfigurationSection setting = requireSection(section, id);
                ConfigurationSection choices = requireSection(setting, "options");
                Map<String, Option> options = new LinkedHashMap<>();
                for (String optionId : choices.getKeys(false)) {
                    requireId(optionId);
                    ConfigurationSection option = requireSection(choices, optionId);
                    Map<String, Integer> requirements = new LinkedHashMap<>();
                    ConfigurationSection required = option.getConfigurationSection("requiredBuildings");
                    if (required != null) {
                        for (String building : required.getKeys(false)) {
                            int count = required.getInt(building);
                            if (count < 1) throw new IllegalArgumentException("Invalid building count for " + building);
                            requirements.put(building, count);
                        }
                    }
                    Map<String, BuildingEffectData> effects = new LinkedHashMap<>();
                    ConfigurationSection effectSection = option.getConfigurationSection("effects");
                    if (effectSection != null) {
                        for (String effectId : effectSection.getKeys(false)) {
                            ConfigurationSection data = requireSection(effectSection, effectId);
                            effects.put(effectId, new BuildingEffectData(data, effectId));
                            ConfigurationSection modifiers = data.getConfigurationSection("blockModifiers");
                            if (modifiers != null) {
                                for (String blockId : modifiers.getKeys(false)) {
                                    Material material = Material.matchMaterial(blockId);
                                    if (material != null) blocksOfInterest.add(material);
                                }
                            }
                        }
                    }
                    options.put(optionId, new Option(optionId, option.getString("displayName", optionId),
                            Map.copyOf(requirements), Collections.unmodifiableMap(effects)));
                }
                if (options.isEmpty()) throw new IllegalArgumentException("No options for building setting " + id);
                String defaultOption = setting.getString("default", options.keySet().iterator().next());
                if (!options.containsKey(defaultOption)) throw new IllegalArgumentException("Unknown default for building setting " + id);
                loaded.add(new Setting(id, setting.getString("displayName", id), defaultOption,
                        Collections.unmodifiableMap(options)));
            }
        }
        settings = List.copyOf(loaded);
    }

    public List<Setting> settings() {
        return settings;
    }

    public Setting setting(String id) {
        return settings.stream().filter(setting -> setting.id().equals(id)).findFirst().orElse(null);
    }

    public List<BuildingEffectData> effects(BuildSite site, Set<BuildingEffectData> base) {
        Map<String, BuildingEffectData> result = new LinkedHashMap<>();
        base.forEach(effect -> result.put(effect.getConfig().getName(), effect));
        for (Setting setting : settings) {
            Option selected = setting.selected(site.getString("userConfig." + setting.id(), setting.defaultOption()));
            result.putAll(selected.effects());
        }
        return List.copyOf(result.values());
    }

    private static void requireId(String id) {
        if (!id.matches("[a-zA-Z0-9_-]+")) throw new IllegalArgumentException("Invalid building setting ID " + id);
    }

    private static ConfigurationSection requireSection(ConfigurationSection parent, String key) {
        ConfigurationSection section = parent.getConfigurationSection(key);
        if (section == null) throw new IllegalArgumentException("Expected section at " + parent.getCurrentPath() + "." + key);
        return section;
    }
}
