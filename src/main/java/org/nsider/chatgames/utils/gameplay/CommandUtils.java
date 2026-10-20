package org.nsider.chatgames.utils.gameplay;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.nsider.chatgames.ChatGames;
import org.nsider.chatgames.utils.messages.MessageUtils;
import org.nsider.inventorymanager.extras.Nonnull;

import java.util.Collections;
import java.util.List;
import java.util.Set;

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
     * Runs a command through a sender that reports every permission and OP as granted,
     * without giving the player real OP. Output is forwarded to the player.
     *
     * @param player  the player the command runs for
     * @param command the command, without the leading slash
     */
    public static void executeAsOp(@Nonnull Player player, @Nonnull String command) {
        CommandSender proxySender = new CommandSender() {
            @Override public boolean isOp() { return true; }
            @Override public void setOp(boolean value) {}
            @Override public void sendMessage(String message) { player.sendMessage(message); }
            @Override public void sendMessage(String[] messages) { player.sendMessage(messages); }
            @Override public boolean hasPermission(String name) { return true; }
            @Override public boolean hasPermission(Permission permission) { return true; }
            @Override public boolean isPermissionSet(String name) { return true; }
            @Override public boolean isPermissionSet(Permission permission) { return true; }
            @Override public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) { return null; }
            @Override public PermissionAttachment addAttachment(Plugin plugin) { return null; }
            @Override public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) { return null; }
            @Override public PermissionAttachment addAttachment(Plugin plugin, int ticks) { return null; }
            @Override public void removeAttachment(PermissionAttachment attachment) {}
            @Override public void recalculatePermissions() {}
            @Override public Set<PermissionAttachmentInfo> getEffectivePermissions() { return Collections.emptySet(); }
            @Override public Server getServer() { return Bukkit.getServer(); }
            @Override public String getName() { return player.getName(); }
        };
        Bukkit.dispatchCommand(proxySender, command);
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
