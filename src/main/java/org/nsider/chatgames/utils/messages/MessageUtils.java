package org.nsider.chatgames.utils.messages;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Utility class for sending and formatting messages.
 */
public final class MessageUtils {

    private static ChatGames plugin;

    private MessageUtils() {}

    /**
     * Gives the message helpers the plugin, for its prefix and PlaceholderAPI. Called once on enable.
     *
     * @param pluginInstance the ChatGames plugin instance
     */
    public static void initialize(ChatGames pluginInstance) {
        plugin = pluginInstance;
    }

    /**
     * Filters suggestions based on user input.
     *
     * @param suggestions The list of all possible suggestions.
     * @param input       The current user input to filter against.
     * @return A list of filtered suggestions matching the input.
     */
    public static List<String> filterSuggestions(List<String> suggestions, String input) {
        if (input == null || input.isEmpty()) {
            return suggestions;
        }
        String typed = input.toLowerCase(Locale.ROOT);
        return suggestions.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(typed))
                .collect(Collectors.toList());
    }

    /**
     * Sends a message to a CommandSender (player or console).
     * Supports <center> tag for centered messages.
     *
     * @param sender  the CommandSender
     * @param message the message to send
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (plugin.isPlaceHolderAPIEnabled() && sender instanceof Player) {
            message = PlaceholderAPI.setPlaceholders((Player) sender, message);
        }
        if (message.contains("<center>")) {
            CenteredMessageUtils.sendCenteredMessage(sender, color(message.replace("<center>", "")));
        } else {
            sender.sendMessage(color(message));
        }
    }

    /**
     * Colors a string by replacing color codes (e.g., &a, &b) and applying a prefix.
     *
     * @param message the string to color
     * @return the colored string
     */
    public static String color(String message) {
        String prefixedMessage = message.replace("%prefix%", plugin.getConfig().getString("prefix", ""));
        return ColorUtils.colorizeMessage(prefixedMessage);
    }

    /**
     * Colors a list of messages by applying the color codes to each string in the list.
     *
     * @param messages the list of messages to colorize
     * @return the list of colorized messages
     */
    public static List<String> colorList(List<String> messages) {
        return messages.stream().map(MessageUtils::color).collect(Collectors.toList());
    }
}
