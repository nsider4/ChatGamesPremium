package org.nsider.chatgames.utils.gameplay;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.annotations.Nonnull;
import org.nsider.chatgames.utils.messages.MessageUtils;

import java.util.List;

/**
 * Utility class to handle command executions for different scenarios in the plugin.
 */
public class CommandUtils {

    private static ChatGames plugin;

    /**
     * Initializes the CommandUtils with the plugin instance.
     *
     * @param plugin The instance of the {@link ChatGames} plugin.
     */
    public void initialize(@Nonnull ChatGames plugin) {
        CommandUtils.plugin = plugin;
    }

    /**
     * Executes a list of commands based on predefined patterns.
     *
     * @param commands A list of command strings to be executed.
     * @param player   The player who triggered the command.
     */
    public static void executeCommands(@Nonnull List<String> commands, @Nonnull Player player) {
        for (String command : commands) {
            String formattedCommand = command.replaceAll("%player%", player.getName());

            if (formattedCommand.contains("[playercmd]")) {
                executePlayerCommand(formattedCommand, player);
            } else if (formattedCommand.contains("[consolecmd]")) {
                executeConsoleCommand(formattedCommand);
            } else if (formattedCommand.contains("[playermsg]")) {
                sendPlayerMessage(formattedCommand, player);
            } else if (formattedCommand.contains("[broadcast]")) {
                broadcastMessage(formattedCommand, player);
            }
        }
    }

    /**
     * Executes a command as the player.
     *
     * @param command The command to be executed.
     * @param player  The player who will execute the command.
     */
    private static void executePlayerCommand(@Nonnull String command, @Nonnull Player player) {
        String cmd = command.replace("[playercmd] ", "");
        Bukkit.dispatchCommand(player, cmd);
    }

    /**
     * Executes a command as the console.
     *
     * @param command The command to be executed.
     */
    private static void executeConsoleCommand(@Nonnull String command) {
        String cmd = command.replace("[consolecmd] ", "");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    /**
     * Sends a message to a player.
     *
     * @param command The command string containing the message.
     * @param player  The player to whom the message will be sent.
     */
    private static void sendPlayerMessage(@Nonnull String command, @Nonnull Player player) {
        String message = command.replace("[playermsg] ", "");
        player.sendMessage(MessageUtils.color(message));
    }

    /**
     * Broadcasts a message to all players in the enabled world.
     *
     * @param command The command string containing the broadcast message.
     * @param player  The player who initiated the broadcast.
     */
    private static void broadcastMessage(@Nonnull String command, @Nonnull Player player) {
        String message = command.replace("[broadcast] ", "");
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (plugin.getGameManager().worldEnabled(player)) {
                onlinePlayer.sendMessage(MessageUtils.color(message));
            }
        }
    }
}
