package org.nsider.chatgames.other;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckerTest {

    @Test
    void olderSpigotVersionIsNotAnUpdate() {
        // Spigot still lists 1.9.9 while 2.0.0 is being tested
        assertFalse(UpdateChecker.isNewer("1.9.9", "2.0.0"));
    }

    @Test
    void sameVersionIsNotAnUpdate() {
        assertFalse(UpdateChecker.isNewer("2.0.0", "2.0.0"));
        assertFalse(UpdateChecker.isNewer("2.0", "2.0.0"));
    }

    @Test
    void higherVersionIsAnUpdate() {
        assertTrue(UpdateChecker.isNewer("2.0.1", "2.0.0"));
        assertTrue(UpdateChecker.isNewer("2.1", "2.0.9"));
        assertTrue(UpdateChecker.isNewer("2.0.10", "2.0.9"));
    }

    @Test
    void suffixesAreIgnored() {
        assertFalse(UpdateChecker.isNewer("2.0.0", "2.0.0-SNAPSHOT"));
        assertTrue(UpdateChecker.isNewer("v2.0.1", "2.0.0"));
    }

    @Test
    void versionKeyIsTheSameOnlyForTheSameVersion() {
        assertEquals(UpdateChecker.versionKey("2.0.3"), UpdateChecker.versionKey("v2.0.3"));
        assertEquals(UpdateChecker.versionKey("2.1"), UpdateChecker.versionKey("2.1.0"));
        assertNotEquals(UpdateChecker.versionKey("2.0.3"), UpdateChecker.versionKey("2.0.30"));
        assertNotEquals(UpdateChecker.versionKey("2.0.3"), UpdateChecker.versionKey("2.3"));
        // 0 is what a player who was never notified has
        assertNotEquals(0L, UpdateChecker.versionKey("0.0.1"));
    }
}
