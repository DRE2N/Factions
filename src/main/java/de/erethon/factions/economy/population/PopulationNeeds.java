package de.erethon.factions.economy.population;

import de.erethon.factions.economy.resource.Resource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

/** Population food is a shared need; grain, fish and fruit substitute one-for-one. */
public final class PopulationNeeds {

    private PopulationNeeds() {
    }

    public static List<Resource> alternatives(Resource resource) {
        return resource == Resource.GRAIN ? List.of(Resource.GRAIN, Resource.FISH, Resource.FRUIT) : List.of(resource);
    }

    public static int available(Resource resource, ToIntFunction<Resource> stock) {
        long total = 0;
        for (Resource alternative : alternatives(resource)) {
            total += Math.max(0, stock.applyAsInt(alternative));
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public static Map<Resource, Integer> consumption(Resource resource, int required, ToIntFunction<Resource> stock) {
        Map<Resource, Integer> consumed = new LinkedHashMap<>();
        int remaining = Math.max(0, required);
        for (Resource alternative : alternatives(resource)) {
            int amount = Math.min(remaining, Math.max(0, stock.applyAsInt(alternative)));
            if (amount > 0) {
                consumed.put(alternative, amount);
                remaining -= amount;
            }
        }
        return consumed;
    }
}
