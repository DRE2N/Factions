package de.erethon.factions.economy;

import de.erethon.factions.economy.report.EconomyCycleReport;
import de.erethon.factions.economy.report.ResourceFlowReport;
import de.erethon.factions.economy.resource.Resource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EconomyHistoryTest {
    @Test
    void keepsThirtyCyclesThroughSerialization() {
        EconomyHistory history = new EconomyHistory();
        for (int cycle = 0; cycle < 35; cycle++) {
            EconomyCycleReport report = new EconomyCycleReport();
            report.resources().put(Resource.GRAIN, new ResourceFlowReport(Resource.GRAIN,
                    cycle, 100, cycle + 2, cycle + 1, 1, 3, cycle - 2, false));
            report.totalTaxRevenue(cycle * 1.5);
            report.unrestAfter(cycle / 100.0);
            history.capture(report);
        }

        EconomyHistory restored = new EconomyHistory();
        restored.load(history.save());
        assertEquals(30, restored.samples().size());
        assertEquals(5, restored.samples().getFirst().resources().get(Resource.GRAIN).stored());
        assertEquals(34, restored.samples().getLast().resources().get(Resource.GRAIN).stored());
        assertEquals(51, restored.samples().getLast().tax());
    }

    @Test
    void ignoresDamagedSavedRows() {
        EconomyHistory history = new EconomyHistory();
        history.load(List.of(java.util.Map.of("time", "invalid")));
        assertEquals(0, history.samples().size());
    }
}
