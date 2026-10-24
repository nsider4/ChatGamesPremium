package org.nsider.chatgames.utils.messages;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for handling centered messages.
 * Provides methods for centering text messages and sending them to players,
 * including support for clickable components.
 */
public final class CenteredMessageUtils {

    private CenteredMessageUtils() {}

    /**
     * Returns a list of centered lines that fit within Minecraft's chat width (320 pixels).
     * The message is split into words, and each line is centered individually.
     *
     * @param message the message to center
     * @return a list of centered lines
     */
    public static List<String> getCenteredMessage(String message) {
        message = ChatColor.translateAlternateColorCodes('&', message);
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        StringBuilder color = new StringBuilder();

        String[] words = message.split(" ");
        for (String word : words) {
            StringBuilder wordBuilder = new StringBuilder();
            int wordWidth = FontUtils.getWidthOfString(word + " ");

            for (int i = 0; i < (word + " ").length(); i++) {
                char c = (word + " ").charAt(i);
                if (c == '§') {
                    color.setLength(0);
                    color.append(c);
                } else if (color.length() > 0 && (color.length() == 1 || color.charAt(1) == 'x')) {
                    color.append(c);
                    if (color.length() == 14 || color.length() == 2) { //Complete hex color or regular color code
                        wordBuilder.append(color);
                        color = new StringBuilder();
                    }
                } else {
                    wordBuilder.append(c);
                }
            }

            //If adding the word would exceed the line width, start a new line
            if (FontUtils.getWidthOfString(currentLine.toString()) + wordWidth > 320) {
                if (currentLine.length() > 0) {
                    lines.add(centerLine(currentLine.toString().trim()));
                    currentLine = new StringBuilder(extractActiveCodes(currentLine.toString())); //Start a new line with the current color and style
                }
            }
            currentLine.append(wordBuilder); //Add the processed word to the line
        }

        if (currentLine.length() > 0) {
            lines.add(centerLine(currentLine.toString().trim()));
        }
        return lines;
    }

    /**
     * Extracts the active color and formatting codes from the end of a string.
     *
     * @param str the string to extract codes from
     * @return the active color and formatting codes
     */
    private static String extractActiveCodes(String str) {
        StringBuilder activeCodes = new StringBuilder();
        for (int i = str.length() - 1; i >= 0; i--) {
            if (str.charAt(i) == '§' && i + 1 < str.length()) {
                activeCodes.insert(0, str.substring(i, i + 2));
                if (str.charAt(i + 1) == 'x' && i + 14 <= str.length()) {
                    activeCodes = new StringBuilder(str.substring(i, i + 14));
                    break;
                }
            }
        }
        return activeCodes.toString();
    }

    /**
     * Centers a single line of text within the available chat width.
     *
     * @param line the line of text to center
     * @return the centered line of text
     */
    private static String centerLine(String line) {
        int paddingCount = getPaddingToCenter(line, 0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paddingCount; i++) {
            sb.append(' ');
        }
        sb.append(line);
        return sb.toString();
    }

    /**
     * @return the spaces that centre text of this (coloured) content on a chat line
     */
    static String padding(String text) {
        StringBuilder padding = new StringBuilder();
        for (int i = getPaddingToCenter(text, 0); i > 0; i--) {
            padding.append(' ');
        }
        return padding.toString();
    }

    /**
     * Sends a centered message to a CommandSender (e.g., a player or the console).
     * Each line of the message is centered individually.
     *
     * @param player  the recipient of the message
     * @param message the message to send
     */
    public static void sendCenteredMessage(CommandSender player, String message) {
        if (message == null || message.equals("")) {
            player.sendMessage("");
            return;
        }
        List<String> centeredMessages = getCenteredMessage(message);
        for (String line : centeredMessages) {
            player.sendMessage(line);
        }
    }

    /**
     * Sends a centered message with a clickable component to a player.
     *
     * @param player             the recipient of the message
     * @param message            the message to send
     * @param clickableComponent the clickable component to attach to the message
     */
    public static void sendCenteredMessage(Player player, String message, TextComponent clickableComponent) {
        int clickableComponentWidth = clickableComponent.toPlainText().length() * 4;
        int paddingToCenter = getPaddingToCenter(message, clickableComponentWidth);

        StringBuilder paddingBuilder = new StringBuilder();
        for (int i = 0; i < paddingToCenter; i++) {
            paddingBuilder.append(' ');
        }

        TextComponent paddedComponent = new TextComponent(TextComponent.fromLegacyText(paddingBuilder + message));
        paddedComponent.addExtra(clickableComponent);

        player.spigot().sendMessage(paddedComponent);
    }

    /**
     * Calculates the amount of padding needed to center a message within Minecraft's chat width.
     *
     * @param message      the message to center
     * @param extraWidth   the additional width (e.g., for clickable components)
     * @return the amount of padding to center the message
     */
    private static int getPaddingToCenter(String message, int extraWidth) {
        int totalWidth = FontUtils.getWidthOfString(message) + extraWidth + 20;
        int chatWidth = 320;
        int spaceWidth = FontUtils.DefaultFontInfo.SPACE.getLength() + 1;
        int availableSpace = (chatWidth - totalWidth) / 2;
        return Math.max(0, availableSpace / spaceWidth);
    }
}
