package org.nsider.chatgames.api;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTypeTest {

    @Test
    void builtInGamesKeepTheirNames() {
        assertSame(GameType.GUESS_THE_NUMBER, GameType.fromName(" Guess_The_Number "));
        assertSame(GameType.MEMORIZE, GameType.fromName("shoppinglist"));
        assertEquals("GUESS_THE_NUMBER", GameType.GUESS_THE_NUMBER.name());
        assertEquals("guess_the_number", GameType.GUESS_THE_NUMBER.getConfigKey());
        assertTrue(GameType.MINE.isMaterialRace());
        assertFalse(GameType.HUNT.isMaterialRace());
        assertTrue(GameType.TAME.isMobRace());
        assertFalse(GameType.SHEAR.isMaterialRace());
        assertFalse(GameType.FISH.isMobRace());
        assertTrue(GameType.COUNT.isGlobalOnly());
        assertNull(GameType.fromName("nope"));
    }

    @Test
    void idsMustBeWellFormed() {
        assertTrue(GameType.isValidId("riddle_2"));
        assertFalse(GameType.isValidId("Riddle"));
        assertFalse(GameType.isValidId("2riddle"));
        assertFalse(GameType.isValidId("r"));
        assertFalse(GameType.isValidId("riddle-game"));
        assertThrows(IllegalArgumentException.class, () -> GameType.register("Bad Id", GameType.Kind.CHAT, "Test"));
    }

    @Test
    void addedGamesCanBeFoundAndRemoved() {
        GameType riddle = GameType.register("test_riddle", GameType.Kind.CHAT, "TestPlugin");
        try {
            assertSame(riddle, GameType.fromName("TEST_RIDDLE"));
            assertFalse(riddle.isBuiltIn());
            assertEquals("TestPlugin", riddle.getOwner());
            assertSame(riddle, GameType.values()[GameType.values().length - 1], "added games come after the built-in ones");
            assertThrows(IllegalArgumentException.class, () -> GameType.register("test_riddle", GameType.Kind.RACE, "Other"));
            assertThrows(IllegalArgumentException.class, () -> GameType.register("math", GameType.Kind.CHAT, "Other"));
            assertThrows(IllegalArgumentException.class, () -> GameType.register("shoppinglist", GameType.Kind.CHAT, "Other"));
        } finally {
            assertTrue(GameType.unregister(riddle));
        }
        assertNull(GameType.fromName("test_riddle"));
        assertFalse(Arrays.asList(GameType.values()).contains(riddle));
        assertFalse(GameType.unregister(riddle), "already gone");
        assertFalse(GameType.unregister(GameType.MATH), "built-in games stay");
    }
}
