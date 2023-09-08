package de.erethon.factions.region;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Fyreum
 */
public class RegionStructureAttribute {

    private final String name;
    private final List<String> suggestions;
    private final List<RegionStructureAttribute> children;

    public RegionStructureAttribute(@NotNull String name) {
        this(name, List.of());
    }

    public RegionStructureAttribute(@NotNull String name, @NotNull List<String> suggestions, @NotNull RegionStructureAttribute... children) {
        this.name = name;
        this.suggestions = suggestions;
        this.children = List.of(children);
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull List<String> getSuggestions() {
        return suggestions;
    }

    public @NotNull List<String> getSuggestions(@NotNull String arg) {
        List<String> list = new ArrayList<>();
        for (RegionStructureAttribute child : children) {
            list.addAll(child.getSuggestions(arg));
        }
        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase().startsWith(arg.toLowerCase())) {
                return suggestions;
            }
        }
        return list;
    }

    public @NotNull List<RegionStructureAttribute> getChildren() {
        return children;
    }
}
