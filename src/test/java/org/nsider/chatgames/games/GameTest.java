package org.nsider.chatgames.games;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.nsider.chatgames.enums.GameType;
import org.nsider.chatgames.games.utils.PlayerResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTest {

    @Test
    void placesAreGivenInOrderUpToMaxWinners() {
        Game game = new Game(GameType.UNSCRAMBLE, "creeper", 2);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        assertEquals(1, game.claimPlace(first).getPlace());
        assertFalse(game.isFull());
        assertEquals(2, game.claimPlace(second).getPlace());
        assertTrue(game.isFull());
        assertNull(game.claimPlace(UUID.randomUUID()), "no third place in a two-winner game");
    }

    @Test
    void aPlayerCanOnlyPlaceOnce() {
        Game game = new Game(GameType.MATH, "12", 3);
        UUID player = UUID.randomUUID();

        assertNotNull(game.claimPlace(player));
        assertNull(game.claimPlace(player));
        assertEquals(1, game.getResultsByPlace().size());
    }

    @Test
    void finishedGamesAcceptNoMoreWinners() {
        Game game = new Game(GameType.REACTION, "go", 3);

        assertTrue(game.finish());
        assertFalse(game.finish(), "only the first caller ends the game");
        assertNull(game.claimPlace(UUID.randomUUID()));
    }

    @Test
    void timeIsMeasuredFromTheStartOfTheGame() throws InterruptedException {
        Game game = new Game(GameType.REACTION, "go", 2);
        Thread.sleep(50);
        PlayerResult result = game.claimPlace(UUID.randomUUID());

        // Windows may wake a sleeping thread a few ms early
        assertTrue(result.getTime() >= 0.04, "time was " + result.getTime());
        assertTrue(result.getTime() < 5, "time was " + result.getTime());
    }

    @Test
    void maxWinnersIsAtLeastOne() {
        assertEquals(1, new Game(GameType.TRIVIA, "a", 0).getMaxWinners());
    }

    /**
     * Fifty players answer at the same instant from different threads, the way
     * async chat delivers them. Exactly three must win, with places 1, 2 and 3.
     */
    @RepeatedTest(20)
    void simultaneousAnswersNeverOverfillOrDuplicatePlaces() throws Exception {
        int maxWinners = 3;
        int players = 50;
        Game game = new Game(GameType.UNSCRAMBLE, "zombie", maxWinners);

        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger winners = new AtomicInteger();
        Set<Integer> places = ConcurrentHashMap.newKeySet();
        AtomicInteger finishers = new AtomicInteger();

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < players; i++) {
            tasks.add(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                PlayerResult result = game.claimPlace(UUID.randomUUID());
                if (result != null) {
                    winners.incrementAndGet();
                    places.add(result.getPlace());
                    if (game.isFull() && game.finish()) {
                        finishers.incrementAndGet();
                    }
                }
            });
        }
        tasks.forEach(pool::submit);
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(maxWinners, winners.get());
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), places);
        assertEquals(1, finishers.get(), "the game must be concluded exactly once");
    }
}
