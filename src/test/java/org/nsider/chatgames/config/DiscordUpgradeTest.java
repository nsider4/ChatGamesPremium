package org.nsider.chatgames.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordUpgradeTest {

    private static final Function<String, InputStream> RESOURCES = name -> DiscordUpgradeTest.class.getClassLoader().getResourceAsStream(name);

    @TempDir
    File data;

    /** A 2.0.0 folder: Discord settings in add-ons.yml and events.yml. */
    @Test
    void settingsMoveToDiscordYml() throws IOException {
        write("add-ons.yml", "#Log games\nlog-file: 'games.txt'\n\n#Discord\ndiscord:\n  webhook-url: 'https://discord.com/api/webhooks/1/abc'\n"
                + "  username: 'Games Bot'\n  img-url: ''\n  title: 'Old title'\n\n#After\nlog-console: true\n");
        write("events.yml", "events: {}\ndiscord:\n  enabled: false\n");

        List<String> moved = DiscordUpgrade.run(data, RESOURCES);
        assertEquals(3, moved.size(), moved.toString());

        YamlConfiguration discord = YamlConfiguration.loadConfiguration(new File(data, "discord.yml"));
        assertEquals("https://discord.com/api/webhooks/1/abc", discord.getString("channels.main.webhook"));
        assertEquals("Games Bot", discord.getString("channels.main.username"));
        assertFalse(discord.getBoolean("events.enabled"));
        assertTrue(read("discord.yml").contains("#"), "discord.yml keeps its comments");

        String addons = read("add-ons.yml");
        assertFalse(addons.contains("discord:"));
        assertFalse(addons.contains("#Discord\n"), "the old section's comment goes with it");
        assertTrue(addons.contains("#Log games\nlog-file: 'games.txt'"));
        assertTrue(addons.contains("#After\nlog-console: true"));
        assertFalse(read("events.yml").contains("discord:"));

        assertTrue(DiscordUpgrade.run(data, RESOURCES).isEmpty(), "runs once");
    }

    @Test
    void nothingToDoOnANewFolder() throws IOException {
        assertTrue(DiscordUpgrade.run(data, RESOURCES).isEmpty());
        assertFalse(new File(data, "discord.yml").exists());
    }

    @Test
    void oneNineNineKeysMap() {
        assertEquals("channels.main.webhook", DiscordUpgrade.mapLegacy("discord.webhook-url"));
        assertEquals(null, DiscordUpgrade.mapLegacy("discord.title"));
        assertEquals(null, DiscordUpgrade.mapLegacy("prefix"));
    }

    private void write(String name, String text) throws IOException {
        Files.write(new File(data, name).toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

    private String read(String name) throws IOException {
        return new String(Files.readAllBytes(new File(data, name).toPath()), StandardCharsets.UTF_8);
    }
}
