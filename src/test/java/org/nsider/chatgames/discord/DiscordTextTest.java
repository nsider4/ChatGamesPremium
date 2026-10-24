package org.nsider.chatgames.discord;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DiscordTextTest {

    private static final String ZWSP = String.valueOf((char) 0x200B);

    @Test
    void knownShortcodesBecomeEmojiAndOthersStay() {
        assertEquals(new String(Character.toChars(0x1F3C6)) + " won", DiscordText.emoji(":trophy: won"));
        assertEquals(":not_an_emoji:", DiscordText.emoji(":not_an_emoji:"));
        // A server's own emoji and Discord timestamps are left alone
        assertEquals("<:trophy:123456>", DiscordText.emoji("<:trophy:123456>"));
        assertEquals("<a:trophy:123456>", DiscordText.emoji("<a:trophy:123456>"));
        assertEquals("<t:1700000000:R>", DiscordText.emoji("<t:1700000000:R>"));
    }

    @Test
    void escapeKeepsNamesAsTypedAndBreaksMentions() {
        assertEquals("\\_\\_Bob\\_\\_", DiscordText.escape("__Bob__"));
        assertEquals("@" + ZWSP + "everyone", DiscordText.escape("@everyone"));
        assertEquals("", DiscordText.escape(null));
        for (String text : new String[]{"__Bob__", "@everyone", "a*b~c`d|e>f#g[h](i)-j\\k"}) {
            assertEquals(text, DiscordText.unescape(DiscordText.escape(text)));
        }
    }

    @Test
    void colourCodesAreRemoved() {
        assertEquals("Hello world", DiscordText.stripColors("&aHello §lworld"));
        assertEquals("Red", DiscordText.stripColors("&#ff0000Red"));
    }

    @Test
    void coloursAreReadFromHexNamesAndNumbers() {
        assertEquals(0xFF0000, (int) DiscordText.color("#ff0000"));
        assertEquals(0x00FF00, (int) DiscordText.color("00FF00"));
        assertEquals(0xF1C40F, (int) DiscordText.color("Gold"), "Discord's gold");
        assertEquals(255, (int) DiscordText.color("255"));
        assertNull(DiscordText.color("not a colour"));
        assertNull(DiscordText.color(""));
        assertNull(DiscordText.color("99999999"));
    }

    @Test
    void barsFillInProportion() {
        assertEquals("##--", DiscordText.bar(5, 10, 4, "#", "-"));
        assertEquals("####", DiscordText.bar(50, 10, 4, "#", "-"));
        assertEquals("----", DiscordText.bar(-3, 10, 4, "#", "-"));
        assertEquals("####", DiscordText.bar(0, 0, 4, "#", "-"));
    }

    @Test
    void placeholdersAreFilledAndUnknownOnesKept() {
        Map<String, String> values = new HashMap<>();
        values.put("%player%", "Steve");
        values.put("%points%", "3");
        assertEquals("Steve +3 %unknown% 100%", DiscordText.fill("%player% +%points% %unknown% 100%", values));
    }
}
