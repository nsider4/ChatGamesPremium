package org.nsider.chatgames.discord;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordMessageTest {

    @Test
    void onlyAllowedRolesAndUsersCanPing() {
        DiscordMessage message = new DiscordMessage().content("<@&111111> @everyone")
                .allowRoles(Arrays.asList("111111", "not-an-id", " 222222 "))
                .allowUser("333333");
        JsonObject mentions = message.toJson(false).getAsJsonObject("allowed_mentions");
        assertEquals(0, mentions.getAsJsonArray("parse").size());
        assertEquals("[\"111111\",\"222222\"]", mentions.getAsJsonArray("roles").toString());
        assertEquals("[\"333333\"]", mentions.getAsJsonArray("users").toString());
    }

    @Test
    void textsAreCutToDiscordsLimits() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 5000; i++) longText.append('a');
        DiscordMessage.Embed embed = new DiscordMessage.Embed().title(longText.toString()).description(longText.toString());
        for (int i = 0; i < 30; i++) embed.field("n" + i, "v", true);
        DiscordMessage message = new DiscordMessage().content(longText.toString()).embed(embed);
        assertEquals(DiscordMessage.CONTENT_LIMIT, message.getContent().length());
        assertTrue(message.getContent().endsWith("…"));
        assertEquals(DiscordMessage.TITLE_LIMIT, embed.getTitle().length());
        assertEquals(DiscordMessage.DESCRIPTION_LIMIT, embed.getDescription().length());
        assertEquals(DiscordMessage.MAX_FIELDS, embed.getFields().size());
    }

    @Test
    void anEmojiIsNeverCutInHalf() {
        String trophy = new String(Character.toChars(0x1F3C6));
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 1000; i++) text.append(trophy);
        String cut = DiscordMessage.cut(text.toString(), 255);
        assertFalse(Character.isHighSurrogate(cut.charAt(cut.length() - 2)));
    }

    @Test
    void editsKeepTheWebhookIdentityAndClearOldText() {
        DiscordMessage message = new DiscordMessage().username("Games").avatarUrl("https://example.com/a.png")
                .embed(new DiscordMessage.Embed().title("Hi"));
        JsonObject post = message.toJson(false);
        JsonObject edit = message.toJson(true);
        assertEquals("Games", post.get("username").getAsString());
        assertFalse(edit.has("username"));
        assertFalse(edit.has("avatar_url"));
        assertEquals("", edit.get("content").getAsString());
        assertEquals(1, edit.getAsJsonArray("embeds").size());
    }

    @Test
    void anEmbedWithOnlyAPictureIsEmpty() {
        DiscordMessage message = new DiscordMessage().embed(new DiscordMessage.Embed().image("https://example.com/a.png"));
        assertTrue(message.isEmpty());
    }
}
