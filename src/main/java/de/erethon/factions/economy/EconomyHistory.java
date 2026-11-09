package de.erethon.factions.economy;

import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.economy.report.EconomyCycleReport;
import de.erethon.factions.economy.report.PopulationLevelReport;
import de.erethon.factions.economy.report.ResourceFlowReport;
import de.erethon.factions.economy.resource.Resource;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small persisted samples for the economy graphs; detailed reports remain transient. */
public final class EconomyHistory {
    public static final int LIMIT = 30;

    public record ResourceSample(double produced, double consumed, int stored) { }
    public record PopulationSample(int count, double happiness) { }
    public record Sample(long timestamp, Map<Resource, ResourceSample> resources,
                         Map<PopulationLevel, PopulationSample> population, double tax, double unrest) { }

    private final List<Sample> samples = new ArrayList<>();

    public List<Sample> samples() {
        return List.copyOf(samples);
    }

    public void capture(EconomyCycleReport report) {
        Map<Resource, ResourceSample> resources = new EnumMap<>(Resource.class);
        for (ResourceFlowReport flow : report.resources().values()) {
            resources.put(flow.resource(), new ResourceSample(flow.acceptedProduction(), flow.consumed(), flow.stored()));
        }
        Map<PopulationLevel, PopulationSample> population = new EnumMap<>(PopulationLevel.class);
        for (PopulationLevelReport level : report.population().values()) {
            population.put(level.level(), new PopulationSample(level.population(), level.happiness()));
        }
        samples.add(new Sample(System.currentTimeMillis(), Map.copyOf(resources), Map.copyOf(population),
                report.totalTaxRevenue(), report.unrestAfter()));
        while (samples.size() > LIMIT) samples.removeFirst();
    }

    public List<Map<String, Object>> save() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Sample sample : samples) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("time", sample.timestamp());
            row.put("tax", sample.tax());
            row.put("unrest", sample.unrest());
            Map<String, Object> resources = new LinkedHashMap<>();
            sample.resources().forEach((resource, value) -> resources.put(resource.name(),
                    List.of(value.produced(), value.consumed(), value.stored())));
            row.put("resources", resources);
            Map<String, Object> population = new LinkedHashMap<>();
            sample.population().forEach((level, value) -> population.put(level.name(),
                    List.of(value.count(), value.happiness())));
            row.put("population", population);
            result.add(row);
        }
        return result;
    }

    public void load(List<? extends Map<?, ?>> rows) {
        samples.clear();
        for (Map<?, ?> row : rows) {
            try {
                Map<Resource, ResourceSample> resources = new EnumMap<>(Resource.class);
                if (row.get("resources") instanceof Map<?, ?> values) {
                    values.forEach((key, value) -> {
                        try {
                            List<?> numbers = (List<?>) value;
                            resources.put(Resource.valueOf(String.valueOf(key)), new ResourceSample(
                                    number(numbers.get(0)), number(numbers.get(1)), (int) number(numbers.get(2))));
                        } catch (RuntimeException ignored) { }
                    });
                }
                Map<PopulationLevel, PopulationSample> population = new EnumMap<>(PopulationLevel.class);
                if (row.get("population") instanceof Map<?, ?> values) {
                    values.forEach((key, value) -> {
                        try {
                            List<?> numbers = (List<?>) value;
                            population.put(PopulationLevel.valueOf(String.valueOf(key)),
                                    new PopulationSample((int) number(numbers.get(0)), number(numbers.get(1))));
                        } catch (RuntimeException ignored) { }
                    });
                }
                samples.add(new Sample(((Number) row.get("time")).longValue(), Map.copyOf(resources),
                        Map.copyOf(population), number(row.get("tax")), number(row.get("unrest"))));
            } catch (RuntimeException ignored) { }
        }
        while (samples.size() > LIMIT) samples.removeFirst();
    }

    private static double number(Object value) {
        return value instanceof Number number && Double.isFinite(number.doubleValue()) ? number.doubleValue() : 0;
    }
}
