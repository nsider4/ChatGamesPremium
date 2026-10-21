package org.nsider.chatgames.utils.gameplay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionUtilsTest {

    @Test
    void parsesLegacySpigotVersions() {
        assertArrayEquals(new int[]{1, 8, 8}, VersionUtils.parse("1.8.8-R0.1-SNAPSHOT"));
        assertArrayEquals(new int[]{1, 21, 11}, VersionUtils.parse("1.21.11-R0.2-SNAPSHOT"));
        assertArrayEquals(new int[]{1, 13, 0}, VersionUtils.parse("1.13-R0.1-SNAPSHOT"));
    }

    @Test
    void parsesYearBasedVersions() {
        assertArrayEquals(new int[]{26, 1, 2}, VersionUtils.parse("26.1.2-R0.1-SNAPSHOT"));
        assertArrayEquals(new int[]{26, 2, 0}, VersionUtils.parse("26.2-R0.1-SNAPSHOT"));
        assertArrayEquals(new int[]{26, 3, 0}, VersionUtils.parse("26.3.build.207-stable"));
        assertArrayEquals(new int[]{26, 1, 2}, VersionUtils.parse("26.1.2.build.8-stable"));
    }

    @Test
    void fallsBackToOldestSupportedVersion() {
        assertArrayEquals(new int[]{1, 8, 0}, VersionUtils.parse("garbage"));
        assertArrayEquals(new int[]{1, 8, 0}, VersionUtils.parse(null));
    }

    @Test
    void yearBasedVersionsAreNewerThanEveryLegacyVersion() {
        int[] current = VersionUtils.parse("26.1-R0.1-SNAPSHOT");
        assertTrue(VersionUtils.isAtLeast(current, 1, 16, 0));
        assertTrue(VersionUtils.isAtLeast(current, 1, 21, 11));
        assertTrue(VersionUtils.isAtLeast(current, 26, 1, 0));
        assertFalse(VersionUtils.isAtLeast(current, 26, 2, 0));
    }

    @Test
    void comparesLegacyVersions() {
        int[] current = VersionUtils.parse("1.12.2-R0.1-SNAPSHOT");
        assertTrue(VersionUtils.isAtLeast(current, 1, 8, 0));
        assertTrue(VersionUtils.isAtLeast(current, 1, 12, 2));
        assertFalse(VersionUtils.isAtLeast(current, 1, 13, 0));
        assertFalse(VersionUtils.isAtLeast(current, 26, 1, 0));
    }
}
