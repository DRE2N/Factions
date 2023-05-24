package de.erethon.factions.util;

import org.junit.jupiter.api.Test;

public class WarMathTest {

    @Test
    void calculateKillScores() {
        int i = 1;
        while (true) {
            double score = WarMath.scoreForKills(i, 5);
            if (score <= 0) {
                break;
            }
            System.out.println(i++ + ": " + score);
        }
    }
}
