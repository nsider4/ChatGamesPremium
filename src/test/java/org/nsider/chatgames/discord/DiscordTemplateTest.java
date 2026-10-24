package org.nsider.chatgames.discord;

import com.google.gson.JsonObject;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordTemplateTest {

    private static YamlConfiguration yaml(String text) throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(text);
        return yaml;
    }

    @Test
    void everyPartIsFilled() throws InvalidConfigurationException {
        DiscordTemplate template = DiscordTemplate.read(yaml(String.join("\n",
                "content: 'Hi %player%'",
                "embed:",
                "  color: gold",
                "  author: '%player% :trophy:'",
                "  title: '%game%'",
                "  description:",
                "    - 'Line one'",
                "    - '%others%'",
                "    - 'Line three'",
                "  fields:",
                "    - name: 'Time'",
                "      value: '%time%s'",
                "      inline: true",
                "    - name: ''",
                "      value: '%others%'",
                "  footer:",
                "    text: '%server%'",
                "    icon: 'not a link'")));
        assertNotNull(template);
        Map<String, String> values = new HashMap<>();
        values.put("%player%", DiscordText.escape("__Bob__"));
        values.put("%game%", "Unscramble");
        values.put("%others%", "");
        values.put("%time%", "2.5");
        values.put("%server%", "My *Server*");
        DiscordMessage message = template.render(values, null);

        assertEquals("Hi \\_\\_Bob\\_\\_", message.getContent());
        DiscordMessage.Embed embed = message.getEmbed();
        assertNotNull(embed);
        assertEquals(0xF1C40F, (int) embed.getColor());
        assertEquals("__Bob__ " + new String(Character.toChars(0x1F3C6)), embed.getAuthorName(), "author names show Markdown as typed");
        assertEquals("Line one\nLine three", embed.getDescription(), "a line emptied by a placeholder is dropped");
        assertEquals(1, embed.getFields().size(), "a field emptied by its placeholder is left out");
        assertEquals("2.5s", embed.getFields().get(0).getValue());
        assertTrue(embed.getFields().get(0).isInline());
        assertEquals("My *Server*", embed.getFooterText());
        assertNull(embed.getFooterIcon(), "a broken link is dropped rather than failing the message");
    }

    @Test
    void aSectionWithNothingToSendHasNoTemplate() throws InvalidConfigurationException {
        assertNull(DiscordTemplate.read(yaml("enabled: true")));
        assertNull(DiscordTemplate.read(null));
    }

    /** Every message the plugin sends exists in the shipped file and renders to something Discord accepts. */
    @Test
    void shippedMessagesAllRender() throws IOException, InvalidConfigurationException {
        String text;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("discord.yml")) {
            assertNotNull(in);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int n; (n = in.read(buffer)) > 0; ) out.write(buffer, 0, n);
            text = new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
        // Spigot 1.8's SnakeYAML refuses the whole file over one emoji outside the BMP
        assertTrue(text.codePoints().allMatch(Character::isBmpCodePoint), "discord.yml uses :shortcodes:, not raw emoji");
        YamlConfiguration file = yaml(text);
        for (String name : DiscordManager.MESSAGES) {
            ConfigurationSection section = file.getConfigurationSection(name);
            DiscordTemplate template = DiscordTemplate.read(section);
            assertNotNull(template, name);
            DiscordMessage message = template.render(new HashMap<String, String>(), null);
            assertFalse(message.isEmpty(), name);
            JsonObject json = message.toJson(false);
            assertTrue(json.has("allowed_mentions"), name);
        }
    }
}
