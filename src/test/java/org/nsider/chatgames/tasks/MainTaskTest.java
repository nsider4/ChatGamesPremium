package org.nsider.chatgames.tasks;

import org.junit.jupiter.api.Test;
import org.nsider.chatgames.enums.GameType;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTaskTest {

    @Test
    void theSameGameNeverRunsTwiceInARow() {
        List<GameType> games = Arrays.asList(GameType.MATH, GameType.TRIVIA, GameType.REACTION);
        Random random = new Random(7);
        GameType previous = null;
        for (int i = 0; i < 1000; i++) {
            GameType next = MainTask.pickGame(games, previous, random, type -> 1);
            assertNotEquals(previous, next);
            previous = next;
        }
    }

    @Test
    void aSingleEnabledGameStillRuns() {
        List<GameType> games = Collections.singletonList(GameType.MATH);
        assertEquals(GameType.MATH, MainTask.pickGame(games, GameType.MATH, new Random(), type -> 1));
    }

    @Test
    void weightsSetHowOftenEachGameIsPicked() {
        List<GameType> games = Arrays.asList(GameType.MATH, GameType.TRIVIA, GameType.REACTION);
        Map<GameType, Integer> counts = new EnumMap<>(GameType.class);
        Random random = new Random(11);
        for (int i = 0; i < 30000; i++) {
            GameType next = MainTask.pickGame(games, null, random, type -> type == GameType.MATH ? 3 : type == GameType.TRIVIA ? 1 : 0);
            counts.merge(next, 1, Integer::sum);
        }
        assertNull(counts.get(GameType.REACTION), "weight 0 is never picked");
        double ratio = counts.get(GameType.MATH) / (double) counts.get(GameType.TRIVIA);
        assertTrue(ratio > 2.7 && ratio < 3.3, "expected about 3:1, got " + ratio);
    }

    @Test
    void nothingIsPickedWhenEveryWeightIsZero() {
        assertNull(MainTask.pickGame(Arrays.asList(GameType.MATH, GameType.TRIVIA), null, new Random(), type -> 0));
    }
}
