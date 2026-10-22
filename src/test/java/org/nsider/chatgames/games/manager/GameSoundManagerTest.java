package org.nsider.chatgames.games.manager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSoundManagerTest {

    @Test
    void vanillaNamesAreMappedAcrossVersions() {
        assertTrue(GameSoundManager.isVanilla("minecraft:entity.player.levelup"));
        assertTrue(GameSoundManager.isVanilla("ENTITY_PLAYER_LEVELUP"));
        assertTrue(GameSoundManager.isVanilla("entity.player.levelup"));
        assertEquals("ENTITY_PLAYER_LEVELUP", GameSoundManager.toEnumName("minecraft:entity.player.levelup"));
    }

    @Test
    void resourcePackSoundsArePlayedAsWritten() {
        // Even when the rest of the name matches a vanilla sound
        assertFalse(GameSoundManager.isVanilla("myserver:entity.player.levelup"));
        assertFalse(GameSoundManager.isVanilla("myserver:custom.ding"));
    }
}
