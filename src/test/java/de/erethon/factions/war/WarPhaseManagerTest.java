package de.erethon.factions.war;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;

/**
 * @author Fyreum
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WarPhaseManagerTest {

    private WarPhaseManager warPhaseManager;

    @BeforeAll
    void beforeClass() {
        warPhaseManager = new WarPhaseManager(new File("test", "schedule.yml"));
    }

    @Test
    void calculateWeeks() {
        String week = "3-2";
        String[] split = week.split("-");
        int start = Integer.parseInt(split[0]);
        int end = Integer.parseInt(split[1]);
        System.out.println(start);
        while (start != end) {
            if (++start > 7) {
                start -= 7;
            }
            System.out.println(start);
        }
    }

}
