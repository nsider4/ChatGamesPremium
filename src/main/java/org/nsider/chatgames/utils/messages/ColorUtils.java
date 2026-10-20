package org.nsider.chatgames.utils.messages;

import org.bukkit.ChatColor;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.utils.gameplay.VersionUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling color-related operations.
 * Provides methods for colorizing messages in Minecraft using both Minecraft formatting codes and hex color codes.
 */
public class ColorUtils {

    private static ChatGames plugin;

    protected static void initialize(ChatGames plugin) {
        ColorUtils.plugin = plugin;
    }

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
        return message.replace("&", "§");
    }

    /**
     * Translates hex color codes in the form of "#RRGGBB" into Minecraft's color codes.
     * For example: #FF5733 becomes "&x&f&f&5&7&3&3".
     *
     * @param message the message containing hex color codes
     * @return the message with hex colors translated to Minecraft color codes
     */
    private static String translateHexColors(String message) {
        Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(message);

        while (matcher.find()) {
            String hexCode = message.substring(matcher.start(), matcher.end());
            String replaceSharp = hexCode.replace('#', 'x');

            char[] ch = replaceSharp.toCharArray();
            StringBuilder builder = new StringBuilder("");
            for (char c : ch) {
                builder.append("&").append(c);
            }

            message = message.replace(hexCode, builder.toString());
            matcher = pattern.matcher(message);
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
