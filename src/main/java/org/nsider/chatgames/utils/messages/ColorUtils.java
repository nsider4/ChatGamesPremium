package org.nsider.chatgames.utils.messages;

import org.bukkit.ChatColor;
import org.nsider.chatgames.utils.gameplay.VersionUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling color-related operations.
 * Provides methods for colorizing messages in Minecraft using both Minecraft formatting codes and hex color codes.
 */
public final class ColorUtils {

    private static final Pattern HEX = Pattern.compile("#[a-fA-F0-9]{6}");

    private ColorUtils() {}

    /**
     * Colorizes a message, translating Minecraft color codes (&) and hex color codes (for Minecraft 1.16+).
     *
     * @param message the message to be colorized
     * @return the colorized message
     */
    protected static String colorizeMessage(String message) {
        if (VersionUtils.isAtLeast(1, 16)) {
            return translateHexColors(message);
        }
        // Only "&" followed by a colour code is translated, so a plain "&" in text survives
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Translates hex color codes in the form of "#RRGGBB" into Minecraft's color codes.
     * For example: #FF5733 becomes "&x&f&f&5&7&3&3".
     *
     * @param message the message containing hex color codes
     * @return the message with hex colors translated to Minecraft color codes
     */
    private static String translateHexColors(String message) {
        Matcher matcher = HEX.matcher(message);
        if (!matcher.find()) return ChatColor.translateAlternateColorCodes('&', message);
        // One pass over the text, instead of searching it again after every colour
        StringBuffer out = new StringBuffer(message.length() + 32);
        do {
            StringBuilder code = new StringBuilder("&x");
            for (int i = matcher.start() + 1; i < matcher.end(); i++) code.append('&').append(message.charAt(i));
            matcher.appendReplacement(out, Matcher.quoteReplacement(code.toString()));
        } while (matcher.find());
        matcher.appendTail(out);
        return ChatColor.translateAlternateColorCodes('&', out.toString());
    }
}
