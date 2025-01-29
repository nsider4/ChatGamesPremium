package org.nsider.chatgames.utils.messages;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.chat.TextComponent;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.utils.gameplay.VersionUtils;

import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Utility class for handling message sending, logging, and message formatting.
 */
public class MessageUtils {

    private static ChatGames plugin;

    /**
     * Initializes the MessageUtils class with the plugin instance.
     * Ensures that only one instance of MessageUtils is created.
     *
     * @param pluginInstance the ElytraTweaks plugin instance
     */
    public static void initialize(ChatGames pluginInstance) {
        plugin = pluginInstance;
        ColorUtils.initialize(pluginInstance);
        CenteredMessageUtils.initialize(pluginInstance);
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
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
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

    public static String removePercentSymbols(String msg) {
        if(msg.contains("%") && !msg.startsWith("$ ")) {
            String[] split = msg.split("%");
            for (int i = 0; i < split.length; i++) {
                if(i % 2 != 0) {
                    msg = msg.replaceAll(split[i], "");
                }
            }
            msg = msg.replaceAll("%", "");
        }

        return msg;
    }

    /**
     * Checks if the player's input matches the expected answer in the game.
     *
     * @param player       the player to check
     * @param chatTextRaw  the raw input from the player
     * @return true if the input matches, false otherwise
     */
    public static boolean textEqualsWord(Player player, String chatTextRaw) {
        if (plugin.getGameManager().getGame() == null) {
            return false;
        }

        return TextMatchUtils.textEqualsWord(
                player,
                chatTextRaw,
                plugin.getGameManager().getGame(),
                plugin
        );
    }

    /**
     * Colors a string by replacing color codes (e.g., &a, &b) and applying a prefix.
     *
     * @param message the string to color
     * @return the colored string
     */
    public static String color(String message) {
        String prefixedMessage = message.replace("%prefix%", plugin.getConfig().getString("prefix", ""));
        return ColorUtils.colorizeMessage(prefixedMessage, VersionUtils.getMCVersion());
    }

    /**
     * Logs a message at the specified level.
     *
     * @param level the log level (e.g., INFO, WARNING)
     * @param msg   the message to log
     */
    public static void log(Level level, String msg) {
        plugin.getLogger().log(level, msg);
    }

    /**
     * Sends a centered message to a CommandSender.
     *
     * @param sender  the CommandSender
     * @param message the message to send
     */
    public static void sendCenteredMessage(CommandSender sender, String message) {
        CenteredMessageUtils.sendCenteredMessage(sender, message);
    }

    /**
     * Sends a centered message with a clickable component to a player.
     *
     * @param player            the player
     * @param message           the message to send
     * @param clickableComponent the clickable component
     */
    public static void sendCenteredMessage(Player player, String message, TextComponent clickableComponent) {
        CenteredMessageUtils.sendCenteredMessage(player, message, clickableComponent);
    }
}
