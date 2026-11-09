package de.erethon.factions.economy.population;

import de.erethon.factions.economy.resource.Resource;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PopulationNeedsTest {

    @Test
    void fishAndFruitCanSatisfyFoodWithoutGrain() {
        Map<Resource, Integer> stock = Map.of(Resource.FISH, 4, Resource.FRUIT, 9);
        assertEquals(13, PopulationNeeds.available(Resource.GRAIN, r -> stock.getOrDefault(r, 0)));
        assertEquals(Map.of(Resource.FISH, 4, Resource.FRUIT, 6),
                PopulationNeeds.consumption(Resource.GRAIN, 10, r -> stock.getOrDefault(r, 0)));
    }

    @Test
    void shortageConsumesOnlyExistingFoodOnce() {
        Map<Resource, Integer> stock = Map.of(Resource.GRAIN, 2, Resource.FISH, 3);
        assertEquals(stock, PopulationNeeds.consumption(Resource.GRAIN, 10, r -> stock.getOrDefault(r, 0)));
    }

    @Test
    void foodCannotReplaceOtherNeeds() {
        Map<Resource, Integer> stock = Map.of(Resource.GRAIN, 100, Resource.COAL, 2);
        assertEquals(Map.of(Resource.COAL, 2),
                PopulationNeeds.consumption(Resource.COAL, 5, r -> stock.getOrDefault(r, 0)));
        assertEquals(Map.of(), PopulationNeeds.consumption(Resource.GRAIN, 0, r -> stock.getOrDefault(r, 0)));
    }
}
