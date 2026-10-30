package org.nsider.chatgames.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigLoadErrorTest {

    @Test
    void pointsAtTheBrokenLine() {
        // A real SnakeYAML error: the tab on line 3 is the problem, line 2 only starts the block
        InvalidConfigurationException e = assertThrows(InvalidConfigurationException.class,
                () -> new YamlConfiguration().loadFromString("a: 1\nb:\n\tc: 2\n"));
        ConfigLoadError error = ConfigLoadError.of("games.yml", e);
        assertEquals("games.yml", error.getFile());
        assertEquals(3, error.getLine());
        assertEquals(1, error.getColumn());
    }

    @Test
    void unreadableFileHasNoPosition() {
        ConfigLoadError error = ConfigLoadError.of("words.yml", new FileNotFoundException("words.yml (Access is denied)"));
        assertFalse(error.hasPosition());
    }
}
